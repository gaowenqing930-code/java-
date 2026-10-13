package com.xbmd.module.admin.interceptor;

import com.xbmd.module.admin.service.OperationLogService;
import com.xbmd.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 后台管理操作日志拦截器：记录 /api/admin/** 请求（成功留痕、异常记 FAIL）
 */
@RequiredArgsConstructor
public class AdminOperationInterceptor implements HandlerInterceptor {

    private final OperationLogService operationLogService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (!(handler instanceof HandlerMethod hm)) {
            return;
        }
        String action = "ADMIN_" + hm.getMethod().getName().toUpperCase()
                + (ex != null ? "_FAIL" : "");
        String detail = hm.getMethod().getDeclaringClass().getSimpleName() + "."
                + hm.getMethod().getName() + " " + request.getMethod() + " " + request.getRequestURI();
        try {
            operationLogService.log(UserContext.getUserId(), action, detail);
        } catch (Exception e) {
            // 日志失败不影响业务
        }
    }
}
