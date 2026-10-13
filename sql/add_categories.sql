-- =====================================================
-- 分类树扩展：支持牛客题库导入（45 张卡 250+ 题挂载）
-- 执行：mysql -uroot -p516888 --default-character-set=utf8mb4 -e "source D:/Codes/ai-interview-coach/sql/add_categories.sql"
-- =====================================================
USE ai_interview_coach;

-- 1) 编程语言（领域1）新增方向（level 2）
INSERT IGNORE INTO knowledge_category (id, parent_id, name, level) VALUES
(15, 1, 'Go', 2),
(16, 1, 'C语言', 2),
(17, 1, 'Kotlin', 2),
(18, 1, 'Swift', 2),
(19, 1, 'HTML/CSS', 2);

-- 已有方向补充"综合"知识点（level 3，供爬取题挂载）
INSERT IGNORE INTO knowledge_category (id, parent_id, name, level) VALUES
(116, 11, 'Java综合', 3),
(121, 12, 'Python综合', 3),
(131, 13, 'C++综合', 3),
(141, 14, 'JavaScript综合', 3);

-- 新增方向的综合知识点（level 3）
INSERT IGNORE INTO knowledge_category (id, parent_id, name, level) VALUES
(151, 15, 'Go综合', 3),
(161, 16, 'C语言综合', 3),
(171, 17, 'Kotlin综合', 3),
(181, 18, 'Swift综合', 3),
(191, 19, 'HTML/CSS综合', 3);

-- 2) 计算机基础（领域3）新增方向
INSERT IGNORE INTO knowledge_category (id, parent_id, name, level) VALUES
(35, 3, '设计模式', 2),
(36, 3, '编译和体系结构', 2),
(37, 3, '软件工程', 2);

-- 数据结构方向补充知识点（图/堆/查找）
INSERT IGNORE INTO knowledge_category (id, parent_id, name, level) VALUES
(218, 21, '图', 3),
(219, 21, '堆', 3),
(2110, 21, '查找', 3);

-- 计算机基础已有方向补充综合知识点 + 新方向综合
INSERT IGNORE INTO knowledge_category (id, parent_id, name, level) VALUES
(313, 31, '操作系统综合', 3),
(323, 32, '网络基础综合', 3),
(334, 33, '数据库综合', 3),
(341, 34, 'Linux综合', 3),
(351, 35, '设计模式综合', 3),
(361, 36, '编译原理综合', 3),
(371, 37, '软件工程综合', 3);

-- 3) 中间件（新领域5）
INSERT IGNORE INTO knowledge_category (id, parent_id, name, level) VALUES
(5, 0, '中间件', 1),
(51, 5, 'Redis', 2),
(52, 5, 'Kafka', 2),
(53, 5, 'Hadoop', 2),
(54, 5, 'Hive', 2),
(55, 5, 'ZooKeeper', 2);

INSERT IGNORE INTO knowledge_category (id, parent_id, name, level) VALUES
(511, 51, 'Redis综合', 3),
(521, 52, 'Kafka综合', 3),
(531, 53, 'Hadoop综合', 3),
(541, 54, 'Hive综合', 3),
(551, 55, 'ZooKeeper综合', 3);

-- 4) AI与大模型（领域4）扩展
INSERT IGNORE INTO knowledge_category (id, parent_id, name, level) VALUES
(43, 4, '机器学习', 2),
(44, 4, '深度学习', 2),
(45, 4, '概率论与数理统计', 2),
(46, 4, '数据挖掘', 2),
(47, 4, '计算机视觉', 2),
(48, 4, '大模型概念', 2),
(49, 4, '微调', 2),
(410, 4, 'Agent', 2);

INSERT IGNORE INTO knowledge_category (id, parent_id, name, level) VALUES
(411, 41, '提示词工程综合', 3),
(421, 42, 'RAG综合', 3),
(431, 43, '机器学习综合', 3),
(441, 44, '深度学习综合', 3),
(451, 45, '概率统计综合', 3),
(461, 46, '数据挖掘综合', 3),
(471, 47, '计算机视觉综合', 3),
(481, 48, '大模型概念综合', 3),
(491, 49, '微调综合', 3),
(4101, 410, 'Agent综合', 3);

-- 5) 框架技术（新领域6）
INSERT IGNORE INTO knowledge_category (id, parent_id, name, level) VALUES
(6, 0, '框架技术', 1),
(61, 6, 'React', 2),
(62, 6, 'Spring', 2),
(63, 6, 'Vue', 2);

INSERT IGNORE INTO knowledge_category (id, parent_id, name, level) VALUES
(611, 61, 'React综合', 3),
(621, 62, 'Spring综合', 3),
(631, 63, 'Vue综合', 3);

SELECT COUNT(*) AS total_categories FROM knowledge_category;
