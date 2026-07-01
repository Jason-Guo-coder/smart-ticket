package com.smartticket.evaluation.service.impl;

import com.smartticket.common.exception.BizException;
import com.smartticket.common.result.ResultCode;
import com.smartticket.evaluation.dto.EvaluateRequest;
import com.smartticket.evaluation.entity.Evaluation;
import com.smartticket.evaluation.mapper.EvaluationMapper;
import com.smartticket.evaluation.service.EvaluationService;
import com.smartticket.ticket.service.TicketService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationServiceImpl implements EvaluationService {

    private final TicketService ticketService;
    private final EvaluationMapper evaluationMapper;

    public EvaluationServiceImpl(TicketService ticketService, EvaluationMapper evaluationMapper) {
        this.ticketService = ticketService;
        this.evaluationMapper = evaluationMapper;
    }

    @Override
    @Transactional
    public void evaluate(Long ticketId, Long userId, String role, EvaluateRequest req) {
        // 1) 状态流转 DONE→RATED（校验状态 + 本人/ADMIN + 写日志，B1/B2）；非 DONE 或非本人被拒
        ticketService.rate(ticketId, userId, role);
        // 2) 评价落库；ticket_id 唯一键兜底防重复（正常已被状态机拦截）
        Evaluation e = new Evaluation();
        e.setTicketId(ticketId);
        e.setScore(req.getScore());
        e.setTags(req.getTags());
        e.setComment(req.getComment());
        try {
            evaluationMapper.insert(e);
        } catch (DuplicateKeyException dup) {
            throw new BizException(ResultCode.CONFLICT, "该工单已评价");
        }
    }
}
