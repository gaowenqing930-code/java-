package com.xbmd.module.question.dto;

import lombok.Data;

/**
 * 简答题评分点
 */
@Data
public class ScorePointDTO {

    /** 评分点描述 */
    private String point;

    /** 分值 */
    private Integer score;
}
