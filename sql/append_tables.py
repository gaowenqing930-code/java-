# -*- coding: utf-8 -*-
import io

add = r"""
-- =====================================================
-- Task 8/9：知识点统计表 + 操作日志表
-- =====================================================

-- 用户知识点统计表（答题后按题目挂载的知识点更新）
CREATE TABLE IF NOT EXISTS `knowledge_stat` (
    `id`           BIGINT    NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT    NOT NULL COMMENT '用户ID',
    `category_id`  BIGINT    NOT NULL COMMENT '知识点ID(level=3)',
    `total_count`  INT       NOT NULL DEFAULT 0 COMMENT '累计答题数',
    `correct_count` INT      NOT NULL DEFAULT 0 COMMENT '累计答对数',
    `mastery`      DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '掌握度0-100（加权正确率）',
    `update_time`  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_category` (`user_id`, `category_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='知识点统计表';

-- 操作日志表（管理操作 + AI 告警留痕）
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT               COMMENT '操作用户ID',
    `action`      VARCHAR(50) NOT NULL COMMENT '动作：ADMIN_USER_UPDATE/ADMIN_QUESTION_STATUS/LOGIN/AI_FAIL 等',
    `detail`      VARCHAR(500)         COMMENT '详情',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_action` (`action`),
    KEY `idx_create` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='操作日志表';
"""

with io.open('sql/schema.sql', 'a', encoding='utf-8') as f:
    f.write(add)
print('appended ok')
