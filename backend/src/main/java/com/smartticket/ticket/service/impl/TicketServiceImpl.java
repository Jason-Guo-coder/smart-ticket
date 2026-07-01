package com.smartticket.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartticket.ai.AiAssistService;
import com.smartticket.ai.model.AiSuggestion;
import com.smartticket.common.exception.BizException;
import com.smartticket.common.result.PageResult;
import com.smartticket.common.result.ResultCode;
import com.smartticket.common.util.RedisUtil;
import com.smartticket.ticket.dto.SubmitTicketRequest;
import com.smartticket.ticket.dto.TicketQuery;
import com.smartticket.ticket.entity.Ticket;
import com.smartticket.ticket.entity.TicketLog;
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
