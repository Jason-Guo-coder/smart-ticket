package com.smartticket.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartticket.user.entity.SysUser;
import com.smartticket.user.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 演示账号初始化：启动时用真实 BCrypt 写入三类角色账号（幂等，已存在则跳过）。
 * 角色 ID 与 schema 种子一致：EMPLOYEE=1 / ENGINEER=2 / ADMIN=3。密码统一 123456。
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seed("liming", "李明", "研发部", 1L);
        seed("zhangwei", "张伟", "网络组", 2L);
        seed("wangfang", "王芳", "IT 运维", 3L);
    }

    private void seed(String username, String realName, String dept, Long roleId) {
        Long exists = userMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (exists != null && exists > 0) {
            return;
        }
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("123456"));
        u.setRealName(realName);
        u.setDept(dept);
        u.setRoleId(roleId);
        u.setStatus(1);
        userMapper.insert(u);
        log.info("初始化演示账号: {} / 123456", username);
    }
}
