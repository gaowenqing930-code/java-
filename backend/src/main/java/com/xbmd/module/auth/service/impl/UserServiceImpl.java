package com.xbmd.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xbmd.common.BusinessException;
import com.xbmd.common.PageResult;
import com.xbmd.common.ResultCode;
import com.xbmd.module.auth.dto.LoginRequest;
import com.xbmd.module.auth.dto.LoginResponse;
import com.xbmd.module.auth.dto.RegisterRequest;
import com.xbmd.module.auth.dto.UserVO;
import com.xbmd.module.auth.entity.User;
import com.xbmd.module.auth.mapper.UserMapper;
import com.xbmd.module.auth.service.UserService;
import com.xbmd.security.TokenBlacklistService;
import com.xbmd.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户认证服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final Integer STATUS_NORMAL = 1;

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public UserVO register(RegisterRequest request) {
        // 用户名唯一性校验
        Long usernameCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (usernameCount != null && usernameCount > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已存在");
        }
        // 邮箱唯一性校验
        Long emailCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
        if (emailCount != null && emailCount > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(ROLE_USER);
        user.setStatus(STATUS_NORMAL);
        userMapper.insert(user);
        return toVO(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        // 用户不存在与密码错误返回同一提示，避免用户名枚举
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() == null || !STATUS_NORMAL.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, toVO(user));
    }

    @Override
    public UserVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return toVO(user);
    }

    @Override
    public UserVO updateProfile(Long userId, String major, String targetJob) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        user.setMajor(StringUtils.hasText(major) ? major.trim() : null);
        user.setTargetJob(StringUtils.hasText(targetJob) ? targetJob.trim() : null);
        userMapper.updateById(user);
        return toVO(user);
    }

    @Override
    public PageResult<UserVO> pageUsers(String keyword, long pageNum, long pageSize) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(User::getUsername, keyword.trim())
                    .or().like(User::getEmail, keyword.trim()));
        }
        qw.orderByDesc(User::getCreateTime);
        Page<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        return PageResult.of(page, this::toVO);
    }

    @Override
    public void updateUserStatus(Long id, Integer status) {
        if (status == null || (status != 1 && status != 0)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态仅支持 1(正常)/0(禁用)");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (ROLE_ADMIN.equals(user.getRole())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不允许禁用管理员账号");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        // Redis 登录态治理：禁用 → 立即踢下线所有端；恢复 → 解除禁用
        if (status == 0) {
            blacklistService.disableUser(id);
        } else {
            blacklistService.enableUser(id);
        }
    }

    private UserVO toVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .major(user.getMajor())
                .targetJob(user.getTargetJob())
                .build();
    }
}
