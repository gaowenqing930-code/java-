package com.xbmd.module.evaluation.dto;

import lombok.Data;

/**
 * 评分点命中结果
 */
@Data
public class PointResultDTO {

    /** 评分点描述 */
    private String point;

    /** 实际得分 */
    private Integer earned;

    /** 该点评分点满分 */
    private Integer total;

    /** 是否命中 */
    private Boolean hit;
}
