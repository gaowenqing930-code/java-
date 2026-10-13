package com.xbmd.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置项（对应 application.properties 中 jwt.*）
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** 签名密钥（HS256 要求长度 ≥ 32 字节） */
    private String secret;

    /** Token 有效期（小时） */
    private long expireHours = 24;
}
