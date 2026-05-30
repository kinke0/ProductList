package com.superpower.modules.data.controller;

import com.superpower.common.Result;
import com.superpower.modules.data.dto.DataEntryDTO;
import com.superpower.modules.data.dto.ExcelImportResult;
import com.superpower.modules.data.dto.TreeNodeDTO;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.service.DataEntryService;
import com.superpower.modules.document.service.DocumentService;
import com.superpower.modules.version.service.VersionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class DataEntryController {

    private final DataEntryService dataEntryService;
    private final VersionService versionService;
    private final DocumentService documentService;

    public DataEntryController(DataEntryService dataEntryService, VersionService versionService,
                               DocumentService documentService) {
        this.dataEntryService = dataEntryService;
        this.versionService = versionService;
        this.documentService = documentService;
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
            @RequestParam(required = false) List<String> status,
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
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String productManager,
            @RequestParam(required = false) String solution,
            @RequestParam(required = false) String versionTag) {
        return Result.success(dataEntryService.getChildren(versionId, parentId, name, status, productManager, solution, versionTag));
    }

    @GetMapping("/{id}")
    public Result<DataEntry> getById(@PathVariable Long id) {
        return Result.success(dataEntryService.getById(id));
    }

    @GetMapping(value = "/{id}/preview", produces = "text/html;charset=UTF-8")
    public String preview(@PathVariable Long id, @RequestParam(defaultValue = "feature") String mode, Authentication auth) {
        boolean isEditing = versionService.isEditable(dataEntryService.getById(id).getVersionId());
        String roleCode = auth != null ? auth.getName() : null;
        return dataEntryService.getPreviewHtml(id, isEditing, roleCode, mode);
    }

    @GetMapping("/{id}/preview-download")
    public ResponseEntity<byte[]> previewDownload(@PathVariable Long id, @RequestParam(defaultValue = "feature") String mode) throws Exception {
        DataEntry entry = dataEntryService.getById(id);
        List<Long> ids = dataEntryService.collectL3AndDescendantIds(id);
        byte[] data = documentService.generateDocument(mode, "word", ids);
        String suffix = "bid".equals(mode) ? "_招标参数" : "_功能说明";
        String filename = (entry.getColProductSystem() != null ? entry.getColProductSystem() : "预览") + suffix + ".docx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + java.net.URLEncoder.encode(filename, "UTF-8") + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping(value = "/preview-batch", produces = "text/html;charset=UTF-8")
    public String previewBatch(@RequestParam String entryIds, @RequestParam(defaultValue = "feature") String mode, Authentication auth) {
        List<Long> ids = java.util.Arrays.stream(entryIds.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(Long::parseLong).toList();
        String roleCode = auth != null ? auth.getName() : null;
        return dataEntryService.getPreviewHtml(ids, false, roleCode, mode);
    }

    @GetMapping("/query/{versionId}")
    public Result<List<DataEntry>> query(
            @PathVariable Long versionId,
            @RequestParam(required = false) Long customTabId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String productManager,
            @RequestParam(required = false) String solution,
            @RequestParam(required = false) String versionTag,
            @RequestParam(required = false) String bizCategory,
            @RequestParam(required = false) String bizDomain,
            @RequestParam(required = false) Integer level) {
        return Result.success(dataEntryService.query(versionId, customTabId, name, status, productManager,
                solution, versionTag, bizCategory, bizDomain, level));
    }

    @PostMapping
    public Result<DataEntry> create(@RequestBody DataEntryDTO dto) {
        checkVersionEditPermission(dto.getVersionId());

        return Result.success(dataEntryService.create(dto));
    }

    @PostMapping("/import-excel")
    public Result<ExcelImportResult> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("versionId") Long versionId) {
        checkVersionEditPermission(versionId);
        return Result.success(dataEntryService.importFromExcel(file, versionId));
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

    @PutMapping("/reorder/{versionId}")
    public Result<Void> reorder(@PathVariable Long versionId) {
        checkVersionEditPermission(versionId);
        dataEntryService.reorderAll(versionId);
        return Result.success();
    }

    @DeleteMapping("/dedup/{versionId}")
    public Result<Integer> dedup(@PathVariable Long versionId) {
        checkVersionEditPermission(versionId);
        int count = dataEntryService.dedupByVersion(versionId);
        return Result.success(count);
    }

    @DeleteMapping("/dedup-deep/{versionId}")
    public Result<Integer> dedupDeep(@PathVariable Long versionId) {
        checkVersionEditPermission(versionId);
        int count = dataEntryService.dedupDeep(versionId);
        return Result.success(count);
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

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestParam Long versionId, @RequestBody List<Long> ids) {
        checkVersionEditPermission(versionId);
        dataEntryService.batchDelete(ids);
        return Result.success();
    }

    @PutMapping("/batch-category")
    public Result<Integer> batchUpdateCategory(@RequestBody Map<String, Object> body) {
        Long versionId = Long.valueOf(body.get("versionId").toString());
        checkVersionEditPermission(versionId);
        List<Long> entryIds = ((List<Number>) body.get("entryIds")).stream().map(Number::longValue).toList();
        Long categoryId = body.get("categoryId") != null ? Long.valueOf(body.get("categoryId").toString()) : null;
        Long domainId = body.get("domainId") != null ? Long.valueOf(body.get("domainId").toString()) : null;
        int count = dataEntryService.batchUpdateCategory(versionId, entryIds, categoryId, domainId);
        return Result.success(count);
    }
}
