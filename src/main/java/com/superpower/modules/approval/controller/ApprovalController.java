package com.superpower.modules.approval.controller;

import com.superpower.common.Result;
import com.superpower.modules.approval.entity.ApprovalLog;
import com.superpower.modules.approval.service.ApprovalService;
import com.superpower.modules.system.entity.SysUser;
import com.superpower.modules.system.service.SysUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/approval")
public class ApprovalController {

    private final ApprovalService approvalService;
    private final SysUserService sysUserService;

    public ApprovalController(ApprovalService approvalService, SysUserService sysUserService) {
        this.approvalService = approvalService;
        this.sysUserService = sysUserService;
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
        return Result.success();
    }

    @GetMapping("/{entryId}/logs")
    public Result<List<ApprovalLog>> getLogs(@PathVariable Long entryId) {
        return Result.success(approvalService.getLogs(entryId));
    }
}
