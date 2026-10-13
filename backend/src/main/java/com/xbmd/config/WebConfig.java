package com.xbmd.config;

import com.xbmd.module.admin.interceptor.AdminOperationInterceptor;
import com.xbmd.module.admin.service.OperationLogService;
import com.xbmd.security.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：JWT 拦截器注册 + 管理操作日志 + 全局跨域
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final OperationLogService operationLogService;

    public WebConfig(JwtInterceptor jwtInterceptor, OperationLogService operationLogService) {
        this.jwtInterceptor = jwtInterceptor;
        this.operationLogService = operationLogService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/register",
                        "/api/auth/login"
                );
        // 管理操作留痕
        registry.addInterceptor(new AdminOperationInterceptor(operationLogService))
                .addPathPatterns("/api/admin/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
