package com.xbmd.module.practice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 题型配额：真题/组合练习按题型指定题目数量
 */
@Data
public class TypeQuota {

    @NotBlank(message = "题型不能为空")
    @Pattern(regexp = "SINGLE|MULTIPLE|JUDGE|SHORT", message = "题型仅支持 SINGLE/MULTIPLE/JUDGE/SHORT")
    private String type;

    @NotNull(message = "题型数量不能为空")
    @Min(value = 1, message = "题型数量范围 1-10")
    @Max(value = 10, message = "题型数量范围 1-10")
    private Integer count;
}
