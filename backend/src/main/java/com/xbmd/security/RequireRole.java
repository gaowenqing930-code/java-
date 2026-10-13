package com.xbmd.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口角色校验注解：标注在 Controller 类或方法上，要求当前用户角色与 value 完全一致
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /** 要求的角色，如 ADMIN */
    String value();
}
