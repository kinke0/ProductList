package com.superpower.modules.customtab.controller;

import com.superpower.common.Result;
import com.superpower.common.BusinessException;
import com.superpower.modules.customtab.entity.CustomTab;
import com.superpower.modules.customtab.service.CustomTabService;
import com.superpower.modules.system.entity.SysUser;
import com.superpower.modules.system.service.SysUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/custom-tab")
public class CustomTabController {

    private final CustomTabService customTabService;
    private final SysUserService sysUserService;

    public CustomTabController(CustomTabService customTabService, SysUserService sysUserService) {
        this.customTabService = customTabService;
        this.sysUserService = sysUserService;
    }

    private Long getUserId(Authentication auth) {
        return sysUserService.findByUsername(auth.getName()).getId();
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private void checkOwnerOrAdmin(Long tabId, Authentication auth) {
        if (isAdmin(auth)) return;
        CustomTab tab = customTabService.getById(tabId);
        Long userId = getUserId(auth);
        if (tab == null || !userId.equals(tab.getUserId())) {
            throw new BusinessException("仅创建人或管理员可操作");
        }
    }

    @GetMapping("/{versionId}")
    public Result<List<CustomTab>> list(@PathVariable Long versionId) {
        return Result.success(customTabService.findByVersionId(versionId));
    }

    @PostMapping("/create-with-filter")
    public Result<CustomTab> createWithFilter(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Long versionId = Long.valueOf(body.get("versionId").toString());
        Long userId = body.get("userId") != null ? Long.valueOf(body.get("userId").toString()) : null;
        String entryName = (String) body.getOrDefault("entryName", "");
        List<String> statusList = body.get("statusList") != null ? (List<String>) body.get("statusList") : List.of();
        String productManager = (String) body.getOrDefault("productManager", "");
        String solution = (String) body.getOrDefault("solution", "");
        String versionTag = (String) body.getOrDefault("versionTag", "");
        CustomTab tab = customTabService.createWithFilter(name, versionId, userId,
                entryName, statusList, productManager, solution, versionTag);
        return Result.success(tab);
    }

    @PostMapping
    public Result<CustomTab> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Long versionId = Long.valueOf(body.get("versionId").toString());
        Long userId = null;
        if (body.get("userId") != null) {
            userId = Long.valueOf(body.get("userId").toString());
        }
        return Result.success(customTabService.create(name, versionId, userId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication auth) {
        checkOwnerOrAdmin(id, auth);
        customTabService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<CustomTab> rename(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication auth) {
        checkOwnerOrAdmin(id, auth);
        return Result.success(customTabService.rename(id, body.get("name")));
    }

    @PostMapping("/{id}/entries")
    public Result<Void> addEntries(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> rawIds = (List<Number>) body.get("entryIds");
        List<Long> entryIds = rawIds.stream().map(Number::longValue).collect(java.util.stream.Collectors.toList());
        customTabService.addEntries(id, entryIds);
        return Result.success();
    }

    @DeleteMapping("/{id}/entries/{entryId}")
    public Result<Void> removeEntry(@PathVariable Long id, @PathVariable Long entryId) {
        customTabService.removeEntry(id, entryId);
        return Result.success();
    }

    @PutMapping("/{id}/sort")
    public Result<Void> updateSort(@PathVariable Long id, @RequestBody List<Map<String, Object>> sortList) {
        customTabService.updateSortOrders(id, sortList);
        return Result.success();
    }
}
