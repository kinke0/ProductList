package com.superpower.modules.data.controller;

import com.superpower.common.Result;
import com.superpower.modules.data.dto.DataEntryDTO;
import com.superpower.modules.data.dto.TreeNodeDTO;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.service.DataEntryService;
import com.superpower.modules.version.service.VersionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class DataEntryController {

    private final DataEntryService dataEntryService;
    private final VersionService versionService;

    public DataEntryController(DataEntryService dataEntryService, VersionService versionService) {
        this.dataEntryService = dataEntryService;
        this.versionService = versionService;
    }

    private void checkVersionEditPermission(Long versionId) {
        if (!versionService.isEditable(versionId)) {
            throw new RuntimeException("已发布版本不可修改");
        }
    }

    @GetMapping("/tree/{versionId}")
    public Result<List<TreeNodeDTO>> getTree(
            @PathVariable Long versionId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String productManager,
            @RequestParam(required = false) String solution,
            @RequestParam(required = false) String versionTag) {
        return Result.success(dataEntryService.getTree(versionId, name, status, productManager, solution, versionTag));
    }

    @GetMapping("/children/{versionId}/{parentId}")
    public Result<List<DataEntry>> getChildren(
            @PathVariable Long versionId,
            @PathVariable Long parentId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String productManager,
            @RequestParam(required = false) String solution,
            @RequestParam(required = false) String versionTag) {
        return Result.success(dataEntryService.getChildren(versionId, parentId, name, status, productManager, solution, versionTag));
    }

    @GetMapping("/{id}")
    public Result<DataEntry> getById(@PathVariable Long id) {
        return Result.success(dataEntryService.getById(id));
    }

    @GetMapping("/query/{versionId}")
    public Result<List<DataEntry>> query(
            @PathVariable Long versionId,
            @RequestParam(required = false) Long customTabId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String productManager,
            @RequestParam(required = false) String solution,
            @RequestParam(required = false) String versionTag,
            @RequestParam(required = false) String bizCategory,
            @RequestParam(required = false) String bizDomain) {
        return Result.success(dataEntryService.query(versionId, customTabId, name, status, productManager,
                solution, versionTag, bizCategory, bizDomain));
    }

    @PostMapping
    public Result<DataEntry> create(@RequestBody DataEntryDTO dto) {
        checkVersionEditPermission(dto.getVersionId());

        return Result.success(dataEntryService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<DataEntry> update(@PathVariable Long id, @RequestBody DataEntryDTO dto) {
        DataEntry entry = dataEntryService.getById(id);

        if (entry == null) {
            return Result.failed("记录不存在");
        }

        checkVersionEditPermission(entry.getVersionId());

        return Result.success(dataEntryService.update(id, dto));
    }

    @PutMapping("/sort")
    public Result<Void> updateSort(@RequestBody List<Map<String, Object>> sortList) {
        for (Map<String, Object> item : sortList) {
            Object versionId = item.get("versionId");
            if (versionId != null) {
                checkVersionEditPermission(Long.valueOf(versionId.toString()));
            }
        }

        dataEntryService.updateSort(sortList);
        return Result.success();
    }

    @PutMapping("/{id}/level-up")
    public Result<Void> levelUp(@PathVariable Long id) {
        DataEntry entry = dataEntryService.getById(id);

        if (entry == null) {
            return Result.failed("记录不存在");
        }

        checkVersionEditPermission(entry.getVersionId());

        dataEntryService.levelUp(id);
        return Result.success();
    }

    @PutMapping("/{id}/level-down")
    public Result<Void> levelDown(@PathVariable Long id) {
        DataEntry entry = dataEntryService.getById(id);

        if (entry == null) {
            return Result.failed("记录不存在");
        }

        checkVersionEditPermission(entry.getVersionId());

        dataEntryService.levelDown(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        DataEntry entry = dataEntryService.getById(id);

        if (entry == null) {
            return Result.failed("记录不存在");
        }

        checkVersionEditPermission(entry.getVersionId());

        dataEntryService.delete(id);
        return Result.success();
    }
}
