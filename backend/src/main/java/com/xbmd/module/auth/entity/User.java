package com.xbmd.module.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表
 */
@Data
@TableName("`user`")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** 邮箱 */
    private String email;

    /** BCrypt 加密后的密码 */
    private String password;

    /** 角色：USER / ADMIN */
    private String role;

    /** 状态：1 正常，0 禁用 */
    private Integer status;

    /** 专业方向（多选，逗号分隔） */
    private String major;

    /** 目标职业 */
    private String targetJob;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
