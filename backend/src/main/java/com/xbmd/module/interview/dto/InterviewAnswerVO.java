package com.xbmd.module.interview.dto;

import lombok.Data;

/**
 * 作答响应：未结束返回下一题，结束返回完整结果；
 * 出题失败返回 needRetry=true（答案已保存，可调 next 接口重试出题）
 */
@Data
public class InterviewAnswerVO {

    private boolean finished;

    private InterviewQuestionVO currentQuestion;

    private InterviewResultVO result;

    /** AI 出下一题失败：答案已保存，需前端引导用户重试出题 */
    private boolean needRetry;
}
