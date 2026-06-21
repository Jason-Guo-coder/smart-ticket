package com.smartticket.common.audit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 审计日志 Mapper（写入 + Phase 9 查询）。 */
@Mapper
public interface AuditLogMapper extends BaseMapper<SysAuditLog> {
}
