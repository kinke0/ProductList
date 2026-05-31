package com.superpower.modules.requirement.controller;

import com.superpower.common.Result;
import com.superpower.modules.requirement.dto.ReqActionDTO;
import com.superpower.modules.requirement.dto.ReqItemDTO;
import com.superpower.modules.requirement.entity.ReqItem;
import com.superpower.modules.requirement.entity.ReqLog;
import com.superpower.modules.requirement.service.RequirementService;
import com.superpower.modules.system.repository.SysUserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requirements")
public class RequirementController {

    private final RequirementService service;
    private final SysUserRepository sysUserRepository;

    public RequirementController(RequirementService service, SysUserRepository sysUserRepository) {
        this.service = service;
        this.sysUserRepository = sysUserRepository;
    }

    private Long getUserId(Authentication auth) {
        String username = auth.getName();
        return sysUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"))
                .getId();
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @GetMapping
    public Result<List<ReqItem>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) String creatorName,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String priority,
            Authentication auth) {
        if ("my".equals(scope)) {
            createdBy = getUserId(auth);
        }
        if (creatorName != null && !creatorName.isBlank()) {
            var users = sysUserRepository.findAll().stream()
                    .filter(u -> (u.getNickname() != null && u.getNickname().contains(creatorName))
                            || (u.getUsername() != null && u.getUsername().contains(creatorName)))
                    .toList();
            if (!users.isEmpty()) {
                createdBy = users.get(0).getId();
            }
        }
        var list = service.listAll(status, createdBy, startDate, endDate, category, domain, type, priority);
        return Result.success(list);
    }

    @GetMapping("/my")
    public Result<List<ReqItem>> myRequirements(Authentication auth) {
        return Result.success(service.listByUser(getUserId(auth)));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getDetail(@PathVariable Long id) {
        ReqItem item = service.getById(id);
        List<ReqLog> logs = service.getLogs(id);
        Map<String, Object> detail = new HashMap<>();
        detail.put("item", item);
        detail.put("logs", logs);
        detail.put("creatorName", service.getNickname(item.getCreatedBy()));
        return Result.success(detail);
    }

    @GetMapping("/stats")
    public Result<Map<String, Long>> getStatusStats(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) String creatorName,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String priority,
            Authentication auth) {
        Long resolvedCreator = resolveCreator(createdBy, creatorName, scope, auth);
        return Result.success(service.getStatusStats(status, resolvedCreator, startDate, endDate, category, null, type, priority));
    }

    @GetMapping("/stats-by-module")
    public Result<Map<String, Long>> getModuleStats(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) String creatorName,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String priority,
            Authentication auth) {
        Long resolvedCreator = resolveCreator(createdBy, creatorName, scope, auth);
        return Result.success(service.getModuleStats(status, resolvedCreator, startDate, endDate, category, null, type, priority));
    }

    @GetMapping("/stats-by-type")
    public Result<Map<String, Long>> getTypeStats(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) String creatorName,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String priority,
            Authentication auth) {
        Long resolvedCreator = resolveCreator(createdBy, creatorName, scope, auth);
        return Result.success(service.getTypeStats(status, resolvedCreator, startDate, endDate, category, null, type, priority));
    }

    private Long resolveCreator(Long createdBy, String creatorName, String scope, Authentication auth) {
        if ("my".equals(scope)) {
            return getUserId(auth);
        }
        if (creatorName != null && !creatorName.isBlank()) {
            var users = sysUserRepository.findAll().stream()
                    .filter(u -> (u.getNickname() != null && u.getNickname().contains(creatorName))
                            || (u.getUsername() != null && u.getUsername().contains(creatorName)))
                    .toList();
            if (!users.isEmpty()) {
                return users.get(0).getId();
            }
        }
        return createdBy;
    }

    @PostMapping
    public Result<ReqItem> create(@RequestBody ReqItemDTO dto, Authentication auth) {
        return Result.success(service.create(getUserId(auth), dto));
    }

    @PutMapping("/{id}")
    public Result<ReqItem> update(@PathVariable Long id, @RequestBody ReqItemDTO dto, Authentication auth) {
        return Result.success(service.update(id, getUserId(auth), dto));
    }

    @PutMapping("/{id}/confirm")
    public Result<ReqItem> confirm(@PathVariable Long id, @RequestBody(required = false) ReqActionDTO dto, Authentication auth) {
        return Result.success(service.confirm(id, getUserId(auth), dto));
    }

    @PutMapping("/{id}/develop")
    public Result<ReqItem> develop(@PathVariable Long id, @RequestBody(required = false) ReqActionDTO dto, Authentication auth) {
        return Result.success(service.develop(id, getUserId(auth), dto));
    }

    @PutMapping("/{id}/ready")
    public Result<ReqItem> ready(@PathVariable Long id, @RequestBody(required = false) ReqActionDTO dto, Authentication auth) {
        return Result.success(service.ready(id, getUserId(auth), dto));
    }

    @PutMapping("/{id}/release")
    public Result<ReqItem> release(@PathVariable Long id, @RequestBody ReqActionDTO dto, Authentication auth) {
        return Result.success(service.release(id, getUserId(auth), dto));
    }

    @PutMapping("/{id}/reject")
    public Result<ReqItem> reject(@PathVariable Long id, @RequestBody ReqActionDTO dto, Authentication auth) {
        return Result.success(service.reject(id, getUserId(auth), dto));
    }

    @PutMapping("/{id}/cancel")
    public Result<ReqItem> cancel(@PathVariable Long id, Authentication auth) {
        return Result.success(service.cancel(id, getUserId(auth)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success();
    }
}
