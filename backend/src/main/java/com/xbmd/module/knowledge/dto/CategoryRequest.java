package com.xbmd.module.knowledge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分类新增/修改请求
 */
@Data
public class CategoryRequest {

    /** 父分类 ID，0 表示新建根级（领域） */
    private Long parentId = 0L;

    @NotBlank(message = "分类名称不能为空")
    private String name;

    @NotNull(message = "层级不能为空")
    @Min(value = 1, message = "层级范围 1-3")
    @Max(value = 3, message = "层级范围 1-3")
    private Integer level;

    private Integer sort = 0;
}
