package com.smartticket.ticket.controller;

import com.smartticket.common.annotation.Idempotent;
import com.smartticket.common.context.UserContext;
import com.smartticket.common.exception.BizException;
import com.smartticket.common.ratelimit.RateLimiter;
import com.smartticket.common.result.PageResult;
import com.smartticket.common.result.Result;
import com.smartticket.common.result.ResultCode;
import com.smartticket.ai.model.SimilarTicket;
import com.smartticket.ticket.dto.RemarkRequest;
import com.smartticket.ticket.dto.SolutionSubmitRequest;
import com.smartticket.ticket.dto.SubmitTicketRequest;
import com.smartticket.ticket.dto.TicketQuery;
import com.smartticket.ticket.service.TicketService;
import com.smartticket.ticket.vo.SubmitResultVO;
import com.smartticket.ticket.vo.TicketDetailVO;
import com.smartticket.ticket.vo.TicketListItemVO;
import com.smartticket.ticket.vo.TodoVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    // ============ 工程师处理（状态机，ticket:handle） ============

    /** 工程师待办页数据（统计条 + 待办卡片）。 */
    @PreAuthorize("hasAuthority('ticket:todo')")
    @GetMapping("/todo")
    public Result<TodoVO> todo() {
        return Result.ok(ticketService.getTodo(UserContext.getUserId()));
    }

    /** AI 参考：处理页展示的同类别历史已解决工单。 */
    @PreAuthorize("hasAuthority('ticket:handle')")
    @GetMapping("/{id}/handle/similar")
    public Result<List<SimilarTicket>> handleSimilar(@PathVariable Long id) {
        return Result.ok(ticketService.handleReference(id));
    }

    /** 接单：待派单→处理中（并发抢单只成功一次）。 */
    @PreAuthorize("hasAuthority('ticket:handle')")
    @PostMapping("/{id}/handle/accept")
    public Result<Void> accept(@PathVariable Long id) {
        ticketService.accept(id, UserContext.getUserId());
        return Result.ok();
    }

    /** 提交完成：处理中→待验收，写解决方案（仅受理人/ADMIN）。 */
    @PreAuthorize("hasAuthority('ticket:handle')")
    @PostMapping("/{id}/handle/submit")
    public Result<Void> submitSolution(@PathVariable Long id, @Valid @RequestBody SolutionSubmitRequest req) {
        ticketService.submitSolution(id, UserContext.getUserId(), UserContext.getRole(), req);
        return Result.ok();
    }

    /** 转派：处理中→待派单，清空受理人（仅受理人/ADMIN）。 */
    @PreAuthorize("hasAuthority('ticket:handle')")
    @PostMapping("/{id}/handle/reassign")
    public Result<Void> reassign(@PathVariable Long id, @RequestBody(required = false) RemarkRequest req) {
        ticketService.reassign(id, UserContext.getUserId(), UserContext.getRole(), req);
        return Result.ok();
    }

    // ============ 报修人验收（状态机，ticket:my + 本人/ADMIN 校验） ============

    /** 验收通过：待验收→已完成（仅报修人本人/ADMIN）。 */
    @PreAuthorize("hasAuthority('ticket:my')")
    @PostMapping("/{id}/verify/pass")
    public Result<Void> verifyPass(@PathVariable Long id) {
        ticketService.verifyPass(id, UserContext.getUserId(), UserContext.getRole());
        return Result.ok();
    }

    /** 验收驳回：待验收→处理中（仅报修人本人/ADMIN）。 */
    @PreAuthorize("hasAuthority('ticket:my')")
    @PostMapping("/{id}/verify/reject")
    public Result<Void> verifyReject(@PathVariable Long id, @RequestBody(required = false) RemarkRequest req) {
        ticketService.verifyReject(id, UserContext.getUserId(), UserContext.getRole(), req);
        return Result.ok();
    }
}
