package com.xbmd.module.practice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 创建练习请求（四种模式 + 组题条件）
 */
@Data
public class CreatePracticeRequest {

    /** 练习方式：PAPER_BANK 题库 / EXAM 真题套卷 / AI 智能出题 / ADAPTIVE 自适应 / WRONG 错题重练 */
    @NotBlank(message = "练习方式不能为空")
    @Pattern(regexp = "PAPER_BANK|EXAM|AI|ADAPTIVE|WRONG", message = "练习方式仅支持 PAPER_BANK/EXAM/AI/ADAPTIVE/WRONG")
    private String mode;

    /** 目标知识点 ID（AI 模式必填） */
    private Long categoryId;

    /** 题干关键词（题库模式） */
    private String keyword;

    /** 题型（可选，题库模式过滤） */
    @Pattern(regexp = "SINGLE|MULTIPLE|JUDGE|SHORT", message = "题型仅支持 SINGLE/MULTIPLE/JUDGE/SHORT")
    private String type;

    /** 难度 1-5（可选） */
    @Min(value = 1, message = "难度范围 1-5")
    @Max(value = 5, message = "难度范围 1-5")
    private Integer difficulty;

    /** 题目数量 */
    @NotNull(message = "题目数量不能为空")
    @Min(value = 1, message = "数量范围 1-50")
    @Max(value = 50, message = "数量范围 1-50")
    private Integer count = 10;

    /** 卷子 ID（EXAM 模式必填）：按预置卷子的固定题目整套作答 */
    private Long paperId;

    /** 题型配额（保留兼容，不再用于 EXAM 组卷） */
    @Valid
    private List<TypeQuota> typeQuota;
}
