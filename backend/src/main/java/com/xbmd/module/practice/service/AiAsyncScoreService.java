package com.xbmd.module.practice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbmd.module.ai.util.AiScoreCoordinator;
import com.xbmd.module.evaluation.dto.EvaluationVO;
import com.xbmd.module.evaluation.entity.EvaluationResult;
import com.xbmd.module.evaluation.mapper.EvaluationResultMapper;
import com.xbmd.module.evaluation.service.AiEvaluationService;
import com.xbmd.module.practice.entity.AnswerRecord;
import com.xbmd.module.practice.mapper.AnswerRecordMapper;
import com.xbmd.module.question.dto.ScorePointDTO;
import com.xbmd.module.question.entity.Question;
import com.xbmd.module.stats.service.StatsService;
import com.xbmd.module.wrong.service.WrongQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 答题卡主观题异步评分服务：
 * 提交答案后立即返回，后台线程池执行 AI 评测并回填分数、收录错题本、更新学习画像。
 * 交卷时由 PracticeServiceImpl 通过 AiScoreCoordinator 等待未评完的题目补齐后再结算。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAsyncScoreService {

    private static final String STATUS_PENDING = "PENDING";

    private final AiEvaluationService aiEvaluationService;
    private final AnswerRecordMapper answerRecordMapper;
    private final EvaluationResultMapper evaluationResultMapper;
    private final WrongQuestionService wrongQuestionService;
    private final StatsService statsService;
    private final AiScoreCoordinator coordinator;
    private final ObjectMapper objectMapper;

    /**
     * 异步评测一道主观题并回填。
     * 必须先由外部（PracticeServiceImpl）以跨 bean 方式调用，@Async 才会生效。
     */
    @Async("aiScoreExecutor")
    public void scoreSubjective(Long answerId, Question question, String userAnswer, Long userId) {
        String key = "practice:" + answerId;
        coordinator.register(key);
        log.info("评分任务开始 answerId={} userId={}", answerId, userId);
        try {
            // 1. 确保有一条 PENDING 占位，前端提交后即可显示"待评测"
            ensurePending(answerId);
            // 2. AI 评测（内部 upsert：占位 PENDING → SUCCESS / FAILED）
            EvaluationVO evaluation = aiEvaluationService.evaluate(answerId, question, userAnswer);
            log.info("evaluate 返回 answerId={} score={} status={} evalType={}", answerId,
                    evaluation == null ? null : evaluation.getScore(),
                    evaluation == null ? null : evaluation.getStatus(),
                    evaluation == null ? null : evaluation.getEvalType());
            if (evaluation == null || evaluation.getScore() == null) {
                log.warn("主观题 {} 评测无分数返回", answerId);
                return;
            }
            // 3. 回填分数与对错（直接 UPDATE，不读实体，避免 selectById 读不到导致漏回填）
            int totalScore = totalScoreOf(question);
            int isCorrect = evaluation.getScore() >= totalScore * 0.6 ? 1 : 0;
            log.info("判定 answerId={} score={} total={} isCorrect={}", answerId, evaluation.getScore(), totalScore, isCorrect);
            int rows = answerRecordMapper.update(null, new LambdaUpdateWrapper<AnswerRecord>()
                    .set(AnswerRecord::getScore, evaluation.getScore())
                    .set(AnswerRecord::getIsCorrect, isCorrect)
                    .eq(AnswerRecord::getId, answerId));
            log.info("回填 answerId={} score={} isCorrect={} 影响行数={}", answerId, evaluation.getScore(), isCorrect, rows);
            if (rows == 0) {
                log.warn("回填影响 0 行：answerId={} 记录不存在", answerId);
            }
            // 4. 答错自动收录错题本（去重由唯一约束处理）
            if (isCorrect == 0) {
                wrongQuestionService.recordWrong(userId, question.getId(), answerId);
            }
            // 5. 更新知识点统计（学习画像）
            statsService.updateStats(userId, question.getId(), isCorrect == 1);
        } catch (Exception e) {
            log.warn("主观题 {} 异步评测异常：{}", answerId, e.getMessage(), e);
        } finally {
            coordinator.complete(key);
        }
    }

    private void ensurePending(Long answerId) {
        EvaluationResult exist = evaluationResultMapper.selectOne(
                new LambdaQueryWrapper<EvaluationResult>().eq(EvaluationResult::getAnswerId, answerId));
        if (exist == null) {
            EvaluationResult pending = new EvaluationResult();
            pending.setAnswerId(answerId);
            pending.setEvalType("AI");
            pending.setStatus(STATUS_PENDING);
            evaluationResultMapper.insert(pending);
        }
    }

    private int totalScoreOf(Question question) {
        if (!StringUtils.hasText(question.getScorePointsJson())) {
            return 10;
        }
        try {
            List<ScorePointDTO> points = objectMapper.readValue(question.getScorePointsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ScorePointDTO.class));
            if (points == null || points.isEmpty()) {
                return 10;
            }
            return points.stream().mapToInt(p -> p.getScore() == null ? 0 : p.getScore()).sum();
        } catch (JsonProcessingException e) {
            return 10;
        }
    }
}
