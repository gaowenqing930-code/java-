package com.xbmd.module.auth.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 用户信息返回体（不包含密码）
 */
@Data
@Builder
public class UserVO {

    private Long id;
    private String username;
    private String email;
    private String role;

    /** 状态：1正常 0禁用 */
    private Integer status;

    /** 专业方向（多选，逗号分隔） */
    private String major;

    /** 目标职业 */
    private String targetJob;
}
