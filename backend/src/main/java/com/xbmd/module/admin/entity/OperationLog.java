package com.xbmd.module.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志表（管理操作 + AI 告警留痕）
 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作用户 ID */
    private Long userId;

    /** 动作：ADMIN_USER_UPDATE / ADMIN_QUESTION_STATUS / AI_FAIL 等 */
    private String action;

    /** 详情 */
    private String detail;

    private LocalDateTime createTime;
}
