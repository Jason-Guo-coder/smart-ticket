package com.smartticket.user.service;

import com.smartticket.user.entity.SysUser;
import com.smartticket.user.vo.UserVO;

import java.util.List;

/** 用户/角色/权限服务（RBAC）。 */
public interface UserService {

    /** 按用户名查用户（含密文，仅供认证用）。 */
    SysUser findByUsername(String username);

    /** 角色码（EMPLOYEE/ENGINEER/ADMIN）。 */
    String getRoleCode(Long roleId);

    /** 角色拥有的权限码集合（带内存缓存）。 */
    List<String> getPermCodes(Long roleId);

    /** 用户列表（管理员，Phase 9 扩展筛选/分页）。 */
    List<UserVO> listAll();
}
