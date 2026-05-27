package com.superpower.modules.customtab.controller;

import com.superpower.common.Result;
import com.superpower.modules.customtab.entity.CustomTab;
import com.superpower.modules.customtab.service.CustomTabService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/custom-tab")
public class CustomTabController {

    private final CustomTabService customTabService;

    public CustomTabController(CustomTabService customTabService) {
        this.customTabService = customTabService;
    }

    @GetMapping("/{versionId}")
    public Result<List<CustomTab>> list(@PathVariable Long versionId) {
        return Result.success(customTabService.findByVersionId(versionId));
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
    public Result<Void> delete(@PathVariable Long id) {
        customTabService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<CustomTab> rename(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(customTabService.rename(id, body.get("name")));
    }

    @PostMapping("/{id}/entries")
    public Result<Void> addEntries(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) body.get("entryIds");
        List<Long> entryIds = rawIds.stream().map(Long::valueOf).collect(java.util.stream.Collectors.toList());
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
