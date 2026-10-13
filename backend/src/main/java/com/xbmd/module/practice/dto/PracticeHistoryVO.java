package com.xbmd.module.practice.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 练习历史记录（本人已完成练习列表）
 */
@Data
public class PracticeHistoryVO {

    private Long id;

    /** 练习方式 */
    private String mode;

    private Integer totalCount;

    private Integer correctCount;

    /** 总得分 */
    private Integer score;

    /** 已答题数 */
    private Integer answeredCount;

    /** 用时（秒） */
    private Long durationSeconds;

    private LocalDateTime createTime;

    private LocalDateTime finishTime;
}
