package com.xbmd.module.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录成功返回：JWT Token + 用户信息
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private UserVO user;
}
