package com.smartticket.user.vo;

import lombok.Data;

/** 工程师负载出参（AI 建议 / 派单 / 工程师管理复用）。 */
@Data
public class EngineerVO {
    private Long userId;
    private String realName;
    private String dept;
    private Integer currentLoad;
    private String categorySkills;
}
