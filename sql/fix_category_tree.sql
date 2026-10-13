-- 修复：重建知识分类树（清理旧树与错乱的新树，重新挂载种子题）
USE `ai_interview_coach`;

DELETE FROM `question_knowledge`;
DELETE FROM `knowledge_category`;

INSERT INTO `knowledge_category` (`id`, `parent_id`, `name`, `level`, `sort`) VALUES
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

-- 种子题重新挂载知识点
INSERT INTO `question_knowledge` (`question_id`, `category_id`)
SELECT q.id, 112 FROM `question` q WHERE q.`stem` LIKE '%HashMap 的默认初始容量%';
INSERT INTO `question_knowledge` (`question_id`, `category_id`)
SELECT q.id, 112 FROM `question` q WHERE q.`stem` LIKE '%转为红黑树%';
INSERT INTO `question_knowledge` (`question_id`, `category_id`)
SELECT q.id, 114 FROM `question` q WHERE q.`stem` LIKE '%JVM 运行时数据区%';
INSERT INTO `question_knowledge` (`question_id`, `category_id`)
SELECT q.id, 115 FROM `question` q WHERE q.`stem` LIKE '%Spring IOC 的核心思想%';
