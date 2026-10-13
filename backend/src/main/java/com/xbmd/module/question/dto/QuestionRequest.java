package com.xbmd.module.question.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 题目新增/修改请求
 */
@Data
public class QuestionRequest {

    @NotBlank(message = "题型不能为空")
    @Pattern(regexp = "SINGLE|MULTIPLE|JUDGE|SHORT", message = "题型仅支持 SINGLE/MULTIPLE/JUDGE/SHORT")
    private String type;

    @NotNull(message = "难度不能为空")
    @Min(value = 1, message = "难度范围 1-5")
    @Max(value = 5, message = "难度范围 1-5")
    private Integer difficulty;

    @NotBlank(message = "题干不能为空")
    private String stem;

    /** 选择题选项 */
    private List<OptionDTO> options;

    @NotBlank(message = "参考答案不能为空")
    private String answer;

    /** 简答题评分点 */
    private List<ScorePointDTO> scorePoints;

    private String analysis;

    /** 关联知识点 ID（level=3），至少 1 个 */
    @NotEmpty(message = "至少关联一个知识点")
    private List<Long> categoryIds;
}
