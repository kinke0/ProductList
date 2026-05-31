package com.superpower.modules.document.controller;

import com.superpower.common.Result;
import com.superpower.modules.document.dto.DocGenerateRequest;
import com.superpower.modules.document.entity.DocGenRecord;
import com.superpower.modules.document.service.DocumentService;
import com.superpower.modules.system.service.SysUserService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final SysUserService sysUserService;

    public DocumentController(DocumentService documentService, SysUserService sysUserService) {
        this.documentService = documentService;
        this.sysUserService = sysUserService;
    }

    @PostMapping("/generate")
    public Result<DocGenRecord> generate(@Valid @RequestBody DocGenerateRequest request, Authentication auth) {
        Long userId = null;
        String userName = "";
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User user) {
            userName = user.getUsername();
        }
        String displayName = userName;
        try {
            com.superpower.modules.system.entity.SysUser sysUser = sysUserService.findByUsername(userName);
            if (sysUser != null && sysUser.getNickname() != null && !sysUser.getNickname().isEmpty()) {
                displayName = sysUser.getNickname();
            }
        } catch (Exception ignored) {}

        List<Long> entryIds = (request.getEntryIds() != null && !request.getEntryIds().isEmpty())
                ? request.getEntryIds() : List.of();

        Long customTabId = request.getCustomTabId();

        DocGenRecord record = documentService.createGenRecord(
                request.getVersionId(), request.getDocName(), request.getDocType(), request.getFormat(), entryIds, userId, displayName);

        Long recordId = record.getId();
        new Thread(() -> {
            try {
                documentService.generateAndSaveDocument(
                        recordId, request.getDocType(), request.getFormat(), entryIds, request.getVersionId(), customTabId);
            } catch (Exception e) {
                try {
                    documentService.updateGenRecordError(recordId, e.getMessage());
                } catch (Exception ex) {
                    try { documentService.updateGenRecordError(recordId, "生成失败: " + e.getClass().getSimpleName()); } catch (Exception ignored) {}
                }
            }
        }).start();

        return Result.success(record);
    }

    @GetMapping("/records/{id}/progress")
    public Result<DocGenRecord> getProgress(@PathVariable Long id) {
        return Result.success(documentService.getGenRecord(id));
    }

    @GetMapping("/records")
    public Result<List<DocGenRecord>> getRecords(@RequestParam Long versionId) {
        return Result.success(documentService.getGenRecords(versionId));
    }

    @GetMapping("/records/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws Exception {
        DocGenRecord record = documentService.getGenRecord(id);
        if (record == null || !"completed".equals(record.getStatus())) {
            return ResponseEntity.notFound().build();
        }

        File file = new File(record.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        String filename = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8);
        String contentType = "word".equals(record.getFormat())
                ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .body(new FileSystemResource(file));
    }

    @DeleteMapping("/records/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteRecord(@PathVariable Long id) {
        documentService.deleteGenRecord(id);
        return Result.success();
    }
}
