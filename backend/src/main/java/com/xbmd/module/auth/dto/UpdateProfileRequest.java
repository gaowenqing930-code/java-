package com.xbmd.module.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新当前用户求职目标请求（专业 + 目标职业）
 */
@Data
public class UpdateProfileRequest {

    /** 专业方向（多选，逗号分隔），可空 */
    @Size(max = 100, message = "专业方向不能超过 100 字符")
    private String major;

    /** 目标职业，可空 */
    @Size(max = 50, message = "目标职业不能超过 50 字符")
    private String targetJob;
}
