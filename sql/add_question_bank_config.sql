-- 增量脚本：题库板块配置表 + 种子（不重建已有表，不清数据）
USE `ai_interview_coach`;

-- 1. knowledge_category 补充唯一键（幂等）
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = 'ai_interview_coach' AND table_name = 'knowledge_category' AND index_name = 'uk_parent_name');
SET @sql := IF(@exist = 0,
               'ALTER TABLE `knowledge_category` ADD UNIQUE KEY `uk_parent_name` (`parent_id`, `name`)',
               'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2. 新表
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

CREATE TABLE IF NOT EXISTS `exam_paper_question` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `paper_id`    BIGINT NOT NULL COMMENT '套卷ID',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `sort`        INT    NOT NULL DEFAULT 0 COMMENT '题号顺序，从1开始',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_paper_question` (`paper_id`, `question_id`),
    KEY `idx_question` (`question_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='真题套卷题目关联表';

-- 3. 种子：知识分类树
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
(33, 3,  '数据库',            3, 3),
(331, 33, 'SQL',              3, 1),
(332, 33, '索引',             3, 2),
(333, 33, '事务',             3, 3),
(34, 3,  'Linux',             2, 4),
(41, 4,  '提示词工程',        2, 1),
(42, 4,  'RAG',               2, 2);

-- 4. 种子：专项练习卡片
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

-- 5. 种子：真题套卷
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

-- 6. 现有种子题挂知识点（幂等）
INSERT IGNORE INTO `question_knowledge` (`question_id`, `category_id`)
SELECT q.id, 112 FROM `question` q WHERE q.`stem` LIKE '%HashMap 的默认初始容量%';
INSERT IGNORE INTO `question_knowledge` (`question_id`, `category_id`)
SELECT q.id, 112 FROM `question` q WHERE q.`stem` LIKE '%转为红黑树%';
INSERT IGNORE INTO `question_knowledge` (`question_id`, `category_id`)
SELECT q.id, 114 FROM `question` q WHERE q.`stem` LIKE '%JVM 运行时数据区%';
INSERT IGNORE INTO `question_knowledge` (`question_id`, `category_id`)
SELECT q.id, 115 FROM `question` q WHERE q.`stem` LIKE '%Spring IOC 的核心思想%';
