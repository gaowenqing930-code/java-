package com.xbmd.module.admin.controller;

import com.xbmd.common.PageResult;
import com.xbmd.common.Result;
import com.xbmd.module.admin.service.OperationLogService;
import com.xbmd.module.auth.dto.UserVO;
import com.xbmd.module.auth.service.UserService;
import com.xbmd.security.RequireRole;
import com.xbmd.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台管理：用户管理（仅 ADMIN）
 */
@RestController
@RequestMapping("/api/admin/users")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final OperationLogService operationLogService;

    /** 用户分页列表 */
    @GetMapping
    public Result<PageResult<UserVO>> list(@RequestParam(required = false) String keyword,
                                           @RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(userService.pageUsers(keyword, pageNum, pageSize));
    }

    /** 启用/禁用用户 */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateUserStatus(id, status);
        operationLogService.log(UserContext.getUserId(), "ADMIN_USER_UPDATE",
                "用户 " + id + " 状态改为 " + status);
        return Result.success(null);
    }
}
