package com.smartticket.dispatch.service.impl;

import com.smartticket.common.exception.BizException;
import com.smartticket.common.result.ResultCode;
import com.smartticket.common.util.RedisUtil;
import com.smartticket.dispatch.service.DispatchService;
import com.smartticket.dispatch.vo.AutoAllResultVO;
import com.smartticket.dispatch.vo.DispatchBoardVO;
import com.smartticket.dispatch.vo.PendingTicketVO;
import com.smartticket.ticket.service.TicketService;
import com.smartticket.ticket.vo.TicketListItemVO;
import com.smartticket.user.service.UserService;
import com.smartticket.user.vo.EngineerVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class DispatchServiceImpl implements DispatchService {

    private static final long LOCK_SECONDS = 10;

    private final TicketService ticketService;
    private final UserService userService;
    private final RedisUtil redisUtil;

    public DispatchServiceImpl(TicketService ticketService, UserService userService, RedisUtil redisUtil) {
        this.ticketService = ticketService;
        this.userService = userService;
        this.redisUtil = redisUtil;
    }

    @Override
    public DispatchBoardVO board() {
        List<PendingTicketVO> pending = ticketService.listPending().stream().map(it -> {
            PendingTicketVO vo = new PendingTicketVO();
            BeanUtils.copyProperties(it, vo);
            EngineerVO sug = userService.recommendEngineer(it.getCategory());
            if (sug != null) {
                vo.setSuggestedEngineerId(sug.getUserId());
                vo.setSuggestedEngineerName(sug.getRealName());
            }
            return vo;
        }).toList();
        return new DispatchBoardVO(pending, userService.engineerLoads());
    }

    @Override
    public void assign(Long ticketId, Long engineerId, Long operatorId) {
        if (!userService.isEngineer(engineerId)) {
            throw new BizException(ResultCode.BAD_REQUEST, "目标不是有效工程师");
        }
        doAssign(ticketId, engineerId, operatorId, "管理员手动指派");
    }

    @Override
    public String autoAssign(Long ticketId, Long operatorId) {
        EngineerVO eng = userService.recommendEngineer(ticketService.categoryOf(ticketId));
        if (eng == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "暂无可用工程师");
        }
        doAssign(ticketId, eng.getUserId(), operatorId, "系统自动分配给 " + eng.getRealName());
        return eng.getRealName();
    }

    @Override
    public AutoAllResultVO autoAssignAll(Long operatorId) {
        List<TicketListItemVO> pending = ticketService.listPending();
        int success = 0, failed = 0;
        for (TicketListItemVO t : pending) {
            try {
                EngineerVO eng = userService.recommendEngineer(t.getCategory());
                if (eng == null) {
                    failed++;
                    continue;
                }
                doAssign(t.getId(), eng.getUserId(), operatorId, "一键智能分派给 " + eng.getRealName());
                success++;
            } catch (Exception e) {
                failed++; // 单条失败（并发被抢/状态已变）不影响其余
            }
        }
        return new AutoAllResultVO(pending.size(), success, failed);
    }

    /**
     * 派单核心：分布式锁内调用中央状态机指派（B3）。
     * 锁贯穿 assignByDispatch 的提交（该调用为独立事务，返回即已提交），
     * 再更新工程师负载，最后释放锁——并发派单同一工单只成功一次。
     */
    private void doAssign(Long ticketId, Long engineerId, Long operatorId, String remark) {
        String lockKey = "dispatch:lock:" + ticketId;
        String token = UUID.randomUUID().toString();
        if (!redisUtil.tryLock(lockKey, token, LOCK_SECONDS, TimeUnit.SECONDS)) {
            throw new BizException(ResultCode.CONFLICT, "该工单正在派单中，请稍后重试");
        }
        try {
            ticketService.assignByDispatch(ticketId, engineerId, operatorId, remark);
            userService.changeEngineerLoad(engineerId, 1);
        } finally {
            redisUtil.unlock(lockKey, token);
        }
    }
}
