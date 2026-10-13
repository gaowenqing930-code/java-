package com.xbmd.module.auth.controller;

import com.xbmd.common.Result;
import com.xbmd.module.auth.dto.UpdateProfileRequest;
import com.xbmd.module.auth.dto.UserVO;
import com.xbmd.module.auth.service.UserService;
import com.xbmd.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户信息接口（需登录，数据由 Token 中解析的用户 ID 绑定）
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public Result<UserVO> profile() {
        return Result.success(userService.getProfile(UserContext.getUserId()));
    }

    /** 更新求职目标（专业 + 目标职业） */
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.success(userService.updateProfile(
                UserContext.getUserId(), request.getMajor(), request.getTargetJob()));
    }
}
