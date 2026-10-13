package com.xbmd.module.auth.service;

import com.xbmd.common.PageResult;
import com.xbmd.module.auth.dto.LoginRequest;
import com.xbmd.module.auth.dto.LoginResponse;
import com.xbmd.module.auth.dto.RegisterRequest;
import com.xbmd.module.auth.dto.UserVO;

/**
 * 用户认证服务
 */
public interface UserService {

    /** 注册：唯一性校验 + BCrypt 加密入库 */
    UserVO register(RegisterRequest request);

    /** 登录：校验凭据 + 签发 JWT */
    LoginResponse login(LoginRequest request);

    /** 获取当前用户信息 */
    UserVO getProfile(Long userId);

    /** 更新当前用户求职目标（专业 + 目标职业） */
    UserVO updateProfile(Long userId, String major, String targetJob);

    /** 管理员：用户分页列表（关键词模糊匹配用户名/邮箱） */
    PageResult<UserVO> pageUsers(String keyword, long pageNum, long pageSize);

    /** 管理员：启用/禁用用户 */
    void updateUserStatus(Long id, Integer status);
}
