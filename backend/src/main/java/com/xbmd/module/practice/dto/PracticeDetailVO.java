package com.xbmd.module.practice.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 练习详情（含逐题结果）
 */
@Data
public class PracticeDetailVO {

    private Long id;

    /** 练习方式 */
    private String mode;

    /** 组题条件快照 */
    private String conditionJson;

    private Integer totalCount;

    private Integer correctCount;

    /** 总得分 */
    private Integer score;

    /** 状态：0进行中 1已完成 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime finishTime;

    /** 用时（秒，完成后有值） */
    private Long durationSeconds;

    /** 逐题明细 */
    private List<PracticeItemVO> items;
}
