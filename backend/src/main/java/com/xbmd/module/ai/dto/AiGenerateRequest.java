package com.xbmd.module.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * AI 出题请求
 */
@Data
public class AiGenerateRequest {

    /** 目标知识点 ID（level=3） */
    @NotNull(message = "知识点不能为空")
    private Long categoryId;

    @NotBlank(message = "题型不能为空")
    @Pattern(regexp = "SINGLE|MULTIPLE|JUDGE|SHORT", message = "题型仅支持 SINGLE/MULTIPLE/JUDGE/SHORT")
    private String type;

    @NotNull(message = "难度不能为空")
    @Min(value = 1, message = "难度范围 1-5")
    @Max(value = 5, message = "难度范围 1-5")
    private Integer difficulty;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量范围 1-10")
    @Max(value = 10, message = "数量范围 1-10")
    private Integer count;
}
