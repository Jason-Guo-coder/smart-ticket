package com.smartticket.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartticket.ticket.entity.Ticket;
import com.smartticket.ticket.entity.TicketSolution;
import com.smartticket.ticket.mapper.TicketMapper;
import com.smartticket.ticket.mapper.TicketSolutionMapper;
import com.smartticket.user.entity.EngineerProfile;
import com.smartticket.user.entity.SysUser;
import com.smartticket.user.mapper.EngineerProfileMapper;
import com.smartticket.user.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 演示数据初始化（幂等）：三类账号 + 工程师档案 + 少量历史已完成工单。
 * 角色 ID 与 schema 种子一致：EMPLOYEE=1 / ENGINEER=2 / ADMIN=3；密码统一 123456。
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final EngineerProfileMapper engineerMapper;
    private final TicketMapper ticketMapper;
    private final TicketSolutionMapper solutionMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(SysUserMapper userMapper, EngineerProfileMapper engineerMapper,
                           TicketMapper ticketMapper, TicketSolutionMapper solutionMapper,
                           PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.engineerMapper = engineerMapper;
        this.ticketMapper = ticketMapper;
        this.solutionMapper = solutionMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 基础三角色
        seedUser("liming", "李明", "研发部", 1L);
        Long zhangwei = seedUser("zhangwei", "张伟", "网络组", 2L);
        Long admin = seedUser("wangfang", "王芳", "IT 运维", 3L);
        // 更多工程师（供 AI 建议 / 派单 / 看板）
        Long liuyang = seedUser("liuyang", "刘洋", "硬件组", 2L);
        Long chenjing = seedUser("chenjing", "陈静", "系统组", 2L);
        Long zhaomin = seedUser("zhaomin", "赵敏", "网络组", 2L);

        seedProfile(zhangwei, "NETWORK", 3);
        seedProfile(liuyang, "HARDWARE", 5);
        seedProfile(chenjing, "ACCOUNT,SOFTWARE", 7);
        seedProfile(zhaomin, "NETWORK", 9);

        // 历史已完成工单（让 AI 相似检索 / 看板统计有数据）
        Long creator = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, "liming")).getId();
        seedHistory("TK20240601", "办公室 WiFi 频繁掉线", "NETWORK", "MID", "DONE",
                creator, zhangwei, "更换 AP 信道，重启核心交换机后恢复。");
        seedHistory("TK20240602", "笔记本无法连接公司无线", "NETWORK", "HIGH", "RATED",
                creator, zhangwei, "重装无线网卡驱动并重新加入公司域。");
        seedHistory("TK20240603", "VPN 在家无法连接内网", "NETWORK", "HIGH", "DONE",
                creator, zhangwei, "重置 VPN 证书并放通对应端口。");
    }

    private Long seedUser(String username, String realName, String dept, Long roleId) {
        SysUser existing = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (existing != null) {
            return existing.getId();
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
        return u.getId();
    }

    private void seedProfile(Long userId, String skills, int load) {
        if (userId == null || engineerMapper.selectById(userId) != null) {
            return;
        }
        EngineerProfile p = new EngineerProfile();
        p.setUserId(userId);
        p.setCategorySkills(skills);
        p.setCurrentLoad(load);
        engineerMapper.insert(p);
    }

    private void seedHistory(String no, String title, String category, String priority,
                             String status, Long creatorId, Long assigneeId, String solution) {
        Long exists = ticketMapper.selectCount(
                Wrappers.<Ticket>lambdaQuery().eq(Ticket::getTicketNo, no));
        if (exists != null && exists > 0) {
            return;
        }
        Ticket t = new Ticket();
        t.setTicketNo(no);
        t.setTitle(title);
        t.setContent(title);
        t.setCategory(category);
        t.setPriority(priority);
        t.setStatus(status);
        t.setCreatorId(creatorId);
        t.setAssigneeId(assigneeId);
        t.setVersion(0);
        t.setSlaOverdue(0);
        ticketMapper.insert(t);

        TicketSolution s = new TicketSolution();
        s.setTicketId(t.getId());
        s.setEngineerId(assigneeId);
        s.setSolutionText(solution);
        solutionMapper.insert(s);
    }
}
