package com.smartticket.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartticket.user.entity.SysRole;
import com.smartticket.user.entity.SysUser;
import com.smartticket.user.mapper.SysPermissionMapper;
import com.smartticket.user.mapper.SysRoleMapper;
import com.smartticket.user.mapper.SysUserMapper;
import com.smartticket.user.service.UserService;
import com.smartticket.user.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;

    /** 角色 -> 权限码缓存（数据驱动 RBAC，避免每请求查库；权限变更时清理）。 */
    private final Map<Long, List<String>> permCache = new ConcurrentHashMap<>();
    private final Map<Long, String> roleCodeCache = new ConcurrentHashMap<>();

    public UserServiceImpl(SysUserMapper userMapper, SysRoleMapper roleMapper,
                           SysPermissionMapper permissionMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public SysUser findByUsername(String username) {
        return userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
    }

    @Override
    public String getRoleCode(Long roleId) {
        return roleCodeCache.computeIfAbsent(roleId, id -> {
            SysRole role = roleMapper.selectById(id);
            return role == null ? null : role.getCode();
        });
    }

    @Override
    public List<String> getPermCodes(Long roleId) {
        return permCache.computeIfAbsent(roleId, permissionMapper::selectPermCodesByRoleId);
    }

    @Override
    public List<UserVO> listAll() {
        return userMapper.selectList(null).stream().map(u -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(u, vo);
            vo.setRole(getRoleCode(u.getRoleId()));
            return vo;
        }).toList();
    }
}
