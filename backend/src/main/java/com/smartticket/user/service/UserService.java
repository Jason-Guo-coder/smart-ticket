package com.smartticket.user.service;

import com.smartticket.user.entity.SysUser;
import com.smartticket.user.vo.EngineerVO;
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

    /** 按类别技能推荐负载最低的工程师（AI 建议 / 自动派单复用；无匹配返回 null）。 */
    EngineerVO recommendEngineer(String category);

    /** 全部工程师负载（派单页负载面板）。 */
    List<EngineerVO> engineerLoads();

    /** 调整工程师当前负载（派单 +1，完成/退回 -1；结果不小于 0）。 */
    void changeEngineerLoad(Long engineerId, int delta);

    /** 校验目标是否为有效工程师（派单目标合法性）。 */
    boolean isEngineer(Long userId);
}
