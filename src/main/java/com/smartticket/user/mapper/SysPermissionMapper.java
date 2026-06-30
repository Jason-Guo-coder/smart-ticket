package com.smartticket.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限查询（RBAC）。按角色查权限码，供鉴权使用。
 */
@Mapper
public interface SysPermissionMapper {

    @Select("""
            SELECT p.perm_code FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            WHERE rp.role_id = #{roleId}
            """)
    List<String> selectPermCodesByRoleId(Long roleId);
}
