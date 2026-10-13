package com.xbmd.module.ai.util;

import com.xbmd.module.ai.config.AiProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 每用户滑动窗口限流器：限制每用户每分钟调用次数（内存实现，单实例够用）
 */
@Component
public class PerUserRateLimiter {

    private final Map<Long, Deque<Long>> windows = new ConcurrentHashMap<>();
    private final int maxPerMinute;

    public PerUserRateLimiter(AiProperties properties) {
        this.maxPerMinute = properties.getRateLimitPerMinute();
    }

    /**
     * @return true 表示放行，false 表示已超限
     */
    public synchronized boolean tryAcquire(Long userId) {
        long now = System.currentTimeMillis();
        Deque<Long> deque = windows.computeIfAbsent(userId, k -> new ArrayDeque<>());
        while (!deque.isEmpty() && now - deque.peekFirst() >= 60_000L) {
            deque.pollFirst();
        }
        if (deque.size() >= maxPerMinute) {
            return false;
        }
        deque.addLast(now);
        return true;
    }
}
