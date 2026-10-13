package com.xbmd.module.evaluation.dto;

import lombok.Data;

import java.util.List;

/**
 * 评测结果返回体
 */
@Data
public class EvaluationVO {

    private Long id;

    /** 评测方式：AI / RULE */
    private String evalType;

    /** 评测得分 */
    private Integer score;

    /** 评分点逐条命中 */
    private List<PointResultDTO> pointResults;

    /** 评语 */
    private String comment;

    /** 状态：PENDING/SUCCESS/FAILED */
    private String status;
}
