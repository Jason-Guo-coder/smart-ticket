package com.smartticket.user.vo;

import lombok.Data;

/** 用户出参（不含密码，ARCHITECTURE §5.2）。 */
@Data
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private String dept;
    private String role;     // 角色码
    private Integer status;
}
