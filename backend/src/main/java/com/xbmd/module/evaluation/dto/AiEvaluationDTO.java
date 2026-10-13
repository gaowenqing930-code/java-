package com.xbmd.module.evaluation.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 评测结构化输出（模型返回 JSON 的映射）
 */
@Data
public class AiEvaluationDTO {

    /** 总分（0-总分） */
    private Integer score;

    /** 评分点逐条命中 */
    private List<PointResultDTO> pointResults;

    /** 评语 */
    private String comment;
}
