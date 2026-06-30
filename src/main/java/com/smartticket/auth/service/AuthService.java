package com.smartticket.auth.service;

import com.smartticket.auth.dto.LoginRequest;
import com.smartticket.auth.vo.LoginVO;

/** 认证服务：登录签发 JWT、登出黑名单。 */
public interface AuthService {

    LoginVO login(LoginRequest req);

    void logout(String token);
}
