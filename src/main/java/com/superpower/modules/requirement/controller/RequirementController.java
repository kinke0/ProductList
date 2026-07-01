package com.superpower.modules.requirement.controller;

import com.superpower.common.AuthUtils;
import com.superpower.common.Result;
import com.superpower.modules.requirement.dto.ReqActionDTO;
import com.superpower.modules.requirement.dto.ReqItemDTO;
import com.superpower.modules.requirement.entity.ReqItem;
import com.superpower.modules.requirement.entity.ReqLog;
import com.superpower.modules.requirement.service.RequirementService;
import com.superpower.modules.system.repository.SysUserRepository;
import com.superpower.modules.system.service.OperationLogService;
import com.superpower.modules.system.service.SysUserService;
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
    private final OperationLogService logService;
    private final SysUserService sysUserService;

    public RequirementController(RequirementService service, SysUserRepository sysUserRepository,
                                  OperationLogService logService, SysUserService sysUserService) {
        this.service = service;
        this.sysUserRepository = sysUserRepository;
        this.logService = logService;
        this.sysUserService = sysUserService;
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

    private String reqTitle(ReqItem item) {
        return item.getTitle() != null && !item.getTitle().isBlank()
                ? item.getTitle() : "#" + item.getId();
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
        ReqItem item = service.create(getUserId(auth), dto);
        logService.record(getUserId(auth), AuthUtils.getUsername(auth),
                "CREATE", "需求管理", "创建需求: " + reqTitle(item), item.getId(), "ReqItem");
        return Result.success(item);
    }

    @PutMapping("/{id}")
    public Result<ReqItem> update(@PathVariable Long id, @RequestBody ReqItemDTO dto, Authentication auth) {
        ReqItem item = service.update(id, getUserId(auth), dto);
        logService.record(getUserId(auth), AuthUtils.getUsername(auth),
                "UPDATE", "需求管理", "编辑需求: " + reqTitle(item), id, "ReqItem");
        return Result.success(item);
    }

    @PutMapping("/{id}/confirm")
    public Result<ReqItem> confirm(@PathVariable Long id, @RequestBody(required = false) ReqActionDTO dto, Authentication auth) {
        ReqItem item = service.confirm(id, getUserId(auth), dto);
        logService.record(getUserId(auth), AuthUtils.getUsername(auth),
                "UPDATE", "需求管理", "确认需求: " + reqTitle(item), id, "ReqItem");
        return Result.success(item);
    }

    @PutMapping("/{id}/develop")
    public Result<ReqItem> develop(@PathVariable Long id, @RequestBody(required = false) ReqActionDTO dto, Authentication auth) {
        ReqItem item = service.develop(id, getUserId(auth), dto);
        logService.record(getUserId(auth), AuthUtils.getUsername(auth),
                "UPDATE", "需求管理", "开发需求: " + reqTitle(item), id, "ReqItem");
        return Result.success(item);
    }

    @PutMapping("/{id}/ready")
    public Result<ReqItem> ready(@PathVariable Long id, @RequestBody(required = false) ReqActionDTO dto, Authentication auth) {
        ReqItem item = service.ready(id, getUserId(auth), dto);
        logService.record(getUserId(auth), AuthUtils.getUsername(auth),
                "UPDATE", "需求管理", "就绪需求: " + reqTitle(item), id, "ReqItem");
        return Result.success(item);
    }

    @PutMapping("/{id}/release")
    public Result<ReqItem> release(@PathVariable Long id, @RequestBody ReqActionDTO dto, Authentication auth) {
        ReqItem item = service.release(id, getUserId(auth), dto);
        logService.record(getUserId(auth), AuthUtils.getUsername(auth),
                "UPDATE", "需求管理", "发布需求: " + reqTitle(item), id, "ReqItem");
        return Result.success(item);
    }

    @PutMapping("/{id}/reject")
    public Result<ReqItem> reject(@PathVariable Long id, @RequestBody ReqActionDTO dto, Authentication auth) {
        ReqItem item = service.reject(id, getUserId(auth), dto);
        logService.record(getUserId(auth), AuthUtils.getUsername(auth),
                "UPDATE", "需求管理", "驳回需求: " + reqTitle(item), id, "ReqItem");
        return Result.success(item);
    }

    @PutMapping("/{id}/cancel")
    public Result<ReqItem> cancel(@PathVariable Long id, Authentication auth) {
        ReqItem item = service.cancel(id, getUserId(auth));
        logService.record(getUserId(auth), AuthUtils.getUsername(auth),
                "UPDATE", "需求管理", "取消需求: " + reqTitle(item), id, "ReqItem");
        return Result.success(item);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id, Authentication auth) {
        ReqItem item = service.getById(id);
        String title = item != null ? reqTitle(item) : "#" + id;
        service.delete(id);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "DELETE", "需求管理", "删除需求: " + title, id, "ReqItem");
        return Result.success();
    }
}
