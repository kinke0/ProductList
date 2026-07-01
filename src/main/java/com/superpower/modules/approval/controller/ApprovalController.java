package com.superpower.modules.approval.controller;

import com.superpower.common.AuthUtils;
import com.superpower.common.Result;
import com.superpower.modules.approval.entity.ApprovalLog;
import com.superpower.modules.approval.service.ApprovalService;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.service.DataEntryService;
import com.superpower.modules.system.entity.SysUser;
import com.superpower.modules.system.service.OperationLogService;
import com.superpower.modules.system.service.SysUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/approval")
public class ApprovalController {

    private final ApprovalService approvalService;
    private final DataEntryService dataEntryService;
    private final SysUserService sysUserService;
    private final OperationLogService logService;

    public ApprovalController(ApprovalService approvalService, DataEntryService dataEntryService,
                              SysUserService sysUserService, OperationLogService logService) {
        this.approvalService = approvalService;
        this.dataEntryService = dataEntryService;
        this.sysUserService = sysUserService;
        this.logService = logService;
    }

    @PostMapping("/{entryId}")
    public Result<Void> approve(@PathVariable Long entryId,
                                @RequestBody Map<String, String> body,
                                Authentication auth) {
        String action = body.get("action");
        String comment = body.get("comment");
        SysUser user = sysUserService.findByUsername(auth.getName());
        String roleCode = user.getRole() != null ? user.getRole().getCode() : "USER";
        approvalService.approve(entryId, action, roleCode, user.getId(), user.getNickname(), comment);
        DataEntry entry = dataEntryService.getById(entryId);
        String title = entry != null && entry.getColProductSystem() != null ? entry.getColProductSystem() : "#" + entryId;
        logService.record(user.getId(), user.getUsername(), action.toUpperCase(), "审批管理",
                "审批[" + action + "]: " + title, entryId, "DataEntry");
        return Result.success();
    }

    @GetMapping("/{entryId}/logs")
    public Result<List<ApprovalLog>> getLogs(@PathVariable Long entryId) {
        return Result.success(approvalService.getLogs(entryId));
    }
}
