package com.smartticket.ticket.service;

import com.smartticket.ai.model.SimilarTicket;
import com.smartticket.common.result.PageResult;
import com.smartticket.ticket.dto.RemarkRequest;
import com.smartticket.ticket.dto.SolutionSubmitRequest;
import com.smartticket.ticket.dto.SubmitTicketRequest;
import com.smartticket.ticket.dto.TicketQuery;
import com.smartticket.ticket.vo.SubmitResultVO;
import com.smartticket.ticket.vo.TicketDetailVO;
import com.smartticket.ticket.vo.TicketListItemVO;
import com.smartticket.ticket.vo.TodoVO;

import java.util.List;

/**
 * 工单核心服务。所有状态变更集中在此实现的状态机方法（B1），每次流转同事务写日志（B2）。
 */
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

    /** 工程师待办页数据（统计条 + 待办卡片）。 */
    TodoVO getTodo(Long engineerId);

    /** AI 参考：与本工单同类别的历史已解决工单（处理页参考）。 */
    List<SimilarTicket> handleReference(Long ticketId);

    /** 接单：待派单→处理中，占派为本人（乐观锁防并发抢单）。 */
    void accept(Long ticketId, Long engineerId);

    /** 提交完成：处理中→待验收，写解决方案（仅受理人/ADMIN）。 */
    void submitSolution(Long ticketId, Long userId, String role, SolutionSubmitRequest req);

    /** 转派：处理中→待派单，清空受理人（仅受理人/ADMIN）。 */
    void reassign(Long ticketId, Long userId, String role, RemarkRequest req);

    /** 验收通过：待验收→已完成（仅报修人本人/ADMIN）。 */
    void verifyPass(Long ticketId, Long userId, String role);

    /** 验收驳回：待验收→处理中，退回原受理人（仅报修人本人/ADMIN）。 */
    void verifyReject(Long ticketId, Long userId, String role, RemarkRequest req);
}
