package com.xbmd.module.interview.dto;

import lombok.Data;

/**
 * AI 单题评分结果（模型输出）
 */
@Data
public class ScoreDTO {

    private Integer score;

    private String comment;
}
