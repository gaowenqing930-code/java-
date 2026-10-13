package com.xbmd.security;

import com.xbmd.common.BusinessException;
import com.xbmd.common.ResultCode;
import com.xbmd.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 登录拦截器：
 * 1. 解析 Authorization: Bearer <token>，失败返回 401；
 * 2. 将当前用户写入 UserContext；
 * 3. 检查 @RequireRole 注解实现角色鉴权，不匹配返回 403。
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;

    public JwtInterceptor(JwtUtil jwtUtil, TokenBlacklistService blacklistService) {
        this.jwtUtil = jwtUtil;
        this.blacklistService = blacklistService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 仅拦截 Controller 方法；放行 CORS 预检请求
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            // SSE(EventSource) 无法携带自定义 header，允许 token 走 query 参数
            token = request.getParameter("token");
        }
        if (token == null || token.isBlank()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或 Token 缺失");
        }

        Claims claims = jwtUtil.parseToken(token);
        // Redis 登录态治理：登出 token 或账号被禁用 → 立即拒绝
        if (blacklistService.isBlacklisted(claims.get("jti", String.class))) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        if (blacklistService.isUserDisabled(Long.valueOf(claims.getSubject()))) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账号已被禁用，请联系管理员");
        }
        UserContext.LoginUser loginUser = new UserContext.LoginUser(
                Long.valueOf(claims.getSubject()),
                claims.get("username", String.class),
                claims.get("role", String.class)
        );
        UserContext.set(loginUser);

        // 角色鉴权：方法级注解优先，其次类级注解
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole != null && !requireRole.value().equals(loginUser.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限访问该资源");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
