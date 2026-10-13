# -*- coding: utf-8 -*-
"""并行评分优化：10 题评分改为 3 线程并发，总耗时减半以上"""
import io

p = r"D:\Codes\ai-interview-coach\backend\src\main\java\com\xbmd\module\interview\service\impl\InterviewServiceImpl.java"
s = io.open(p, encoding="utf-8").read()

# 1) import 并发包
if "java.util.concurrent." not in s:
    s = s.replace(
        "import java.time.LocalDateTime;",
        "import java.time.LocalDateTime;\nimport java.util.concurrent.ExecutorService;\nimport java.util.concurrent.Executors;\nimport java.util.concurrent.Future;",
    )

# 2) 评分段改为并发
old = """        // 10 题全部答完：统一逐题评分 + 总结评价
        for (InterviewQuestion q : listQuestions(sessionId)) {
            if (!Q_SCORED.equals(q.getStatus())) {
                scoreOne(q);
                questionMapper.updateById(q);
            }
        }
        List<InterviewQuestion> scored = listQuestions(sessionId);"""

new = """        // 10 题全部答完：统一逐题评分（并发加速）+ 总结评价
        List<InterviewQuestion> pending = listQuestions(sessionId);
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(3, pending.size()));
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (InterviewQuestion q : pending) {
                futures.add(pool.submit(() -> {
                    scoreOne(q);
                    questionMapper.updateById(q);
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                f.get();
            }
        } catch (Exception e) {
            log.warn("并发评分异常：{}", e.getMessage(), e);
        } finally {
            pool.shutdown();
        }
        List<InterviewQuestion> scored = listQuestions(sessionId);"""

assert old in s, "scoring block not found"
s = s.replace(old, new)
io.open(p, "w", encoding="utf-8", newline="").write(s)
print("parallel scoring patched")
