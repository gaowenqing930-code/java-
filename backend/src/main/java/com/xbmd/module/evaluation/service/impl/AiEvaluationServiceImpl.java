package com.xbmd.module.evaluation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbmd.common.BusinessException;
import com.xbmd.common.ResultCode;
import com.xbmd.module.ai.config.AiProperties;
import com.xbmd.module.ai.util.PerUserRateLimiter;
import com.xbmd.module.evaluation.dto.AiEvaluationDTO;
import com.xbmd.module.evaluation.dto.EvaluationVO;
import com.xbmd.module.evaluation.dto.PointResultDTO;
import com.xbmd.module.evaluation.entity.EvaluationResult;
import com.xbmd.module.evaluation.mapper.EvaluationResultMapper;
import com.xbmd.module.evaluation.service.AiEvaluationService;
import com.xbmd.module.practice.entity.AnswerRecord;
import com.xbmd.module.practice.mapper.AnswerRecordMapper;
import com.xbmd.module.question.entity.Question;
import com.xbmd.module.question.mapper.QuestionMapper;
import com.xbmd.module.question.dto.ScorePointDTO;
import com.xbmd.module.question.util.QuestionContentValidator;
import com.xbmd.module.stats.service.StatsService;
import com.xbmd.module.wrong.service.WrongQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 评测服务实现：模型评测 + 规则降级 + 失败标记待重试
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiEvaluationServiceImpl implements AiEvaluationService {

    private static final String PROMPT_FILE = "EVALUATION.txt";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final EvaluationResultMapper evaluationResultMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final QuestionMapper questionMapper;
    private final AiProperties aiProperties;
    private final PerUserRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final WrongQuestionService wrongQuestionService;
    private final StatsService statsService;

    @Override
    public EvaluationVO evaluate(Long answerId, Question question, String userAnswer) {
        List<ScorePointDTO> points = parsePoints(question);
        int totalScore = points.stream().mapToInt(ScorePointDTO::getScore).sum();

        // 1. 本地大模型（Ollama 等）由自动配置提供；拿不到才规则降级评分
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            return save(answerId, "RULE", ruleEvaluate(points, userAnswer, totalScore),
                    "大模型不可用，已使用关键词规则降级评分");
        }
        // 2. 评测接口按用户限流（超限降级规则评分，不阻塞出分）
        if (!rateLimiter.tryAcquire(question.getId())) {
            return save(answerId, "RULE", ruleEvaluate(points, userAnswer, totalScore),
                    "AI 评测调用过于频繁，已使用关键词规则降级评分");
        }
        // 3. 有限重试调用模型
        for (int attempt = 1; attempt <= aiProperties.getMaxRetries(); attempt++) {
            try {
                AiEvaluationDTO dto = callModelAndParse(chatModel, question, points, totalScore, userAnswer);
                return save(answerId, "AI", dto, null);
            } catch (BusinessException e) {
                log.warn("AI 评测第 {} 次尝试失败：{}", attempt, e.getMessage());
            } catch (Exception e) {
                log.warn("AI 评测第 {} 次尝试异常：{}", attempt, e.getMessage());
            }
        }
        // 4. 重试耗尽 → 标记 FAILED（待重试），不阻塞客观题出分
        return saveFailed(answerId);
    }

    @Override
    public EvaluationVO retry(Long answerId, Long userId) {
        AnswerRecord answer = answerRecordMapper.selectById(answerId);
        if (answer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "答题记录不存在");
        }
        if (!answer.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作他人的评测记录");
        }
        Question question = questionMapper.selectById(answer.getQuestionId());
        if (question == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在");
        }
        EvaluationVO vo = evaluate(answerId, question, answer.getUserAnswer());
        // 重试成功后回填答题记录：分数、对错、错题本、学习画像（与异步评分保持一致）
        if (vo != null && vo.getScore() != null) {
            int total = parsePoints(question).stream().mapToInt(ScorePointDTO::getScore).sum();
            int isCorrect = vo.getScore() >= total * 0.6 ? 1 : 0;
            AnswerRecord rec = answerRecordMapper.selectById(answerId);
            if (rec != null) {
                rec.setScore(vo.getScore());
                rec.setIsCorrect(isCorrect);
                answerRecordMapper.updateById(rec);
                if (isCorrect == 0) {
                    wrongQuestionService.recordWrong(userId, question.getId(), answerId);
                }
                statsService.updateStats(userId, question.getId(), isCorrect == 1);
            }
        }
        return vo;
    }

    // ---------- 模型调用与解析 ----------

    private AiEvaluationDTO callModelAndParse(ChatModel chatModel, Question question,
                                              List<ScorePointDTO> points, int totalScore, String userAnswer) {
        String prompt = loadTemplate()
                .replace("${stem}", question.getStem())
                .replace("${answer}", question.getAnswer() == null ? "" : question.getAnswer())
                .replace("${scorePoints}", formatPoints(points))
                .replace("${totalScore}", String.valueOf(totalScore))
                .replace("${userAnswer}", userAnswer == null ? "" : userAnswer);

        ChatClient chatClient = ChatClient.create(chatModel);
        String content = chatClient.prompt().user(prompt).call().content();
        if (content == null || content.isBlank()) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "模型返回内容为空");
        }
        String json = extractJsonObject(content);
        AiEvaluationDTO dto;
        try {
            dto = objectMapper.readValue(json, AiEvaluationDTO.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "模型返回 JSON 解析失败");
        }
        validate(dto, points, totalScore);
        return dto;
    }

    /** 校验模型输出：分数区间、评分点一一对应、评语非空 */
    private void validate(AiEvaluationDTO dto, List<ScorePointDTO> points, int totalScore) {
        if (dto == null || dto.getScore() == null || dto.getScore() < 0 || dto.getScore() > totalScore) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "评测分数超出范围");
        }
        if (dto.getPointResults() == null || dto.getPointResults().size() != points.size()) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "评分点数量与题目不一致");
        }
        for (int i = 0; i < points.size(); i++) {
            PointResultDTO pr = dto.getPointResults().get(i);
            int total = points.get(i).getScore();
            if (pr.getEarned() == null || pr.getEarned() < 0 || pr.getEarned() > total) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "评分点得分超出范围");
            }
            if (!StringUtils.hasText(pr.getPoint())) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "评分点描述为空");
            }
            pr.setTotal(total);
            pr.setHit(pr.getEarned() != null && pr.getEarned() >= total);
        }
        if (!StringUtils.hasText(dto.getComment())) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "评语为空");
        }
    }

    // ---------- 规则降级评分 ----------

    private AiEvaluationDTO ruleEvaluate(List<ScorePointDTO> points, String userAnswer, int totalScore) {
        List<PointResultDTO> results = new ArrayList<>();
        int earned = 0;
        for (ScorePointDTO sp : points) {
            boolean hit = StringUtils.hasText(userAnswer) && containsAnyKeyword(sp.getPoint(), userAnswer);
            int pointScore = hit ? sp.getScore() : 0;
            earned += pointScore;
            PointResultDTO pr = new PointResultDTO();
            pr.setPoint(sp.getPoint());
            pr.setTotal(sp.getScore());
            pr.setEarned(pointScore);
            pr.setHit(hit);
            results.add(pr);
        }
        AiEvaluationDTO dto = new AiEvaluationDTO();
        dto.setScore(Math.min(earned, totalScore));
        dto.setPointResults(results);
        dto.setComment("规则降级评分：命中 " + (int) results.stream().filter(PointResultDTO::getHit).count()
                + "/" + results.size() + " 个评分点");
        return dto;
    }

    /** 评分点拆词，用户答案命中任一关键词即判定命中
     *  中文评分点常为长句（如"说出控制反转/依赖注入概念"），按标点拆后仍是整体长词，
     *  因此对长词再做 2 字滑窗扩展（如"控制反转"→控制/反转），保证要点词可被命中 */
    private boolean containsAnyKeyword(String point, String userAnswer) {
        String normalized = userAnswer.toLowerCase();
        List<String> keywords = new ArrayList<>();
        for (String w : point.toLowerCase().split("[^\\p{L}\\p{N}]+")) {
            if (w.length() >= 2) {
                keywords.add(w);
            }
        }
        List<String> extra = new ArrayList<>();
        for (String w : keywords) {
            if (w.length() > 4) {
                for (int i = 0; i + 2 <= w.length(); i++) {
                    extra.add(w.substring(i, i + 2));
                }
            }
        }
        keywords.addAll(extra);
        if (keywords.isEmpty()) {
            return normalized.contains(point.toLowerCase());
        }
        return keywords.stream().anyMatch(normalized::contains);
    }

    // ---------- 落库 ----------

    private EvaluationVO save(Long answerId, String evalType, AiEvaluationDTO dto, String fallbackComment) {
        EvaluationResult entity = getByAnswerId(answerId);
        if (entity == null) {
            entity = new EvaluationResult();
            entity.setAnswerId(answerId);
            entity.setStatus(STATUS_PENDING);
        }
        entity.setEvalType(evalType);
        entity.setScore(dto.getScore());
        entity.setPointResultsJson(toJson(dto.getPointResults()));
        entity.setComment(fallbackComment != null ? fallbackComment : dto.getComment());
        entity.setStatus(STATUS_SUCCESS);
        if (entity.getId() == null) {
            evaluationResultMapper.insert(entity);
        } else {
            evaluationResultMapper.updateById(entity);
        }
        return toVO(entity);
    }

    private EvaluationVO saveFailed(Long answerId) {
        EvaluationResult entity = getByAnswerId(answerId);
        if (entity == null) {
            entity = new EvaluationResult();
            entity.setAnswerId(answerId);
            evaluationResultMapper.insert(entity);
        } else {
            entity.setEvalType("AI");
            entity.setStatus(STATUS_FAILED);
            evaluationResultMapper.updateById(entity);
        }
        return toVO(entity);
    }

    private EvaluationResult getByAnswerId(Long answerId) {
        return evaluationResultMapper.selectOne(
                new LambdaQueryWrapper<EvaluationResult>().eq(EvaluationResult::getAnswerId, answerId));
    }

    private EvaluationVO toVO(EvaluationResult entity) {
        EvaluationVO vo = new EvaluationVO();
        vo.setId(entity.getId());
        vo.setEvalType(entity.getEvalType());
        vo.setScore(entity.getScore());
        vo.setPointResults(fromJson(entity.getPointResultsJson()));
        vo.setComment(entity.getComment());
        vo.setStatus(entity.getStatus());
        return vo;
    }

    // ---------- 工具 ----------

    private List<ScorePointDTO> parsePoints(Question question) {
        List<ScorePointDTO> points = fromJsonPoints(question.getScorePointsJson());
        if (points == null || points.isEmpty()) {
            // 无评分点兜底：默认一个 10 分要点
            ScorePointDTO dto = new ScorePointDTO();
            dto.setPoint("参考答案要点");
            dto.setScore(10);
            return List.of(dto);
        }
        return points;
    }

    private String formatPoints(List<ScorePointDTO> points) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            sb.append(i + 1).append(". ").append(points.get(i).getPoint())
                    .append("（").append(points.get(i).getScore()).append("分）\n");
        }
        return sb.toString();
    }

    private String loadTemplate() {
        Resource resource = resourceLoader.getResource(
                "classpath:" + aiProperties.getPromptDir() + "/" + PROMPT_FILE);
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "评测 Prompt 模板加载失败");
        }
    }

    /** 从模型输出中提取 JSON 对象（兼容 Markdown 代码块与前后缀文字） */
    private String extractJsonObject(String content) {
        String text = content.trim();
        if (text.startsWith("```")) {
            int newline = text.indexOf('\n');
            int end = text.lastIndexOf("```");
            if (newline >= 0 && end > newline) {
                text = text.substring(newline + 1, end).trim();
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        throw new BusinessException(ResultCode.INTERNAL_ERROR, "模型输出中未找到 JSON 对象");
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "评测结果序列化失败");
        }
    }

    private List<PointResultDTO> fromJson(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, PointResultDTO.class));
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    private List<ScorePointDTO> fromJsonPoints(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ScorePointDTO.class));
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }
}
