package com.smartticket.user.enums;

/** 角色码（RBAC，PRD §1.4）。与 sys_role.code 对应。 */
public enum RoleCode {
    EMPLOYEE("报修人"),
    ENGINEER("工程师"),
    ADMIN("管理员");

    private final String label;

    RoleCode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
