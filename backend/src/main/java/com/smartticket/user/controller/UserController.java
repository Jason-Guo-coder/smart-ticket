package com.smartticket.user.controller;

import com.smartticket.common.result.Result;
import com.smartticket.user.service.UserService;
import com.smartticket.user.vo.UserVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理（管理员）。Phase 1 仅列表（验证 RBAC：非 ADMIN 越权返回 403）。
 * 增删改 / 角色分配在 Phase 9 扩展。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasAuthority('user:manage')")
    @GetMapping
    public Result<List<UserVO>> list() {
        return Result.ok(userService.listAll());
    }
}
