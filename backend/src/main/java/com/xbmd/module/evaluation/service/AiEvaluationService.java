package com.xbmd.module.evaluation.service;

import com.xbmd.module.evaluation.dto.EvaluationVO;
import com.xbmd.module.question.entity.Question;

/**
 * AI 评测服务（主观题）：评分点逐条命中 + 评语
 */
public interface AiEvaluationService {

    /**
     * 对主观题作答进行评测并落库（answer_id 唯一约束，重复评测覆盖）
     * AI 可用走模型评测；未配置或失败走规则降级评分；模型输出解析失败标记 FAILED 待重试
     *
     * @param answerId   答题记录 ID
     * @param question   题目
     * @param userAnswer 用户作答文本
     */
    EvaluationVO evaluate(Long answerId, Question question, String userAnswer);

    /**
     * 重试评测（仅本人可见的答题记录）
     */
    EvaluationVO retry(Long answerId, Long userId);
}
