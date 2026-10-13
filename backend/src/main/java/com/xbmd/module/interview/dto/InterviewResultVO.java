package com.xbmd.module.interview.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 面试结果：总分 + 评价 + 建议 + 逐题明细
 */
@Data
public class InterviewResultVO {

    private Long sessionId;

    private Integer totalScore;

    private String summary;

    private String suggestion;

    private LocalDateTime finishTime;

    private List<InterviewQuestionVO> questions;
}
