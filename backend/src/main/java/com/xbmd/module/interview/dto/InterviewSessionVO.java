package com.xbmd.module.interview.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 面试历史列表项
 */
@Data
public class InterviewSessionVO {

    private Long sessionId;

    /** 总分（0-100） */
    private Integer totalScore;

    /** 已答题数 */
    private Integer answeredCount;

    /** CREATED / IN_PROGRESS / FINISHED */
    private String status;

    /** 技术栈（逗号分隔），便于用户区分场次 */
    private String techStack;

    private LocalDateTime createTime;

    private LocalDateTime finishTime;
}
