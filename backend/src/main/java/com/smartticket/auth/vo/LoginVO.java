package com.smartticket.auth.vo;

import lombok.Data;

/** 登录出参。 */
@Data
public class LoginVO {
    private String token;
    private String username;
    private String realName;
    private String role; // EMPLOYEE/ENGINEER/ADMIN
}
