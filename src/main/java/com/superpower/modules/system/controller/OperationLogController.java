package com.superpower.modules.system.controller;

import com.superpower.common.Result;
import com.superpower.modules.system.entity.OperationLog;
import com.superpower.modules.system.service.OperationLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operation-logs")
public class OperationLogController {

    private final OperationLogService logService;

    public OperationLogController(OperationLogService logService) {
        this.logService = logService;
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<OperationLog>> getByUser(@PathVariable Long userId) {
        return Result.success(logService.getByUserId(userId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<OperationLog>> getAll() {
        return Result.success(logService.getAll());
    }
}
