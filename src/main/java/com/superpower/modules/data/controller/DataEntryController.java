package com.superpower.modules.data.controller;

import com.superpower.common.AuthUtils;
import com.superpower.common.Result;
import com.superpower.modules.data.dto.DataEntryDTO;
import com.superpower.modules.data.dto.DataEntrySummaryDTO;
import com.superpower.modules.data.dto.ExcelImportResult;
import com.superpower.modules.data.dto.RenumberRequest;
import com.superpower.modules.data.dto.TreeNodeDTO;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.service.DataEntryService;
import com.superpower.modules.document.service.DocumentService;
import com.superpower.modules.system.service.OperationLogService;
import com.superpower.modules.system.service.SysUserService;
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
    private final OperationLogService logService;
    private final SysUserService sysUserService;

    public DataEntryController(DataEntryService dataEntryService, VersionService versionService,
                               DocumentService documentService, OperationLogService logService,
                               SysUserService sysUserService) {
        this.dataEntryService = dataEntryService;
        this.versionService = versionService;
        this.documentService = documentService;
        this.logService = logService;
        this.sysUserService = sysUserService;
    }

    private void checkVersionEditPermission(Long versionId) {
        if (!versionService.isEditable(versionId)) {
            throw new RuntimeException("已发布版本不可修改");
        }
    }

    private String entryTitle(DataEntry e) {
        return e.getColProductSystem() != null && !e.getColProductSystem().isBlank()
                ? e.getColProductSystem() : "#" + e.getId();
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
    public ResponseEntity<byte[]> previewDownload(@PathVariable Long id, @RequestParam(defaultValue = "feature") String mode,
                                                   @RequestParam(defaultValue = "true") Boolean includeImages) throws Exception {
        DataEntry entry = dataEntryService.getById(id);
        List<Long> ids = dataEntryService.collectL3AndDescendantIds(id);
        byte[] data = documentService.generateDocument(mode, "word", ids, includeImages);
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
    public Result<List<DataEntrySummaryDTO>> query(
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
        List<DataEntry> entries = dataEntryService.query(versionId, customTabId, name, status, productManager,
                solution, versionTag, bizCategory, bizDomain, level);
        return Result.success(entries.stream().map(DataEntrySummaryDTO::fromEntity).toList());
    }

    @PostMapping
    public Result<DataEntry> create(@RequestBody DataEntryDTO dto, Authentication auth) {
        checkVersionEditPermission(dto.getVersionId());
        DataEntry created = dataEntryService.create(dto);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "CREATE", "数据清单", "新建: " + entryTitle(created), created.getId(), "DataEntry");
        return Result.success(created);
    }

    @PostMapping("/import-excel")
    public Result<ExcelImportResult> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("versionId") Long versionId, Authentication auth) {
        checkVersionEditPermission(versionId);
        ExcelImportResult result = dataEntryService.importFromExcel(file, versionId);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "IMPORT", "数据清单", "Excel导入 " + result.getSuccessRows() + " 条 (版本#" + versionId + ")");
        return Result.success(result);
    }

    @PutMapping("/{id}")
    public Result<DataEntry> update(@PathVariable Long id, @RequestBody DataEntryDTO dto, Authentication auth) {
        DataEntry entry = dataEntryService.getById(id);

        if (entry == null) {
            return Result.failed("记录不存在");
        }

        checkVersionEditPermission(entry.getVersionId());
        DataEntry updated = dataEntryService.update(id, dto);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "UPDATE", "数据清单", "编辑: " + entryTitle(entry), id, "DataEntry");
        return Result.success(updated);
    }

    @PutMapping("/sort")
    public Result<Void> updateSort(@RequestBody List<Map<String, Object>> sortList, Authentication auth) {
        for (Map<String, Object> item : sortList) {
            Object versionId = item.get("versionId");
            if (versionId != null) {
                checkVersionEditPermission(Long.valueOf(versionId.toString()));
            }
        }

        dataEntryService.updateSort(sortList);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "UPDATE", "数据清单", "拖拽排序调整 " + sortList.size() + " 条");
        return Result.success();
    }

    @PutMapping("/reorder/{versionId}")
    public Result<Void> reorder(@PathVariable Long versionId, Authentication auth) {
        checkVersionEditPermission(versionId);
        dataEntryService.reorderAll(versionId);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "UPDATE", "数据清单", "全量重排序 (版本#" + versionId + ")");
        return Result.success();
    }

    @DeleteMapping("/dedup/{versionId}")
    public Result<Integer> dedup(@PathVariable Long versionId, Authentication auth) {
        checkVersionEditPermission(versionId);
        int count = dataEntryService.dedupByVersion(versionId);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "DELETE", "数据清单", "去重删除 " + count + " 条 (版本#" + versionId + ")");
        return Result.success(count);
    }

    @DeleteMapping("/dedup-deep/{versionId}")
    public Result<Integer> dedupDeep(@PathVariable Long versionId, Authentication auth) {
        checkVersionEditPermission(versionId);
        int count = dataEntryService.dedupDeep(versionId);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "DELETE", "数据清单", "深度去重删除 " + count + " 条 (版本#" + versionId + ")");
        return Result.success(count);
    }

    @PutMapping("/{id}/level-up")
    public Result<Void> levelUp(@PathVariable Long id, Authentication auth) {
        DataEntry entry = dataEntryService.getById(id);

        if (entry == null) {
            return Result.failed("记录不存在");
        }

        checkVersionEditPermission(entry.getVersionId());

        dataEntryService.levelUp(id);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "UPDATE", "数据清单", "层级升级: " + entryTitle(entry), id, "DataEntry");
        return Result.success();
    }

    @PutMapping("/{id}/level-down")
    public Result<Void> levelDown(@PathVariable Long id, Authentication auth) {
        DataEntry entry = dataEntryService.getById(id);

        if (entry == null) {
            return Result.failed("记录不存在");
        }

        checkVersionEditPermission(entry.getVersionId());

        dataEntryService.levelDown(id);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "UPDATE", "数据清单", "层级降级: " + entryTitle(entry), id, "DataEntry");
        return Result.success();
    }

    @PutMapping("/{id}/move-to-parent")
    public Result<Void> moveToParent(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        Long newParentId = Long.valueOf(body.get("newParentId").toString());
        DataEntry entry = dataEntryService.getById(id);
        if (entry == null) {
            return Result.failed("记录不存在");
        }
        checkVersionEditPermission(entry.getVersionId());
        dataEntryService.moveToParent(id, newParentId);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "UPDATE", "数据清单", "移动: " + entryTitle(entry), id, "DataEntry");
        return Result.success();
    }

    @PutMapping("/{id}/move-to-sibling")
    public Result<Void> moveToSibling(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        Long targetId = Long.valueOf(body.get("targetId").toString());
        DataEntry entry = dataEntryService.getById(id);
        if (entry == null) {
            return Result.failed("记录不存在");
        }
        checkVersionEditPermission(entry.getVersionId());
        dataEntryService.moveToSibling(id, targetId);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "UPDATE", "数据清单", "移动: " + entryTitle(entry), id, "DataEntry");
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication auth) {
        DataEntry entry = dataEntryService.getById(id);

        if (entry == null) {
            return Result.failed("记录不存在");
        }

        checkVersionEditPermission(entry.getVersionId());

        dataEntryService.delete(id);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "DELETE", "数据清单", "删除: " + entryTitle(entry), id, "DataEntry");
        return Result.success();
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestParam Long versionId, @RequestBody List<Long> ids, Authentication auth) {
        checkVersionEditPermission(versionId);
        dataEntryService.batchDelete(ids);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "DELETE", "数据清单", "批量删除 " + ids.size() + " 条清单");
        return Result.success();
    }

    @GetMapping("/domain-tree/{versionId}")
    public Result<List<TreeNodeDTO>> getDomainTree(
            @PathVariable Long versionId,
            @RequestParam Long domainId,
            @RequestParam(required = false) Long categoryId) {
        return Result.success(dataEntryService.getDomainTree(versionId, domainId, categoryId));
    }

    @GetMapping("/sub-tree/{versionId}/{parentId}")
    public Result<List<TreeNodeDTO>> getSubTree(
            @PathVariable Long versionId,
            @PathVariable Long parentId) {
        return Result.success(dataEntryService.getSubTree(versionId, parentId));
    }

    @PutMapping("/batch-category")
    public Result<Integer> batchUpdateCategory(@RequestBody Map<String, Object> body, Authentication auth) {
        Long versionId = Long.valueOf(body.get("versionId").toString());
        checkVersionEditPermission(versionId);
        List<Long> entryIds = ((List<Number>) body.get("entryIds")).stream().map(Number::longValue).toList();
        Long categoryId = body.get("categoryId") != null ? Long.valueOf(body.get("categoryId").toString()) : null;
        Long domainId = body.get("domainId") != null ? Long.valueOf(body.get("domainId").toString()) : null;
        Long productId = body.get("productId") != null ? Long.valueOf(body.get("productId").toString()) : null;
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
        int count = dataEntryService.batchUpdateCategory(versionId, entryIds, categoryId, domainId, productId, parentId);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "UPDATE", "数据清单", "批量修改分类/域 " + count + " 条");
        return Result.success(count);
    }

    @PutMapping("/fix-hierarchy/{versionId}")
    public Result<Map<String, Object>> fixDataHierarchy(@PathVariable Long versionId, Authentication auth) {
        checkVersionEditPermission(versionId);
        Map<String, Object> result = dataEntryService.fixDataHierarchy(versionId);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "UPDATE", "数据清单", "修复层级结构 (版本#" + versionId + ")");
        return Result.success(result);
    }

    @PutMapping("/renumber")
    public Result<Void> renumber(@RequestBody RenumberRequest request, Authentication auth) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return Result.failed("重编号列表不能为空");
        }
        DataEntry first = dataEntryService.getById(request.getItems().get(0).getEntryId());
        if (first == null) {
            return Result.failed("条目不存在");
        }
        Long versionId = first.getVersionId();
        checkVersionEditPermission(versionId);
        dataEntryService.renumberEntries(versionId, request.getItems());
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "UPDATE", "数据清单", "编码重排序 (" + request.getItems().size() + "条)");
        return Result.success();
    }

    @PostMapping("/copy")
    public Result<Void> copyEntries(@RequestBody Map<String, Object> body, Authentication auth) {
        List<Long> sourceIds = ((List<Number>) body.get("sourceIds")).stream().map(Number::longValue).toList();
        Long targetId = Long.valueOf(body.get("targetId").toString());
        String mode = (String) body.get("mode");
        if (sourceIds == null || sourceIds.isEmpty()) {
            return Result.failed("请选择要复制的节点");
        }
        DataEntry target = dataEntryService.getById(targetId);
        if (target == null) {
            return Result.failed("目标节点不存在");
        }
        checkVersionEditPermission(target.getVersionId());
        dataEntryService.copyEntriesToTarget(sourceIds, targetId, mode);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "CREATE", "数据清单", "复制 " + sourceIds.size() + " 条到" + ("child".equals(mode) ? "下级" : "下方"));
        return Result.success();
    }

    @PutMapping("/move")
    public Result<Void> moveEntries(@RequestBody Map<String, Object> body, Authentication auth) {
        List<Long> sourceIds = ((List<Number>) body.get("sourceIds")).stream().map(Number::longValue).toList();
        Long targetId = Long.valueOf(body.get("targetId").toString());
        String mode = (String) body.get("mode");
        if (sourceIds == null || sourceIds.isEmpty()) {
            return Result.failed("请选择要移动的节点");
        }
        DataEntry target = dataEntryService.getById(targetId);
        if (target == null) {
            return Result.failed("目标节点不存在");
        }
        checkVersionEditPermission(target.getVersionId());
        dataEntryService.moveEntriesToTarget(sourceIds, targetId, mode);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "UPDATE", "数据清单", "移动 " + sourceIds.size() + " 条到" + ("child".equals(mode) ? "下级" : "下方"));
        return Result.success();
    }
}
