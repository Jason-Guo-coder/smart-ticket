package com.smartticket.ticket.service.impl;

import com.smartticket.ai.AiAssistService;
import com.smartticket.ai.model.AiSuggestion;
import com.smartticket.common.util.RedisUtil;
import com.smartticket.ticket.dto.SubmitTicketRequest;
import com.smartticket.ticket.entity.Ticket;
import com.smartticket.ticket.entity.TicketLog;
import com.smartticket.ticket.enums.TicketStatus;
import com.smartticket.ticket.mapper.TicketLogMapper;
import com.smartticket.ticket.mapper.TicketMapper;
import com.smartticket.ticket.service.TicketService;
import com.smartticket.ticket.vo.SubmitResultVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
public class TicketServiceImpl implements TicketService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TicketMapper ticketMapper;
    private final TicketLogMapper ticketLogMapper;
    private final AiAssistService aiAssistService;
    private final RedisUtil redisUtil;
    private final int slaHours;

    public TicketServiceImpl(TicketMapper ticketMapper, TicketLogMapper ticketLogMapper,
                             AiAssistService aiAssistService, RedisUtil redisUtil,
                             @Value("${smart-ticket.sla.default-hours}") int slaHours) {
        this.ticketMapper = ticketMapper;
        this.ticketLogMapper = ticketLogMapper;
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
