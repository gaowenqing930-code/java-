# -*- coding: utf-8 -*-
"""修复 schema.sql 追加段（反引号被 PowerShell 吞掉）并执行建表"""
import io
import re
import sys

sys.path.insert(0, r"C:\Program Files\Python313\Lib\site-packages")

PATH = r"D:\Codes\ai-interview-coach\sql\schema.sql"
sql = io.open(PATH, encoding="utf-8").read()

# 删除旧的损坏追加段（从面试标记开始）
idx = sql.find("-- AI 智能面试")
if idx > 0:
    sql = sql[:idx].rstrip() + "\n"

add = """

-- =====================================================
-- AI 智能面试：会话 + 面试题
-- =====================================================

CREATE TABLE IF NOT EXISTS `interview_session` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
    `resume_text` TEXT                 COMMENT '简历文本（原始）',
    `tech_stack`  VARCHAR(500)         COMMENT '技术栈（逗号分隔）',
    `projects`    TEXT                 COMMENT '项目列表 JSON',
    `status`      VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/IN_PROGRESS/FINISHED',
    `total_score` INT         NOT NULL DEFAULT 0 COMMENT '总分（每题满分10，共100）',
    `summary`     TEXT                 COMMENT '总体评价',
    `suggestion`  TEXT                 COMMENT '改进建议',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `finish_time` DATETIME             COMMENT '结束时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='AI 面试会话';

CREATE TABLE IF NOT EXISTS `interview_question` (
    `id`                 BIGINT      NOT NULL AUTO_INCREMENT,
    `session_id`         BIGINT      NOT NULL COMMENT '会话ID',
    `question_no`        INT         NOT NULL COMMENT '题号 1-10',
    `question`           TEXT                 COMMENT '面试题',
    `score_points_json`  TEXT                 COMMENT '评分点 [{point,score}]',
    `reference_answer`   TEXT                 COMMENT '参考回答',
    `user_answer`        TEXT                 COMMENT '用户回答',
    `ai_score`           INT                  COMMENT 'AI 评分 0-10',
    `ai_comment`         TEXT                 COMMENT 'AI 评语',
    `status`             VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SCORED',
    PRIMARY KEY (`id`),
    KEY `idx_session` (`session_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='AI 面试题目与评分';
"""
sql += add
io.open(PATH, "w", encoding="utf-8").write(sql)
print("schema.sql fixed")

# 执行建表
stmts = re.findall(r"CREATE TABLE IF NOT EXISTS `interview_\w+`.*?ENGINE = InnoDB[^;]*;", sql, re.S)
print("match count:", len(stmts))
conn = pymysql.connect(host="127.0.0.1", user="root", password="516888", database="ai_interview_coach", charset="utf8mb4")
cur = conn.cursor()
for s in stmts:
    cur.execute(s)
    print("executed:", s.split("`")[1])
conn.commit()
conn.close()
print("DONE")
