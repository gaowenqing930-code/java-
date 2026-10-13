package com.xbmd.module.question.dto;

import lombok.Data;

/**
 * 题目组合筛选条件（领域+方向+知识点+题型+难度+关键词）
 */
@Data
public class QuestionQuery {

    /** 领域 ID（level=1） */
    private Long domainId;

    /** 方向 ID（level=2） */
    private Long directionId;

    /** 知识点 ID（level=3） */
    private Long pointId;

    /** 题型 */
    private String type;

    /** 难度 1-5 */
    private Integer difficulty;

    /** 题干关键词 */
    private String keyword;

    /** 状态：默认 1 上架；管理员可传 null 查全部 */
    private Integer status = 1;

    private long pageNum = 1;
    private long pageSize = 10;
}
