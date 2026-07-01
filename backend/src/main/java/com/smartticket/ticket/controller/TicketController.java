package com.smartticket.ticket.controller;

import com.smartticket.common.annotation.Idempotent;
import com.smartticket.common.context.UserContext;
import com.smartticket.common.exception.BizException;
import com.smartticket.common.ratelimit.RateLimiter;
import com.smartticket.common.result.PageResult;
import com.smartticket.common.result.Result;
import com.smartticket.common.result.ResultCode;
import com.smartticket.ticket.dto.SubmitTicketRequest;
import com.smartticket.ticket.dto.TicketQuery;
import com.smartticket.ticket.service.TicketService;
import com.smartticket.ticket.vo.SubmitResultVO;
import com.smartticket.ticket.vo.TicketDetailVO;
import com.smartticket.ticket.vo.TicketListItemVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final RateLimiter rateLimiter;
    private final int submitPerMinute;

    public TicketController(TicketService ticketService, RateLimiter rateLimiter,
                            @Value("${smart-ticket.ratelimit.submit-per-minute}") int submitPerMinute) {
        this.ticketService = ticketService;
        this.rateLimiter = rateLimiter;
        this.submitPerMinute = submitPerMinute;
    }

    /** 提交工单（报修人）。幂等 + 滑动窗口限流防刷（B9）。 */
    @PreAuthorize("hasAuthority('ticket:submit')")
    @Idempotent
    @PostMapping("/submit")
    public Result<SubmitResultVO> submit(@Valid @RequestBody SubmitTicketRequest req) {
        Long uid = UserContext.getUserId();
        if (!rateLimiter.tryAcquire("submit:" + uid, submitPerMinute, 60)) {
            throw new BizException(ResultCode.TOO_MANY_REQUESTS, "提交过于频繁，请稍后再试");
        }
        return Result.ok(ticketService.submit(req, uid));
    }

    /** 我的工单分页查询（报修人，仅本人）。 */
    @PreAuthorize("hasAuthority('ticket:my')")
    @GetMapping("/my")
    public Result<PageResult<TicketListItemVO>> myTickets(TicketQuery query) {
        return Result.ok(ticketService.pageMyTickets(query, UserContext.getUserId()));
    }

    /** 工单详情（时间线 + 解决方案 + SLA）。数据权限在服务层校验（B4）。 */
    @PreAuthorize("hasAuthority('ticket:detail')")
    @GetMapping("/{id}")
    public Result<TicketDetailVO> detail(@PathVariable Long id) {
        return Result.ok(ticketService.getDetail(id, UserContext.getUserId(), UserContext.getRole()));
    }
}
