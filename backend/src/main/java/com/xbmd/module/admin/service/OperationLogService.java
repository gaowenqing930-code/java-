package com.xbmd.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xbmd.common.PageResult;
import com.xbmd.module.admin.entity.OperationLog;
import com.xbmd.module.admin.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 操作日志服务：记录 + 分页查询
 */
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    /** 记录一条操作日志（管理操作 / AI 告警） */
    public void log(Long userId, String action, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setDetail(detail == null || detail.length() <= 500 ? detail : detail.substring(0, 500));
        operationLogMapper.insert(log);
    }

    /** 分页查询（按动作/关键词筛选） */
    public PageResult<OperationLog> pageQuery(String action, Long userId, long pageNum, long pageSize) {
        LambdaQueryWrapper<OperationLog> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(action)) {
            qw.eq(OperationLog::getAction, action.trim());
        }
        if (userId != null) {
            qw.eq(OperationLog::getUserId, userId);
        }
        qw.orderByDesc(OperationLog::getCreateTime);
        Page<OperationLog> page = operationLogMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        return PageResult.of(page);
    }
}
