package com.smartticket.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 角色（ARCHITECTURE §4 sys_role）。 */
@Data
@TableName("sys_role")
public class SysRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roleName;

    /** EMPLOYEE / ENGINEER / ADMIN，对应 RoleCode。 */
    private String code;
}
