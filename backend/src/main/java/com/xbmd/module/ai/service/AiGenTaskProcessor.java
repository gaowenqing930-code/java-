package com.xbmd.module.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbmd.common.BusinessException;
import com.xbmd.common.ResultCode;
import com.xbmd.module.ai.config.AiProperties;
import com.xbmd.module.ai.dto.AiGenerateRequest;
import com.xbmd.module.ai.dto.AiGenerateResult;
import com.xbmd.module.ai.dto.AiGeneratedQuestion;
import com.xbmd.module.ai.entity.AiGenTask;
import com.xbmd.module.ai.mapper.AiGenTaskMapper;
import com.xbmd.module.ai.util.SsePusher;
import com.xbmd.module.knowledge.entity.KnowledgeCategory;
import com.xbmd.module.knowledge.mapper.KnowledgeCategoryMapper;
import com.xbmd.module.knowledge.service.KnowledgeCategoryService;
import com.xbmd.module.question.dto.QuestionVO;
import com.xbmd.module.question.entity.Question;
import com.xbmd.module.question.service.QuestionService;
import com.xbmd.module.question.util.QuestionContentValidator;
import com.xbmd.module.question.util.QuestionFingerprintUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AI 出题异步任务处理器：独立 Bean，承载 @Async 逐题生成（避开自调用 @Async 失效）。
 * 流程：RUNNING → 逐题调用模型/校验/入库 → 推进度 + SSE → SUCCESS/FAILED。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGenTaskProcessor {

    private static final String SOURCE_AI = "AI";
    private static final String PENDING = "PENDING";
    private static final String RUNNING = "RUNNING";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";

    private final AiGenTaskMapper taskMapper;
    private final SsePusher ssePusher;
    private final ObjectProvider<ChatModel> chatModelProvider;
    private final QuestionService questionService;
    private final KnowledgeCategoryMapper categoryMapper;
    private final KnowledgeCategoryService categoryService;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Async("aiScoreExecutor")
    public void processTask(Long taskId) {
        AiGenTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(RUNNING);
        task.setProgress(0);
        taskMapper.updateById(task);
        try {
            AiGenerateRequest req = objectMapper.readValue(task.getRequestJson(), AiGenerateRequest.class);
            KnowledgeCategory point = categoryMapper.selectById(req.getCategoryId());
            if (point == null) {
                fail(task, "知识点不存在");
                return;
            }
            ChatModel model = chatModelProvider.getIfAvailable();
            if (model == null) {
                success(task, new AiGenerateResult(
                        doFallback(req), true, "未配置可用的大模型，已从题库兜底抽题"));
                return;
            }
            List<Long> pointIds = categoryService.collectKnowledgePointIds(req.getCategoryId());
            if (pointIds.isEmpty()) {
                fail(task, "目标分类下没有可关联的知识点");
                return;
            }
            List<Long> categoryIds = List.of(req.getCategoryId());
            int total = req.getCount();
            List<QuestionVO> saved = new ArrayList<>();
            Set<String> batchFp = new HashSet<>();
            for (int i = 0; i < total; i++) {
                if (!generateOne(task, req, point.getName(), categoryIds, batchFp, saved)) {
                    return; // 已 fail
                }
                int progress = (int) ((i + 1) * 100.0 / total);
                task.setProgress(progress);
                taskMapper.updateById(task);
                ssePusher.pushProgress(taskId, i + 1, total);
            }
            success(task, new AiGenerateResult(saved, false, "AI 出题成功"));
        } catch (Exception e) {
            log.warn("任务 {} 出题异常：{}", taskId, e.getMessage(), e);
            fail(task, e.getMessage());
        }
    }

    /** 生成并保存一题，成功返回 true；重试耗尽返回 false（内部已 fail 任务） */
    private boolean generateOne(AiGenTask task, AiGenerateRequest req, String pointName,
                                List<Long> categoryIds, Set<String> batchFp, List<QuestionVO> saved) {
        for (int attempt = 1; attempt <= aiProperties.getMaxRetries(); attempt++) {
            try {
                AiGeneratedQuestion q = callModelOne(req, pointName);
                String fp = QuestionFingerprintUtil.compute(q.getStem(), categoryIds, req.getDifficulty());
                if (batchFp.contains(fp) || questionService.fingerprintExists(fp)) {
                    throw new BusinessException(ResultCode.CONFLICT, "与已有题目指纹重复");
                }
                batchFp.add(fp);

                Question question = new Question();
                question.setType(req.getType());
                question.setDifficulty(req.getDifficulty());
                question.setStem(q.getStem().trim());
                question.setOptionsJson(toJson(q.getOptions()));
                question.setAnswer(q.getAnswer().trim());
                question.setScorePointsJson(toJson(q.getScorePoints()));
                question.setAnalysis(q.getAnalysis());
                question.setSource(SOURCE_AI);
                questionService.saveGenerated(question, categoryIds);
                saved.add(questionService.getDetail(question.getId()));
                return true;
            } catch (BusinessException e) {
                log.warn("生成一题第 {} 次尝试失败：{}", attempt, e.getMessage());
            } catch (Exception e) {
                log.warn("生成一题第 {} 次尝试异常：{}", attempt, e.getMessage());
            }
        }
        fail(task, "单题生成多次失败，已停止");
        return false;
    }

    /** 调用模型生成单题并校验 */
    private AiGeneratedQuestion callModelOne(AiGenerateRequest req, String pointName) {
        ChatModel model = chatModelProvider.getIfAvailable();
        if (model == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "大模型不可用");
        }
        String template = loadTemplate(req.getType());
        String prompt = template
                .replace("${knowledgePoint}", pointName)
                .replace("${difficulty}", String.valueOf(req.getDifficulty()))
                .replace("${count}", "1");
        ChatClient client = ChatClient.create(model);
        String content = client.prompt().user(prompt).call().content();
        if (content == null || content.isBlank()) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "模型返回内容为空");
        }
        String json = extractJsonArray(content);
        List<AiGeneratedQuestion> parsed;
        try {
            parsed = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, AiGeneratedQuestion.class));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "模型返回 JSON 解析失败：" + e.getMessage());
        }
        if (parsed == null || parsed.isEmpty()) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "模型未生成题目");
        }
        AiGeneratedQuestion q = parsed.get(0);
        QuestionContentValidator.validateAndNormalize(
                req.getType(), q.getStem(), q.getOptions(), q.getAnswer(), q.getScorePoints());
        return q;
    }

    private List<QuestionVO> doFallback(AiGenerateRequest req) {
        List<Long> pointIds = categoryService.collectKnowledgePointIds(req.getCategoryId());
        int count = Math.min(req.getCount(), aiProperties.getFallbackMaxCount());
        return questionService.randomPick(req.getType(), req.getDifficulty(), pointIds, count);
    }

    private void success(AiGenTask task, AiGenerateResult result) {
        task.setStatus(SUCCESS);
        task.setProgress(100);
        task.setResultJson(toJson(result));
        task.setFinishTime(LocalDateTime.now());
        taskMapper.updateById(task);
        ssePusher.pushDone(task.getId(), result.getQuestions(), result.getMessage());
    }

    private void fail(AiGenTask task, String msg) {
        task.setStatus(FAILED);
        task.setErrorMsg(msg);
        task.setFinishTime(LocalDateTime.now());
        taskMapper.updateById(task);
        ssePusher.pushError(task.getId(), msg);
    }

    private String loadTemplate(String type) {
        Resource resource = resourceLoader.getResource(
                "classpath:" + aiProperties.getPromptDir() + "/" + type + ".txt");
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "Prompt 模板加载失败：" + type);
        }
    }

    private String extractJsonArray(String content) {
        String text = content.trim();
        int fenceStart = text.indexOf("```");
        if (fenceStart >= 0) {
            int newline = text.indexOf('\n', fenceStart);
            int fenceEnd = text.lastIndexOf("```");
            if (newline >= 0 && fenceEnd > newline) {
                text = text.substring(newline + 1, fenceEnd).trim();
            }
        }
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        throw new BusinessException(ResultCode.INTERNAL_ERROR, "模型输出中未找到 JSON 数组");
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "题目序列化失败");
        }
    }
}
