package com.xbmd.module.auth.controller;

import com.xbmd.common.Result;
import com.xbmd.module.auth.dto.LoginRequest;
import com.xbmd.module.auth.dto.LoginResponse;
import com.xbmd.module.auth.dto.RegisterRequest;
import com.xbmd.module.auth.dto.UserVO;
import com.xbmd.module.auth.service.UserService;
import com.xbmd.security.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册 / 登录（公开）
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final TokenBlacklistService blacklistService;

    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(userService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    /**
     * 登出：把当前 token 的 jti 写入 Redis 黑名单（TTL=剩余有效期），
     * 之后该 token 访问任何接口都会被拦截器拒绝。
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            blacklistService.blacklistToken(authHeader.substring(7));
        }
        return Result.success(null);
    }
}
