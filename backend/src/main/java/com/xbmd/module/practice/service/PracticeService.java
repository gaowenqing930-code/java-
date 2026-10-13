package com.xbmd.module.practice.service;

import com.xbmd.module.practice.dto.AnswerSubmitRequest;
import com.xbmd.module.practice.dto.AnswerSubmitResult;
import com.xbmd.module.practice.dto.CreatePracticeRequest;
import com.xbmd.module.practice.dto.PracticeDetailVO;
import com.xbmd.module.practice.dto.PracticeHistoryVO;

import java.util.List;

/**
 * 在线练习服务：创建练习、提交答案、判分、交卷
 */
public interface PracticeService {

    /**
     * 创建练习（题库/AI/自适应/错题重练四种模式）并组题
     */
    PracticeDetailVO create(CreatePracticeRequest request, Long userId);

    /**
     * 提交单题答案（客观题即时判分，主观题触发 AI 评测；(practiceId, questionId) 幂等）
     */
    AnswerSubmitResult submitAnswer(Long practiceId, AnswerSubmitRequest request, Long userId);

    /**
     * 交卷：批量提交剩余答案并结算练习成绩
     */
    PracticeDetailVO submitAll(Long practiceId, List<AnswerSubmitRequest> answers, Long userId);

    /**
     * 练习详情（含逐题结果；仅本人可见）
     */
    PracticeDetailVO detail(Long practiceId, Long userId);

    /**
     * 历史答题记录：本人已完成练习，按时间倒序（最多 100 条）
     */
    List<PracticeHistoryVO> history(Long userId);
}
