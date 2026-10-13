package com.xbmd.security;

import com.xbmd.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 基于 Redis 的登录态治理：
 * 1. jwt:bl:{jti}    —— 登出黑名单（token 级，TTL = token 剩余有效期）
 * 2. user:disabled:{userId} —— 用户禁用标记（人级，禁用即全端踢下线）
 */
@Service
public class TokenBlacklistService {

    private static final String BL_KEY = "jwt:bl:";
    private static final String DIS_KEY = "user:disabled:";
    /** 禁用标记 TTL：与 JWT 有效期一致，过期自动清理 */
    private static final Duration DISABLE_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final JwtUtil jwtUtil;

    public TokenBlacklistService(StringRedisTemplate redis, JwtUtil jwtUtil) {
        this.redis = redis;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 登出：把当前 token 的 jti 写入黑名单，TTL = token 剩余有效期。
     * token 过期后黑名单 key 自动被 Redis 清理，不堆积。
     */
    public void blacklistToken(String token) {
        Claims claims = jwtUtil.parseToken(token);
        String jti = claims.get("jti", String.class);
        if (jti == null || jti.isBlank()) {
            return;
        }
        long expireAt = claims.getExpiration().getTime();
        long remain = Math.max(0, expireAt - System.currentTimeMillis());
        redis.opsForValue().set(BL_KEY + jti, "1", Duration.ofMillis(remain));
    }

    /** 校验某 token 是否已登出 */
    public boolean isBlacklisted(String jti) {
        return jti != null && Boolean.TRUE.equals(redis.hasKey(BL_KEY + jti));
    }

    /** 禁用用户：写入标记，该用户所有已签发 token 立即失效 */
    public void disableUser(Long userId) {
        redis.opsForValue().set(DIS_KEY + userId, "1", DISABLE_TTL);
    }

    /** 恢复用户：删除禁用标记 */
    public void enableUser(Long userId) {
        redis.delete(DIS_KEY + userId);
    }

    /** 校验用户是否被禁用 */
    public boolean isUserDisabled(Long userId) {
        return Boolean.TRUE.equals(redis.hasKey(DIS_KEY + userId));
    }
}
