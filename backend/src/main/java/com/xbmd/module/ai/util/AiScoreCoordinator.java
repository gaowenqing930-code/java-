package com.xbmd.module.ai.util;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 异步评分协调器：记录每道题的评分 Future，供"交卷/结束"时等待未评完的题目补齐。
 * 答题卡用 key="practice:{answerId}"，AI 面试用 key="interview:{questionId}"，避免跨表 id 冲突。
 * 评分完成后通过 complete() 从 map 移除，防止内存膨胀。
 */
@Component
public class AiScoreCoordinator {

    private final ConcurrentHashMap<String, CompletableFuture<Void>> futures = new ConcurrentHashMap<>();

    /** 注册一个评分任务，返回其 Future（评分线程在 finally 里 complete 它） */
    public CompletableFuture<Void> register(String key) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        futures.put(key, future);
        return future;
    }

    /** 评分完成：标记 future 完成并从 map 移除 */
    public void complete(String key) {
        CompletableFuture<Void> future = futures.remove(key);
        if (future != null) {
            future.complete(null);
        }
    }

    /** 等待单个评分任务，超时静默返回（不抛异常，避免阻塞交卷/结束主流程） */
    public void await(String key, long timeoutMs) {
        CompletableFuture<Void> future = futures.get(key);
        if (future != null) {
            try {
                future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (Exception ignore) {
                // 超时/中断：不阻塞交卷，未评完的按现有状态处理
            }
        }
    }

    /** 批量等待多个评分任务补齐 */
    public void awaitAll(List<String> keys, long timeoutMs) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            await(key, timeoutMs);
        }
    }
}
