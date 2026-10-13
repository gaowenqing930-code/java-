package com.xbmd.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 出题配置项（对应 application.yaml 中 ai.generate.*）
 */
@Data
@ConfigurationProperties(prefix = "ai.generate")
public class AiProperties {

    /** 解析/生成失败最大重试次数 */
    private int maxRetries = 3;

    /** 每用户每分钟调用上限 */
    private int rateLimitPerMinute = 5;

    /** 兜底抽题时最多抽取条数 */
    private int fallbackMaxCount = 10;

    /** Prompt 模板目录（classpath 下） */
    private String promptDir = "ai/prompts";
}
