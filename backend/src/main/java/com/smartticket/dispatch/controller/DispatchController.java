package com.smartticket.dispatch.controller;

import com.smartticket.common.annotation.AuditLog;
import com.smartticket.common.context.UserContext;
import com.smartticket.common.result.Result;
import com.smartticket.dispatch.dto.AssignRequest;
import com.smartticket.dispatch.service.DispatchService;
import com.smartticket.dispatch.vo.AutoAllResultVO;
import com.smartticket.dispatch.vo.DispatchBoardVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 派单管理（管理员，dispatch:manage）。派单操作写审计（B7）。 */
@RestController
@RequestMapping("/api/dispatch")
@PreAuthorize("hasAuthority('dispatch:manage')")
public class DispatchController {

    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    /** 派单页数据：待派单卡片（含 AI 建议）+ 工程师负载。 */
    @GetMapping("/board")
    public Result<DispatchBoardVO> board() {
        return Result.ok(dispatchService.board());
    }

    /** 手动指派 / 采纳 AI 建议。 */
    @AuditLog("派单")
    @PostMapping("/{id}/assign")
    public Result<Void> assign(@PathVariable Long id, @Valid @RequestBody AssignRequest req) {
        dispatchService.assign(id, req.getEngineerId(), UserContext.getUserId());
        return Result.ok();
    }

    /** 自动分配（按类别 + 负载）。 */
    @AuditLog("派单")
    @PostMapping("/{id}/auto")
    public Result<String> auto(@PathVariable Long id) {
        return Result.ok(dispatchService.autoAssign(id, UserContext.getUserId()));
    }

    /** 一键智能分派全部待派单。 */
    @AuditLog("派单")
    @PostMapping("/auto-all")
    public Result<AutoAllResultVO> autoAll() {
        return Result.ok(dispatchService.autoAssignAll(UserContext.getUserId()));
    }
}
