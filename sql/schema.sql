-- =====================================================
-- AI智能面试与学习陪练平台 - 数据库初始化脚本
-- 数据库：MySQL 8.x
-- 说明：Task 2 阶段仅包含 user 表，后续任务按模块追加
-- =====================================================

CREATE DATABASE IF NOT EXISTS `ai_interview_coach`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `ai_interview_coach`;

-- 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `email`       VARCHAR(100) NOT NULL COMMENT '邮箱',
    `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER/ADMIN',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='用户表';

-- =====================================================
-- Task 3：知识体系与题库模块
-- =====================================================

-- 知识分类表（自关联树：领域(1) → 方向(2) → 知识点(3)）
CREATE TABLE IF NOT EXISTS `knowledge_category` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id`   BIGINT      NOT NULL DEFAULT 0 COMMENT '父分类ID，0 表示根',
    `name`        VARCHAR(50) NOT NULL COMMENT '分类名称',
    `level`       TINYINT     NOT NULL DEFAULT 1 COMMENT '层级：1领域 2方向 3知识点',
    `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_parent_name` (`parent_id`, `name`),
    KEY `idx_parent` (`parent_id`),
    KEY `idx_level` (`level`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='知识分类表';

-- 题目表
CREATE TABLE IF NOT EXISTS `question` (
    `id`                 BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `type`               VARCHAR(20) NOT NULL COMMENT '题型：SINGLE/MULTIPLE/JUDGE/SHORT',
    `difficulty`         TINYINT     NOT NULL DEFAULT 2 COMMENT '难度 1-5',
    `stem`               TEXT        NOT NULL COMMENT '题干',
    `options_json`       TEXT                 COMMENT '选项 JSON：[{"key":"A","text":"..."}]',
    `answer`             TEXT        NOT NULL COMMENT '参考答案',
    `score_points_json`  TEXT                 COMMENT '评分点 JSON：[{"point":"...","score":2}]（简答题）',
    `analysis`           TEXT                 COMMENT '解析',
    `source`             VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT '来源：MANUAL/AI',
    `fingerprint`        VARCHAR(64)          COMMENT '题目指纹（SHA-256），唯一索引',
    `status`             TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1上架 0下架/删除',
    `create_time`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_fingerprint` (`fingerprint`),
    KEY `idx_type_difficulty` (`type`, `difficulty`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='题目表';

-- 标签表
CREATE TABLE IF NOT EXISTS `tag` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(50) NOT NULL COMMENT '标签名',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='标签表';

-- 题目-标签关联表
CREATE TABLE IF NOT EXISTS `question_tag` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `question_id` BIGINT NOT NULL,
    `tag_id`      BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_tag` (`question_id`, `tag_id`),
    KEY `idx_tag` (`tag_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='题目-标签关联表';

-- 题目-知识点关联表
CREATE TABLE IF NOT EXISTS `question_knowledge` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `question_id` BIGINT NOT NULL,
    `category_id` BIGINT NOT NULL COMMENT '知识点分类ID（level=3）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_category` (`question_id`, `category_id`),
    KEY `idx_category` (`category_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='题目-知识点关联表';

-- =====================================================
-- Task 5：在线练习与答题模块
-- =====================================================

-- 练习记录表
CREATE TABLE IF NOT EXISTS `practice_record` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        BIGINT       NOT NULL COMMENT '用户ID',
    `mode`           VARCHAR(20)  NOT NULL COMMENT '练习方式：PAPER_BANK/AI/ADAPTIVE/WRONG',
    `condition_json` TEXT                  COMMENT '组题条件快照（JSON）',
    `total_count`    INT          NOT NULL DEFAULT 0 COMMENT '总题数',
    `correct_count`  INT          NOT NULL DEFAULT 0 COMMENT '答对题数',
    `score`          INT          NOT NULL DEFAULT 0 COMMENT '总得分（每题满分10，主观题按评测分）',
    `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0进行中 1已完成',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `finish_time`    DATETIME              COMMENT '完成时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='练习记录表';

-- 答题记录表
CREATE TABLE IF NOT EXISTS `answer_record` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `practice_id` BIGINT      NOT NULL COMMENT '练习ID',
    `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
    `question_id` BIGINT      NOT NULL COMMENT '题目ID',
    `user_answer` TEXT                 COMMENT '用户答案（选项key或文本）',
    `is_correct`  TINYINT              COMMENT '是否答对：1对 0错 NULL待评测',
    `score`       INT                  COMMENT '本题得分（客观题10/0，主观题按评测）',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作答时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_practice_question` (`practice_id`, `question_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_question` (`question_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='答题记录表';

-- =====================================================
-- Task 6：AI 评测模块（主观题）
-- =====================================================

-- 评测结果表
CREATE TABLE IF NOT EXISTS `evaluation_result` (
    `id`                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `answer_id`         BIGINT      NOT NULL COMMENT '答题记录ID',
    `eval_type`         VARCHAR(20) NOT NULL COMMENT '评测方式：AUTO/AI/RULE',
    `score`             INT                  COMMENT '评测得分（0-总分）',
    `point_results_json` TEXT               COMMENT '评分点命中 JSON：[{"point":"...","earned":2,"total":2,"hit":true}]',
    `comment`           TEXT                 COMMENT '评语',
    `status`            VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING待评测/SUCCESS成功/FAILED失败',
    `create_time`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_answer` (`answer_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='评测结果表';

-- =====================================================
-- Task 7 预置：错题表（Task 5 错题重练模式依赖）
-- =====================================================

CREATE TABLE IF NOT EXISTS `wrong_question` (
    `id`             BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT      NOT NULL COMMENT '用户ID',
    `question_id`    BIGINT      NOT NULL COMMENT '题目ID',
    `last_answer_id` BIGINT               COMMENT '最近一次答错的答题记录ID',
    `wrong_count`    INT         NOT NULL DEFAULT 1 COMMENT '累计答错次数',
    `status`         TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1未掌握 0已掌握/移除',
    `create_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='错题表';

-- =====================================================
-- 题库板块配置：专项练习卡片 + 真题套卷（对应前端题库首页/套卷详情页）
-- 说明：题目数据后续爬取填充，此处仅建表结构 + 卡片/套卷配置种子
-- =====================================================

-- 专项练习卡片表
CREATE TABLE IF NOT EXISTS `practice_card` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`                VARCHAR(50)  NOT NULL COMMENT '卡片名（Java/数组/哈希等）',
    `group_name`          VARCHAR(20)  NOT NULL COMMENT '模块分组：推荐/编程语言/数据结构/基础算法/计算机基础/数据库',
    `sort`                INT          NOT NULL DEFAULT 0 COMMENT '组内排序，越小越靠前',
    `keyword`             VARCHAR(50)           COMMENT '进入做题列表的关键词（题干模糊匹配）',
    `category_ids_json`   VARCHAR(500)          COMMENT '关联知识点分类ID JSON，如 [11,12]（组题更精确筛选，自动展开子级）',
    `related_majors_json` VARCHAR(255)          COMMENT '关联求职方向 JSON，如 ["Java","后端"]，用于“为你推荐”打标',
    `new_count`           INT          NOT NULL DEFAULT 0 COMMENT '新题角标数（后台可维护）',
    `status`              TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1上架 0下架',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_group_sort` (`group_name`, `sort`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='专项练习卡片表';

-- 真题套卷表
CREATE TABLE IF NOT EXISTS `exam_paper` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `company`        VARCHAR(50)  NOT NULL COMMENT '企业名称',
    `year`           INT          NOT NULL COMMENT '年份',
    `season`         VARCHAR(10)  NOT NULL COMMENT '场次季节：秋招/春招/社招',
    `round`          VARCHAR(50)           COMMENT '具体场次：如 9月17号/一面/第一批笔试',
    `position`       VARCHAR(50)           COMMENT '面向岗位：如 Java 工程师',
    `type`           VARCHAR(10)  NOT NULL COMMENT '类型：笔试/面试',
    `heat`           INT          NOT NULL DEFAULT 0 COMMENT '热度（浏览量）',
    `hot`            TINYINT      NOT NULL DEFAULT 0 COMMENT '是否热门企业：1是 0否',
    `question_count` INT          NOT NULL DEFAULT 0 COMMENT '收录题数（与关联表保持一致）',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1上架 0下架',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_type_heat` (`type`, `heat`),
    KEY `idx_company` (`company`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='真题套卷表';

-- 套卷-题目关联表
CREATE TABLE IF NOT EXISTS `exam_paper_question` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `paper_id`    BIGINT NOT NULL COMMENT '套卷ID',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `sort`        INT    NOT NULL DEFAULT 0 COMMENT '题号顺序，从1开始',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_paper_question` (`paper_id`, `question_id`),
    KEY `idx_question` (`question_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='真题套卷题目关联表';

-- =====================================================
-- 配置种子：知识分类树（领域→方向→知识点）+ 专项卡片 + 真题套卷
-- 说明：题目数据由爬虫/导入脚本后续填充；卡片与套卷是页面配置，先落库
-- =====================================================

-- 知识分类树种子（幂等，按 parent_id+name 防重）
INSERT IGNORE INTO `knowledge_category` (`id`, `parent_id`, `name`, `level`, `sort`) VALUES
(1,  0,  '编程语言',          1, 1),
(2,  0,  '数据结构与算法',     1, 2),
(3,  0,  '计算机基础',         1, 3),
(4,  0,  'AI与大模型',        1, 4),
(11, 1,  'Java',              2, 1),
(12, 1,  'Python',            2, 2),
(13, 1,  'C++',               2, 3),
(14, 1,  'JavaScript',        2, 4),
(111, 11, 'Java基础',         3, 1),
(112, 11, '集合框架',         3, 2),
(113, 11, '并发编程',         3, 3),
(114, 11, 'JVM',              3, 4),
(115, 11, 'Spring',           3, 5),
(21, 2,  '数据结构',          2, 1),
(211, 21, '数组',             3, 1),
(212, 21, '字符串',           3, 2),
(213, 21, '链表',             3, 3),
(214, 21, '栈',               3, 4),
(215, 21, '队列',             3, 5),
(216, 21, '树',               3, 6),
(217, 21, '哈希表',           3, 7),
(22, 2,  '基础算法',          2, 2),
(221, 22, '递归',             3, 1),
(222, 22, '动态规划',         3, 2),
(223, 22, '贪心',             3, 3),
(224, 22, '排序',             3, 4),
(31, 3,  '操作系统',          2, 1),
(311, 31, '进程与线程',       3, 1),
(312, 31, '内存管理',         3, 2),
(32, 3,  '网络基础',          2, 2),
(321, 32, 'TCP/IP',           3, 1),
(322, 32, 'HTTP',             3, 2),
(33, 3,  '数据库',            2, 3),
(331, 33, 'SQL',              3, 1),
(332, 33, '索引',             3, 2),
(333, 33, '事务',             3, 3),
(34, 3,  'Linux',             2, 4),
(41, 4,  '提示词工程',        2, 1),
(42, 4,  'RAG',               2, 2);

-- 专项练习卡片种子（与前端 mock/practice.ts 对齐）
INSERT IGNORE INTO `practice_card` (`name`, `group_name`, `sort`, `keyword`, `category_ids_json`, `related_majors_json`, `new_count`) VALUES
('Java',          '推荐',       1, 'Java',      '[11]',  '["Java","后端","全栈"]',     0),
('SQL',           '推荐',       2, 'SQL',       '[331]', '["Java","后端","数据"]',     0),
('操作系统',      '推荐',       3, '操作系统', '[31]',   '["Java","后端","嵌入式"]',   0),
('网络基础',      '推荐',       4, '网络',      '[32]',   '["Java","后端","前端"]',     0),
('数据库',        '推荐',       5, '数据库',    '[33]',   '["Java","后端","数据"]',     0),
('Linux',         '推荐',       6, 'Linux',     '[34]',   '["后端","运维"]',            0),
('提示词工程',    '推荐',       7, 'Prompt',    '[41]',   '["AI","产品"]',              100),
('RAG',           '推荐',       8, 'RAG',       '[42]',   '["AI","大模型"]',            192),
('数组',          '数据结构',   1, '数组',      '[211]', '["Java","前端","算法"]',     0),
('字符串',        '数据结构',   2, '字符串',    '[212]', '["Java","前端","算法"]',     0),
('链表',          '数据结构',   3, '链表',      '[213]', '["Java","算法"]',            0),
('栈',            '数据结构',   4, '栈',        '[214]', '["Java","算法"]',            0),
('队列',          '数据结构',   5, '队列',      '[215]', '["Java","算法"]',            0),
('树',            '数据结构',   6, '二叉树',    '[216]', '["Java","算法"]',            0),
('哈希',          '数据结构',   7, '哈希',      '[217]', '["Java","算法"]',            0),
('递归',          '基础算法',   1, '递归',      '[221]', '["Java","算法"]',            0),
('动态规划',      '基础算法',   2, '动态规划',  '[222]', '["算法"]',                   0),
('贪心',          '基础算法',   3, '贪心',      '[223]', '["算法"]',                   0),
('排序',          '基础算法',   4, '排序',      '[224]', '["Java","算法"]',            0),
('Python',        '编程语言',   1, 'Python',    '[12]',  '["Python","AI","数据"]',     0),
('C++',           '编程语言',   2, 'C++',       '[13]',  '["C++","嵌入式"]',           0),
('JavaScript',    '编程语言',   3, 'JavaScript','[14]',  '["前端","全栈"]',            0);

-- 真题套卷种子（与前端 mock/exam.ts 对齐）
INSERT IGNORE INTO `exam_paper` (`company`, `year`, `season`, `round`, `position`, `type`, `heat`, `hot`, `question_count`) VALUES
('华为',      2025, '秋招', '9月17号',      'AI 岗',         '笔试', 379, 1, 42),
('阿里云',    2025, '春招', '第一批笔试',   '研发岗',        '笔试', 155, 1, 38),
('美团',      2025, '春招', '综合能力测试', '产品&运营笔试', '笔试', 324, 1, 45),
('中国银行',  2025, '春招', '模拟卷2',      '通用岗',        '笔试', 481, 0, 60),
('中国移动',  2025, '春招', '模拟卷2',      '技术岗',        '笔试', 863, 0, 55),
('华为',      2025, '秋招', '9月3号',       'AI 岗',         '笔试', 551, 0, 40),
('中国联通',  2025, '秋招', '模拟卷10',     '综合类',        '笔试', 448, 0, 50),
('美团',      2025, '秋招', '第一批笔试',   '技术岗',        '笔试', 629, 1, 46),
('招商银行',  2025, '春招', '模拟卷',       '金融科技岗',    '笔试', 705, 0, 52),
('京东',      2025, '秋招', '第二批笔试',   'Java 工程师',   '笔试', 432, 1, 44),
('携程',      2025, '秋招', '第一批笔试',   '后端研发',      '笔试', 289, 1, 36),
('小米集团',  2025, '春招', '综合笔试',     '前端开发',      '笔试', 267, 1, 33),
('字节跳动',  2025, '秋招', '一面',         '后端研发',      '面试', 921, 0, 28),
('腾讯',      2025, '秋招', '二面',         'Java 工程师',   '面试', 756, 0, 31),
('阿里巴巴',  2025, '秋招', '一面',         '前端开发',      '面试', 634, 0, 26),
('美团',      2025, '秋招', '一面',         'Java 工程师',   '面试', 588, 0, 29),
('百度',      2025, '春招', '二面',         '算法工程师',    '面试', 473, 0, 24),
('京东',      2025, '春招', '一面',         'Python 后端',   '面试', 356, 0, 22);

-- 现有种子题挂知识点（幂等，按 uk_question_category 防重）
INSERT IGNORE INTO `question_knowledge` (`question_id`, `category_id`)
SELECT q.id, 112 FROM `question` q WHERE q.`stem` LIKE '%HashMap 的默认初始容量%';
INSERT IGNORE INTO `question_knowledge` (`question_id`, `category_id`)
SELECT q.id, 112 FROM `question` q WHERE q.`stem` LIKE '%转为红黑树%';
INSERT IGNORE INTO `question_knowledge` (`question_id`, `category_id`)
SELECT q.id, 114 FROM `question` q WHERE q.`stem` LIKE '%JVM 运行时数据区%';
INSERT IGNORE INTO `question_knowledge` (`question_id`, `category_id`)
SELECT q.id, 115 FROM `question` q WHERE q.`stem` LIKE '%Spring IOC 的核心思想%';
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


-- =====================================================


-- =====================================================


-- =====================================================


-- =====================================================


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
