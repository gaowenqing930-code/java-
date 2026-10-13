package com.xbmd.module.admin.controller;

import com.xbmd.common.PageResult;
import com.xbmd.common.Result;
import com.xbmd.module.admin.entity.OperationLog;
import com.xbmd.module.admin.service.OperationLogService;
import com.xbmd.security.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台管理：操作日志查询（仅 ADMIN）
 */
@RestController
@RequestMapping("/api/admin/logs")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class AdminLogController {

    private final OperationLogService operationLogService;

    /** 日志分页（按动作筛选） */
    @GetMapping
    public Result<PageResult<OperationLog>> list(@RequestParam(required = false) String action,
                                                 @RequestParam(required = false) Long userId,
                                                 @RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(operationLogService.pageQuery(action, userId, pageNum, pageSize));
    }
}
