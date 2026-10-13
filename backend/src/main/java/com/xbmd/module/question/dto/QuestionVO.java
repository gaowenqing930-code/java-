package com.xbmd.module.question.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目返回体（不包含内部结构字段的原始 JSON）
 */
@Data
public class QuestionVO {

    private Long id;
    private String type;
    private Integer difficulty;
    private String stem;
    private List<OptionDTO> options;
    private String answer;
    private List<ScorePointDTO> scorePoints;
    private String analysis;
    private String source;
    private Integer status;
    private List<Long> categoryIds;
    private List<String> categoryNames;
    private LocalDateTime createTime;
}
