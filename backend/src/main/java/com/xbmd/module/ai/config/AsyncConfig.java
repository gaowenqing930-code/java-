package com.xbmd.module.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 全局异步配置：AI 评分专用线程池。
 * 答题卡主观题评测、AI 面试单题评分都投递到该池后台执行，
 * 不占用 Tomcat Web 请求线程，避免长耗时 AI 调用阻塞前端请求。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("aiScoreExecutor")
    public Executor aiScoreExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("ai-score-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
