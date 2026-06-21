package com.smartticket.common.audit;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 全局操作审计日志（ARCHITECTURE §4 sys_audit_log）。 */
@Data
@TableName("sys_audit_log")
public class SysAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long operatorId;
    private String role;
    private String actionType;
    private String target;
    private String ip;
    private String result; // SUCCESS / FAIL

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
