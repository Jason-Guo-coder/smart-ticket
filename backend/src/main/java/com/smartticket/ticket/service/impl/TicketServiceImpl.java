package com.smartticket.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartticket.ai.AiAssistService;
import com.smartticket.ai.model.AiSuggestion;
import com.smartticket.ai.model.SimilarTicket;
import com.smartticket.common.exception.BizException;
import com.smartticket.common.result.PageResult;
import com.smartticket.common.result.ResultCode;
import com.smartticket.common.util.RedisUtil;
import com.smartticket.ticket.dto.RemarkRequest;
import com.smartticket.ticket.dto.SolutionSubmitRequest;
import com.smartticket.ticket.dto.SubmitTicketRequest;
import com.smartticket.ticket.dto.TicketQuery;
import com.smartticket.ticket.entity.Ticket;
import com.smartticket.ticket.entity.TicketLog;
import com.smartticket.ticket.entity.TicketSolution;
import com.smartticket.ticket.enums.Category;
import com.smartticket.ticket.enums.Priority;
import com.smartticket.ticket.enums.TicketStatus;
import com.smartticket.ticket.mapper.TicketLogMapper;
import com.smartticket.ticket.mapper.TicketMapper;
import com.smartticket.ticket.mapper.TicketSolutionMapper;
import com.smartticket.ticket.service.TicketService;
import com.smartticket.ticket.vo.SubmitResultVO;
import com.smartticket.ticket.vo.TicketDetailVO;
import com.smartticket.ticket.vo.TicketListItemVO;
import com.smartticket.ticket.vo.TodoVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TicketServiceImpl implements TicketService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TicketMapper ticketMapper;
    private final TicketLogMapper ticketLogMapper;
    private final TicketSolutionMapper ticketSolutionMapper;
    private final AiAssistService aiAssistService;
    private final RedisUtil redisUtil;
    private final int slaHours;

    public TicketServiceImpl(TicketMapper ticketMapper, TicketLogMapper ticketLogMapper,
                             TicketSolutionMapper ticketSolutionMapper,
                             AiAssistService aiAssistService, RedisUtil redisUtil,
                             @Value("${smart-ticket.sla.default-hours}") int slaHours) {
        this.ticketMapper = ticketMapper;
        this.ticketLogMapper = ticketLogMapper;
        this.ticketSolutionMapper = ticketSolutionMapper;
        this.aiAssistService = aiAssistService;
        this.redisUtil = redisUtil;
        this.slaHours = slaHours;
    }

    @Override
    @Transactional
    public SubmitResultVO submit(SubmitTicketRequest req, Long creatorId) {
        // 1) 落库（状态=待派单，生成工单号，SLA 截止）
        Ticket ticket = new Ticket();
        ticket.setTicketNo(genTicketNo());
        ticket.setTitle(req.getTitle());
        ticket.setContent(req.getContent());
        ticket.setImageUrl(req.getImageUrl());
        ticket.setStatus(TicketStatus.PENDING.name());
        ticket.setCreatorId(creatorId);
        ticket.setSlaDeadline(LocalDateTime.now().plusHours(slaHours));
        ticket.setSlaOverdue(0);
        ticketMapper.insert(ticket);

        // 2) 写首条时间线（B2：业务 + 日志同事务）
        writeLog(ticket.getId(), "提交", creatorId, "报修人提交工单");

        // 3) AI 分析（AiAssistService 永不抛异常，失败降级，B5）
        AiSuggestion ai = aiAssistService.analyze(req.getTitle(), req.getContent());

        // 4) 命中则回填类别/优先级
        if (!ai.isDegraded()) {
            ticket.setCategory(ai.getCategory());
            ticket.setPriority(ai.getPriority());
            ticketMapper.updateById(ticket);
        }

        SubmitResultVO vo = new SubmitResultVO();
        vo.setTicketId(ticket.getId());
        vo.setTicketNo(ticket.getTicketNo());
        vo.setAi(ai);
        return vo;
    }

    @Override
    public PageResult<TicketListItemVO> pageMyTickets(TicketQuery query, Long creatorId) {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10
                : Math.min(query.getPageSize(), 50); // 上限 50，防超大分页

        LambdaQueryWrapper<Ticket> w = new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getCreatorId, creatorId) // 仅本人（B4 数据域）
                .eq(StringUtils.hasText(query.getStatus()), Ticket::getStatus, query.getStatus())
                .and(StringUtils.hasText(query.getKeyword()), q -> q
                        .like(Ticket::getTitle, query.getKeyword())
                        .or().like(Ticket::getTicketNo, query.getKeyword()))
                .orderByDesc(Ticket::getCreateTime);

        IPage<Ticket> page = ticketMapper.selectPage(new Page<>(pageNum, pageSize), w);
        List<TicketListItemVO> list = page.getRecords().stream().map(this::toListItem).toList();
        return PageResult.of(page.getTotal(), list);
    }

    private TicketListItemVO toListItem(Ticket t) {
        TicketListItemVO vo = new TicketListItemVO();
        vo.setId(t.getId());
        vo.setTicketNo(t.getTicketNo());
        vo.setTitle(t.getTitle());
        vo.setCategory(t.getCategory());
        vo.setCategoryLabel(categoryLabel(t.getCategory()));
        vo.setPriority(t.getPriority());
        vo.setPriorityLabel(priorityLabel(t.getPriority()));
        vo.setStatus(t.getStatus());
        vo.setStatusLabel(statusLabel(t.getStatus()));
        vo.setCreateTime(t.getCreateTime());
        vo.setSlaDeadline(t.getSlaDeadline());
        vo.setSlaOverdue(t.getSlaOverdue());
        return vo;
    }

    @Override
    public TicketDetailVO getDetail(Long id, Long userId, String role) {
        TicketDetailVO vo = ticketMapper.selectDetail(id);
        if (vo == null) {
            throw new BizException(ResultCode.NOT_FOUND, "工单不存在");
        }
        // 数据权限（B4）：非 ADMIN 仅本人报修或本人受理可看
        boolean admin = "ADMIN".equals(role);
        boolean owner = userId != null
                && (userId.equals(vo.getCreatorId()) || userId.equals(vo.getAssigneeId()));
        if (!admin && !owner) {
            throw new BizException(ResultCode.FORBIDDEN, "无权查看该工单");
        }
        vo.setCategoryLabel(categoryLabel(vo.getCategory()));
        vo.setPriorityLabel(priorityLabel(vo.getPriority()));
        vo.setStatusLabel(statusLabel(vo.getStatus()));
        vo.setTimeline(ticketLogMapper.selectTimeline(id));
        vo.setSolutions(ticketSolutionMapper.selectByTicket(id));
        return vo;
    }

    @Override
    public TodoVO getTodo(Long engineerId) {
        TodoVO.Stats s = new TodoVO.Stats();
        s.setPending(ticketMapper.selectCount(
                new LambdaQueryWrapper<Ticket>().eq(Ticket::getStatus, TicketStatus.PENDING.name())));
        s.setProcessing(ticketMapper.selectCount(new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getAssigneeId, engineerId)
                .eq(Ticket::getStatus, TicketStatus.PROCESSING.name())));
        s.setDoneToday(ticketMapper.selectCount(new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getAssigneeId, engineerId)
                .in(Ticket::getStatus, TicketStatus.DONE.name(), TicketStatus.RATED.name())
                .ge(Ticket::getUpdateTime, LocalDate.now().atStartOfDay())));
        Double mins = ticketMapper.selectAvgHandleMinutes(engineerId);
        s.setAvgHours(mins == null ? 0d : Math.round(mins / 6.0) / 10.0); // 小时保留 1 位小数

        TodoVO vo = new TodoVO();
        vo.setStats(s);
        vo.setList(ticketMapper.selectTodoList(engineerId).stream().map(this::toListItem).toList());
        return vo;
    }

    @Override
    public List<SimilarTicket> handleReference(Long ticketId, Long userId, String role) {
        Ticket t = load(ticketId);
        // 访问控制：ADMIN / 受理人 / 待派单（抢单前可参考）
        boolean allowed = "ADMIN".equals(role)
                || (userId != null && userId.equals(t.getAssigneeId()))
                || TicketStatus.PENDING.name().equals(t.getStatus());
        if (!allowed) {
            throw new BizException(ResultCode.FORBIDDEN, "无权查看该工单参考");
        }
        return ticketMapper.selectSimilar(t.getCategory(), null, 5);
    }

    @Override
    @Transactional
    public void accept(Long ticketId, Long engineerId) {
        Ticket t = load(ticketId);
        requireStatus(t, TicketStatus.PENDING, "工单已被接单或状态已变化");
        // 待派单→处理中，占派为本人；乐观锁保证并发抢单只成功一次
        transition(t, TicketStatus.PROCESSING, "接单", engineerId, engineerId, true, "工程师接单，开始处理");
    }

    @Override
    @Transactional
    public void submitSolution(Long ticketId, Long userId, String role, SolutionSubmitRequest req) {
        Ticket t = load(ticketId);
        requireStatus(t, TicketStatus.PROCESSING, "工单不在处理中，无法提交");
        requireAssigneeOrAdmin(t, userId, role);
        // 先写解决方案，再流转；同事务，非法流转会一并回滚
        TicketSolution sol = new TicketSolution();
        sol.setTicketId(ticketId);
        sol.setEngineerId(userId);
        sol.setSolutionText(req.getSolutionText());
        sol.setImageUrl(req.getImageUrl());
        ticketSolutionMapper.insert(sol);
        transition(t, TicketStatus.ACCEPTING, "提交完成", userId, null, false, "工程师提交解决方案，待验收");
    }

    @Override
    @Transactional
    public void reassign(Long ticketId, Long userId, String role, RemarkRequest req) {
        Ticket t = load(ticketId);
        requireStatus(t, TicketStatus.PROCESSING, "工单不在处理中，无法转派");
        requireAssigneeOrAdmin(t, userId, role);
        String reason = req != null && StringUtils.hasText(req.getReason())
                ? req.getReason() : "工程师转派，退回待派单";
        // 处理中→待派单，清空受理人（touchAssignee=true, newAssignee=null）
        transition(t, TicketStatus.PENDING, "转派", userId, null, true, reason);
    }

    @Override
    @Transactional
    public void verifyPass(Long ticketId, Long userId, String role) {
        Ticket t = load(ticketId);
        requireStatus(t, TicketStatus.ACCEPTING, "工单不在待验收，无法验收");
        requireCreatorOrAdmin(t, userId, role);
        transition(t, TicketStatus.DONE, "验收通过", userId, null, false, "报修人验收通过");
    }

    @Override
    @Transactional
    public void verifyReject(Long ticketId, Long userId, String role, RemarkRequest req) {
        Ticket t = load(ticketId);
        requireStatus(t, TicketStatus.ACCEPTING, "工单不在待验收，无法驳回");
        requireCreatorOrAdmin(t, userId, role);
        String reason = req != null && StringUtils.hasText(req.getReason())
                ? req.getReason() : "报修人验收驳回，退回处理中";
        transition(t, TicketStatus.PROCESSING, "验收驳回", userId, null, false, reason);
    }

    @Override
    public List<TicketListItemVO> listPending() {
        List<Ticket> pending = ticketMapper.selectList(new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getStatus, TicketStatus.PENDING.name())
                .last("ORDER BY FIELD(priority, 'LOW', 'MID', 'HIGH') DESC, create_time ASC"));
        return pending.stream().map(this::toListItem).toList();
    }

    @Override
    public String categoryOf(Long ticketId) {
        return load(ticketId).getCategory();
    }

    @Override
    @Transactional
    public void assignByDispatch(Long ticketId, Long engineerId, Long operatorId, String remark) {
        Ticket t = load(ticketId);
        requireStatus(t, TicketStatus.PENDING, "工单不在待派单，无法派单");
        // 待派单→处理中，占派为指定工程师；乐观锁保证并发只成功一次（B1/B2/B3）
        transition(t, TicketStatus.PROCESSING, "派单", operatorId, engineerId, true, remark);
    }

    /**
     * 状态机核心（B1）：校验合法流转 → 乐观锁更新状态(+受理人) → 同事务写日志（B2）。
     * 非法流转与并发冲突均抛 CONFLICT。调用方须处于 @Transactional 中。
     */
    private void transition(Ticket t, TicketStatus target, String action, Long operatorId,
                            Long newAssignee, boolean touchAssignee, String remark) {
        TicketStatus cur = TicketStatus.valueOf(t.getStatus());
        if (!cur.canTransferTo(target)) {
            throw new BizException(ResultCode.CONFLICT,
                    "非法状态流转：" + cur.getLabel() + " → " + target.getLabel());
        }
        LambdaUpdateWrapper<Ticket> uw = new LambdaUpdateWrapper<Ticket>()
                .eq(Ticket::getId, t.getId())
                .eq(Ticket::getVersion, t.getVersion()) // 乐观锁：并发只成功一次
                .set(Ticket::getStatus, target.name())
                .setSql("version = version + 1");
        if (touchAssignee) {
            uw.set(Ticket::getAssigneeId, newAssignee);
        }
        int rows = ticketMapper.update(null, uw);
        if (rows == 0) {
            throw new BizException(ResultCode.CONFLICT, "工单状态已变化，请刷新后重试");
        }
        writeLog(t.getId(), action, operatorId, remark);
    }

    private Ticket load(Long id) {
        Ticket t = ticketMapper.selectById(id);
        if (t == null) {
            throw new BizException(ResultCode.NOT_FOUND, "工单不存在");
        }
        return t;
    }

    /**
     * 显式校验动作的源状态（B1）。
     * 因 canTransferTo 仅校验目标可达，而“处理中”可由“待派单/待验收”两源到达，
     * 故每个动作须锁定唯一合法源状态，防止“接单/验收驳回”越权命中同一目标造成劫持。
     */
    private void requireStatus(Ticket t, TicketStatus expected, String msg) {
        if (!expected.name().equals(t.getStatus())) {
            throw new BizException(ResultCode.CONFLICT, msg);
        }
    }

    private void requireAssigneeOrAdmin(Ticket t, Long userId, String role) {
        if ("ADMIN".equals(role)) return;
        if (userId == null || !userId.equals(t.getAssigneeId())) {
            throw new BizException(ResultCode.FORBIDDEN, "只能操作本人受理的工单");
        }
    }

    private void requireCreatorOrAdmin(Ticket t, Long userId, String role) {
        if ("ADMIN".equals(role)) return;
        if (userId == null || !userId.equals(t.getCreatorId())) {
            throw new BizException(ResultCode.FORBIDDEN, "只能验收本人提交的工单");
        }
    }

    private static String statusLabel(String code) {
        try {
            return code == null ? null : TicketStatus.valueOf(code).getLabel();
        } catch (IllegalArgumentException e) {
            return code;
        }
    }

    private static String categoryLabel(String code) {
        try {
            return code == null ? null : Category.valueOf(code).getLabel();
        } catch (IllegalArgumentException e) {
            return code;
        }
    }

    private static String priorityLabel(String code) {
        try {
            return code == null ? null : Priority.valueOf(code).getLabel();
        } catch (IllegalArgumentException e) {
            return code;
        }
    }

    private void writeLog(Long ticketId, String action, Long operatorId, String remark) {
        TicketLog log = new TicketLog();
        log.setTicketId(ticketId);
        log.setAction(action);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        ticketLogMapper.insert(log);
    }

    /** 工单号：TK + yyyyMMdd + 4 位日内自增（Redis 原子自增）。 */
    private String genTicketNo() {
        String day = LocalDate.now().format(DAY);
        String key = "ticket:no:" + day;
        Long seq = redisUtil.increment(key, 1);
        redisUtil.expire(key, 2, TimeUnit.DAYS);
        return "TK" + day + String.format("%04d", seq == null ? 1 : seq);
    }
}
