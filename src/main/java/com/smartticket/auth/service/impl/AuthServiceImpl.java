package com.smartticket.auth.service.impl;

import com.smartticket.auth.dto.LoginRequest;
import com.smartticket.auth.service.AuthService;
import com.smartticket.auth.vo.LoginVO;
import com.smartticket.common.exception.BizException;
import com.smartticket.common.result.ResultCode;
import com.smartticket.common.security.JwtAuthFilter;
import com.smartticket.common.security.JwtUtil;
import com.smartticket.common.util.RedisUtil;
import com.smartticket.user.entity.SysUser;
import com.smartticket.user.service.UserService;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    public AuthServiceImpl(UserService userService, PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil, RedisUtil redisUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisUtil = redisUtil;
    }

    @Override
    public LoginVO login(LoginRequest req) {
        SysUser user = userService.findByUsername(req.getUsername());
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BizException(ResultCode.BAD_REQUEST, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已停用");
        }
        String role = userService.getRoleCode(user.getRoleId());
        String token = jwtUtil.generate(user.getId(), user.getUsername(), role);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(role);
        return vo;
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        // 黑名单 TTL = token 剩余有效期
        try {
            Claims claims = jwtUtil.parse(token);
            long ttl = claims.getExpiration().getTime() - new Date().getTime();
            if (ttl > 0) {
                redisUtil.set(JwtAuthFilter.BLACKLIST_PREFIX + token, "1", ttl, TimeUnit.MILLISECONDS);
            }
        } catch (Exception ignored) {
            // 已过期/非法 token 无需拉黑
        }
    }
}
