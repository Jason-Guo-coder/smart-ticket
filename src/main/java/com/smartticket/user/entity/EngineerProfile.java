package com.smartticket.user.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 工程师档案（ARCHITECTURE §4 engineer_profile）。 */
@Data
@TableName("engineer_profile")
public class EngineerProfile {

    @TableId
    private Long userId;

    /** 技能类别，逗号分隔枚举名，如 NETWORK,SOFTWARE。 */
    private String categorySkills;

    private Integer currentLoad;
}
