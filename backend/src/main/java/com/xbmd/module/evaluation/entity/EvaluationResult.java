package com.xbmd.module.evaluation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评测结果表（主观题）
 */
@Data
@TableName("evaluation_result")
public class EvaluationResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 答题记录 ID（唯一） */
    private Long answerId;

    /** 评测方式：AI / RULE（规则降级） */
    private String evalType;

    /** 评测得分（0-总分） */
    private Integer score;

    /** 评分点命中 JSON：[{"point":"...","earned":2,"total":2,"hit":true}] */
    private String pointResultsJson;

    /** 评语 */
    private String comment;

    /** 状态：PENDING待评测/SUCCESS成功/FAILED失败（可重试） */
    private String status;

    private LocalDateTime createTime;
}
