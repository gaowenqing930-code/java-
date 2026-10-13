package com.xbmd.module.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbmd.common.BusinessException;
import com.xbmd.common.ResultCode;
import com.xbmd.module.admin.service.OperationLogService;
import com.xbmd.module.ai.config.AiProperties;
import com.xbmd.module.ai.dto.AiGenerateRequest;
import com.xbmd.module.ai.dto.AiGenerateResult;
import com.xbmd.module.ai.dto.AiGeneratedQuestion;
import com.xbmd.module.ai.dto.AiGenTaskVO;
import com.xbmd.module.ai.entity.AiGenTask;
import com.xbmd.module.ai.mapper.AiGenTaskMapper;
import com.xbmd.module.ai.service.AiGenTaskProcessor;
import com.xbmd.module.ai.service.AiQuestionService;
import com.xbmd.module.ai.util.PerUserRateLimiter;
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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.util.StringUtils;

/**
 * AI 智能出题服务实现：调用-解析-校验-重试-降级五级管道
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQuestionServiceImpl implements AiQuestionService {

    private static final String SOURCE_AI = "AI";

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final QuestionService questionService;
    private final KnowledgeCategoryMapper categoryMapper;
    private final KnowledgeCategoryService categoryService;
    private final AiProperties aiProperties;
    private final PerUserRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final AiGenTaskMapper aiGenTaskMapper;
    private final AiGenTaskProcessor aiGenTaskProcessor;
    private final ResourceLoader resourceLoader;
    private final OperationLogService operationLogService;

    @Override
    public AiGenerateResult generate(AiGenerateRequest request, Long userId) {
        // 1. 按用户限流
        if (!rateLimiter.tryAcquire(userId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "AI 出题操作过于频繁，请 " + aiProperties.getRateLimitPerMinute() + " 分钟后再试");
        }
        // 2. 校验知识点
        KnowledgeCategory point = categoryMapper.selectById(request.getCategoryId());
        if (point == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "知识点不存在");
        }
        // 3. 本地大模型（Ollama 等）由自动配置提供；拿不到才题库兜底
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            return doFallback(request, "未配置可用的大模型，已从题库兜底抽题");
        }
        // 4. 有限重试生成
        for (int attempt = 1; attempt <= aiProperties.getMaxRetries(); attempt++) {
            try {
                List<AiGeneratedQuestion> parsed = callModelAndParse(chatModel, request, point.getName());
                List<QuestionVO> saved = saveValidatedQuestions(parsed, request);
                return new AiGenerateResult(saved, false, "AI 出题成功");
            } catch (BusinessException e) {
                log.warn("AI 出题第 {} 次尝试失败：{}", attempt, e.getMessage());
            } catch (Exception e) {
                log.warn("AI 出题第 {} 次尝试异常：{}", attempt, e.getMessage());
            }
        }
        // 5. 重试耗尽 → 题库兜底
        return doFallback(request, "AI 出题多次失败，已从题库兜底抽题");
    }

    @Override
    public AiGenTaskVO submitGenerate(AiGenerateRequest request, Long userId) {
        // 1. 按用户限流
        if (!rateLimiter.tryAcquire(userId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "AI 出题操作过于频繁，请 " + aiProperties.getRateLimitPerMinute() + " 分钟后再试");
        }
        // 2. 校验知识点
        KnowledgeCategory point = categoryMapper.selectById(request.getCategoryId());
        if (point == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "知识点不存在");
        }
        // 3. 建任务并触发异步生成（立即返回 taskId，不阻塞）
        AiGenTask task = new AiGenTask();
        task.setUserId(userId);
        task.setStatus("PENDING");
        task.setProgress(0);
        task.setRequestJson(toJson(request));
        task.setCreateTime(LocalDateTime.now());
        aiGenTaskMapper.insert(task);
        aiGenTaskProcessor.processTask(task.getId());
        return toTaskVO(task);
    }

    @Override
    public AiGenTaskVO getTaskStatus(Long taskId, Long userId) {
        AiGenTask task = aiGenTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "出题任务不存在");
        }
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该任务");
        }
        return toTaskVO(task);
    }

    /** 任务实体 → VO（SUCCESS 时反序列化结果题目） */
    private AiGenTaskVO toTaskVO(AiGenTask task) {
        AiGenTaskVO vo = new AiGenTaskVO();
        vo.setTaskId(task.getId());
        vo.setStatus(task.getStatus());
        vo.setProgress(task.getProgress());
        vo.setCreateTime(task.getCreateTime());
        vo.setFinishTime(task.getFinishTime());
        if ("SUCCESS".equals(task.getStatus()) && StringUtils.hasText(task.getResultJson())) {
            try {
                AiGenerateResult result = objectMapper.readValue(task.getResultJson(), AiGenerateResult.class);
                vo.setQuestions(result.getQuestions());
                vo.setMessage(result.getMessage());
            } catch (JsonProcessingException ignore) {
                // 反序列化失败不影响状态查询
            }
        }
        if ("FAILED".equals(task.getStatus())) {
            vo.setMessage(task.getErrorMsg());
        }
        return vo;
    }

    /** 调用模型并解析、校验，返回通过校验的题目列表 */
    private List<AiGeneratedQuestion> callModelAndParse(ChatModel chatModel, AiGenerateRequest request,
                                                        String pointName) {
        String template = loadTemplate(request.getType());
        String prompt = template
                .replace("${knowledgePoint}", pointName)
                .replace("${difficulty}", String.valueOf(request.getDifficulty()))
                .replace("${count}", String.valueOf(request.getCount()));

        ChatClient chatClient = ChatClient.create(chatModel);
        String content = chatClient.prompt().user(prompt).call().content();
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
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "模型未生成任何题目");
        }
        // 按题型逐题校验（题干/选项/答案/评分点）
        for (AiGeneratedQuestion q : parsed) {
            QuestionContentValidator.validateAndNormalize(
                    request.getType(), q.getStem(), q.getOptions(), q.getAnswer(), q.getScorePoints());
        }
        return parsed;
    }

    /** 整体校验指纹（批内去重 + 库内查重），全部通过后统一入库 */
    private List<QuestionVO> saveValidatedQuestions(List<AiGeneratedQuestion> parsed, AiGenerateRequest request) {
        List<Long> pointIds = categoryService.collectKnowledgePointIds(request.getCategoryId());
        if (pointIds.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "目标分类下没有可关联的知识点");
        }
        List<Long> categoryIds = List.of(request.getCategoryId());

        Set<String> batchFingerprints = new HashSet<>();
        List<Question> toSave = new ArrayList<>();
        for (AiGeneratedQuestion q : parsed) {
            String fingerprint = QuestionFingerprintUtil.compute(q.getStem(), categoryIds, request.getDifficulty());
            if (batchFingerprints.contains(fingerprint)) {
                continue; // 批内重复：跳过
            }
            if (questionService.fingerprintExists(fingerprint)) {
                throw new BusinessException(ResultCode.CONFLICT, "与库中已有题目指纹重复，重新生成");
            }
            batchFingerprints.add(fingerprint);

            Question question = new Question();
            question.setType(request.getType());
            question.setDifficulty(request.getDifficulty());
            question.setStem(q.getStem().trim());
            question.setOptionsJson(toJson(q.getOptions()));
            question.setAnswer(q.getAnswer().trim());
            question.setScorePointsJson(toJson(q.getScorePoints()));
            question.setAnalysis(q.getAnalysis());
            question.setSource(SOURCE_AI);
            toSave.add(question);
        }
        if (toSave.size() != request.getCount()) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "去重后仅 " + toSave.size() + " 道，未达到 " + request.getCount() + " 道，重新生成");
        }
        List<QuestionVO> result = new ArrayList<>();
        for (Question q : toSave) {
            questionService.saveGenerated(q, categoryIds);
            result.add(questionService.getDetail(q.getId()));
        }
        return result;
    }

    /** 题库兜底抽题 */
    private AiGenerateResult doFallback(AiGenerateRequest request, String message) {
        try {
            operationLogService.log(com.xbmd.security.UserContext.getUserId(), "AI_FAIL",
                    "AI出题降级兜底(" + request.getType() + "): " + message);
        } catch (Exception ignore) {
            // 日志失败不影响业务
        }
        List<Long> pointIds = categoryService.collectKnowledgePointIds(request.getCategoryId());
        int count = Math.min(request.getCount(), aiProperties.getFallbackMaxCount());
        List<QuestionVO> questions = questionService.randomPick(request.getType(), request.getDifficulty(), pointIds, count);
        return new AiGenerateResult(questions, true, message);
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

    /** 从模型输出中提取 JSON 数组（兼容 Markdown 代码块与前后缀文字） */
    private String extractJsonArray(String content) {
        String text = content.trim();
        // 1. 先剥离 Markdown 代码块（不要求以 ``` 开头，模型可能先输出解释文字）
        int fenceStart = text.indexOf("```");
        if (fenceStart >= 0) {
            int newline = text.indexOf('\n', fenceStart);
            int fenceEnd = text.lastIndexOf("```");
            if (newline >= 0 && fenceEnd > newline) {
                text = text.substring(newline + 1, fenceEnd).trim();
            }
        }
        // 2. 截取首个 [ 到最后一个 ]
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
