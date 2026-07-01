package com.smartticket.ticket.service;

import com.smartticket.common.result.PageResult;
import com.smartticket.ticket.dto.SubmitTicketRequest;
import com.smartticket.ticket.dto.TicketQuery;
import com.smartticket.ticket.vo.SubmitResultVO;
import com.smartticket.ticket.vo.TicketDetailVO;
import com.smartticket.ticket.vo.TicketListItemVO;

/** 工单核心服务。状态机/查询在后续 Phase 扩展（同一实现集中状态变更，B1）。 */
public interface TicketService {

    /** 提交工单：落库(待派单)+工单号+首条时间线，同步 AI 分析（失败降级）。 */
    SubmitResultVO submit(SubmitTicketRequest req, Long creatorId);

    /** 我的工单分页查询（仅本人 creatorId，支持状态/关键词过滤）。 */
    PageResult<TicketListItemVO> pageMyTickets(TicketQuery query, Long creatorId);

    /**
     * 工单详情（表头 + 时间线 + 解决方案 + SLA）。
     * 数据权限（B4）：ADMIN 可看全部；否则仅报修人本人或受理工程师可看，其余 403。
     */
    TicketDetailVO getDetail(Long id, Long userId, String role);
}
