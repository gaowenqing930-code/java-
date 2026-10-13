package com.xbmd.module.interview.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbmd.common.BusinessException;
import com.xbmd.common.ResultCode;
import com.xbmd.module.ai.config.AiProperties;
import com.xbmd.module.ai.util.AiScoreCoordinator;
import com.xbmd.module.interview.dto.ScoreDTO;
import com.xbmd.module.interview.entity.InterviewQuestion;
import com.xbmd.module.interview.mapper.InterviewQuestionMapper;
import com.xbmd.module.question.dto.ScorePointDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 面试单题评分组件：独立 @Async 方法，供 InterviewServiceImpl 答完一题后异步后台评分。
 * 评分逻辑自包含（AI 评分 + 回答长度规则降级），完成后回写分数与状态；
 * 结束面试时由 InterviewServiceImpl 通过 AiScoreCoordinator 等待未评完的题补齐。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewScoreScorer {

    private static final String Q_SCORED = "SCORED";

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final InterviewQuestionMapper questionMapper;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final AiScoreCoordinator coordinator;

    @Value("${spring.ai.openai.chat.options.model:}")
    private String aiModel;

    /** 答完一题后触发：后台异步评分单题，不阻塞出下一题 */
    @Async("aiScoreExecutor")
    public void scoreAsync(Long questionId) {
        String key = "interview:" + questionId;
        coordinator.register(key);
        try {
            InterviewQuestion q = questionMapper.selectById(questionId);
            if (q == null) {
                return;
            }
            scoreOne(q);
            questionMapper.updateById(q);
        } catch (Exception e) {
            log.warn("面试题 {} 异步评分异常：{}", questionId, e.getMessage(), e);
        } finally {
            coordinator.complete(key);
        }
    }

    /** 单题 AI 评分，多次失败按回答长度降级（与业务无关，纯单题评分） */
    private void scoreOne(InterviewQuestion q) {
        ChatModel model = chatModelProvider.getIfAvailable();
        if (model == null) {
            q.setAiScore(ruleScore(q.getUserAnswer()));
            q.setAiComment("大模型不可用，已按回答长度降级评分");
            q.setStatus(Q_SCORED);
            return;
        }
        String pointsText = formatPoints(fromJsonPoints(q.getScorePointsJson()));
        for (int attempt = 1; attempt <= aiProperties.getMaxRetries(); attempt++) {
            try {
                String prompt = loadTemplate("INTERVIEW_SCORE.txt")
                        .replace("${question}", nvl(q.getQuestion()))
                        .replace("${scorePoints}", pointsText)
                        .replace("${referenceAnswer}", nvl(q.getReferenceAnswer()))
                        .replace("${userAnswer}", nvl(q.getUserAnswer()));
                String content = callModel(model, prompt, 512);
                ScoreDTO dto = objectMapper.readValue(extractJsonObject(content), ScoreDTO.class);
                if (dto == null || dto.getScore() == null || dto.getScore() < 0 || dto.getScore() > 10) {
                    throw new BusinessException(ResultCode.INTERNAL_ERROR, "评分超出范围");
                }
                if (!StringUtils.hasText(dto.getComment())) {
                    throw new BusinessException(ResultCode.INTERNAL_ERROR, "评语为空");
                }
                q.setAiScore(dto.getScore());
                q.setAiComment(dto.getComment());
                q.setStatus(Q_SCORED);
                return;
            } catch (BusinessException e) {
                log.warn("面试评分第 {} 次尝试失败：{}", attempt, e.getMessage());
            } catch (Exception e) {
                log.warn("面试评分第 {} 次尝试异常：{}", attempt, e.getMessage());
            }
        }
        q.setAiScore(ruleScore(q.getUserAnswer()));
        q.setAiComment("AI 评分多次失败，已按回答长度降级评分");
        q.setStatus(Q_SCORED);
    }

    /** 规则降级评分：按回答长度 */
    private int ruleScore(String answer) {
        if (!StringUtils.hasText(answer)) {
            return 0;
        }
        int len = answer.length();
        if (len >= 150) {
            return 8;
        }
        if (len >= 80) {
            return 6;
        }
        if (len >= 30) {
            return 4;
        }
        return 2;
    }

    private String callModel(ChatModel model, String prompt, int maxTokens) {
        OpenAiChatOptions.Builder optBuilder = OpenAiChatOptions.builder().maxTokens(maxTokens);
        if (StringUtils.hasText(aiModel)) {
            optBuilder.model(aiModel);
        }
        ChatClient client = ChatClient.builder(model)
                .defaultOptions(optBuilder)
                .build();
        String content = client.prompt().user(prompt).call().content();
        if (content == null || content.isBlank()) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "模型返回内容为空");
        }
        return content;
    }

    private String loadTemplate(String name) {
        Resource resource = resourceLoader.getResource(
                "classpath:" + aiProperties.getPromptDir() + "/" + name);
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "Prompt 模板加载失败：" + name);
        }
    }

    private String extractJsonObject(String content) {
        String text = content.trim();
        int fence = text.indexOf("```");
        if (fence >= 0) {
            int newline = text.indexOf('\n', fence);
            int fenceEnd = text.lastIndexOf("```");
            if (newline >= 0 && fenceEnd > newline) {
                text = text.substring(newline + 1, fenceEnd).trim();
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        throw new BusinessException(ResultCode.INTERNAL_ERROR, "模型输出中未找到 JSON 对象");
    }

    private String formatPoints(List<ScorePointDTO> points) {
        if (points == null || points.isEmpty()) {
            return "（无评分点，满分 10 分）";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            sb.append(i + 1).append(". ").append(nvl(points.get(i).getPoint()))
                    .append("（").append(points.get(i).getScore()).append("分）\n");
        }
        return sb.toString();
    }

    private List<ScorePointDTO> fromJsonPoints(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, ScorePointDTO.class));
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}
