package com.smartticket.ticket.service;

import com.smartticket.ticket.dto.SubmitTicketRequest;
import com.smartticket.ticket.vo.SubmitResultVO;

/** 工单核心服务。状态机/查询在后续 Phase 扩展（同一实现集中状态变更，B1）。 */
public interface TicketService {

    /** 提交工单：落库(待派单)+工单号+首条时间线，同步 AI 分析（失败降级）。 */
    SubmitResultVO submit(SubmitTicketRequest req, Long creatorId);
}
