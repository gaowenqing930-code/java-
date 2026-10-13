package com.xbmd.module.auth.controller;

import com.xbmd.common.Result;
import com.xbmd.module.auth.dto.UserVO;
import com.xbmd.module.auth.service.UserService;
import com.xbmd.security.RequireRole;
import com.xbmd.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端接口示例：仅 ADMIN 角色可访问（Task 9 将扩展完整后台管理）
 */
@RestController
@RequestMapping("/api/admin")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping("/profile")
    public Result<UserVO> profile() {
        return Result.success(userService.getProfile(UserContext.getUserId()));
    }
}
