package com.smartticket.auth.controller;

import com.smartticket.auth.dto.LoginRequest;
import com.smartticket.auth.service.AuthService;
import com.smartticket.auth.vo.LoginVO;
import com.smartticket.common.annotation.AuditLog;
import com.smartticket.common.context.UserContext;
import com.smartticket.common.result.Result;
import com.smartticket.user.entity.SysUser;
import com.smartticket.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @AuditLog("登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req));
    }

    @AuditLog("登出")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        String token = (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;
        authService.logout(token);
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        UserContext.CurrentUser cu = UserContext.get();
        Map<String, Object> data = new HashMap<>();
        if (cu != null) {
            SysUser user = userService.findByUsername(cu.getUsername());
            data.put("userId", cu.getUserId());
            data.put("username", cu.getUsername());
            data.put("role", cu.getRole());
            data.put("realName", user != null ? user.getRealName() : null);
            data.put("dept", user != null ? user.getDept() : null);
        }
        return Result.ok(data);
    }
}
