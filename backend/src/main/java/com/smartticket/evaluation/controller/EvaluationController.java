package com.smartticket.evaluation.controller;

import com.smartticket.common.annotation.AuditLog;
import com.smartticket.common.annotation.Idempotent;
import com.smartticket.common.context.UserContext;
import com.smartticket.common.result.Result;
import com.smartticket.evaluation.dto.EvaluateRequest;
import com.smartticket.evaluation.service.EvaluationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 工单评价（报修人，ticket:evaluate）。幂等防抖（B9）+ 写审计（B7）。 */
@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PreAuthorize("hasAuthority('ticket:evaluate')")
    @Idempotent
    @AuditLog("评价")
    @PostMapping("/{ticketId}")
    public Result<Void> evaluate(@PathVariable Long ticketId, @Valid @RequestBody EvaluateRequest req) {
        evaluationService.evaluate(ticketId, UserContext.getUserId(), UserContext.getRole(), req);
        return Result.ok();
    }
}
