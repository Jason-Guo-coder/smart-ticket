package com.smartticket.dispatch.service;

import com.smartticket.dispatch.vo.AutoAllResultVO;
import com.smartticket.dispatch.vo.DispatchBoardVO;

/**
 * 派单服务。状态变更委托 TicketService 中央状态机（B1/B2），
 * 工程师推荐/负载委托 UserService（B11 不跨模块调 mapper）；
 * 分布式锁 + 乐观锁保证并发派单只成功一次（B3）。
 */
public interface DispatchService {

    /** 派单页数据：待派单卡片（含 AI 建议）+ 工程师负载。 */
    DispatchBoardVO board();

    /** 手动指派 / 采纳 AI 建议：指派给指定工程师。 */
    void assign(Long ticketId, Long engineerId, Long operatorId);

    /** 自动分配：按类别 + 负载推荐工程师并指派，返回被指派工程师姓名。 */
    String autoAssign(Long ticketId, Long operatorId);

    /** 一键智能分派全部待派单。 */
    AutoAllResultVO autoAssignAll(Long operatorId);
}
