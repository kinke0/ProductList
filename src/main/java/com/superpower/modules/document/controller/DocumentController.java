package com.superpower.modules.document.controller;

import com.superpower.common.AuthUtils;
import com.superpower.common.Result;
import com.superpower.modules.document.dto.DocGenerateRequest;
import com.superpower.modules.document.entity.DocGenRecord;
import com.superpower.modules.document.service.DocumentService;
import com.superpower.modules.system.service.OperationLogService;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);
    private static final AtomicInteger threadCounter = new AtomicInteger(0);

    private final DocumentService documentService;
    private final SysUserService sysUserService;
    private final OperationLogService logService;
    private final ExecutorService docGenExecutor = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "doc-gen-" + threadCounter.incrementAndGet());
        t.setDaemon(true);
        return t;
    });

    public DocumentController(DocumentService documentService, SysUserService sysUserService, OperationLogService logService) {
        this.documentService = documentService;
        this.sysUserService = sysUserService;
        this.logService = logService;
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
        log.info("文档生成任务提交: recordId={}, docType={}, format={}, entryCount={}, customTabId={}",
                recordId, request.getDocType(), request.getFormat(), entryIds.size(), customTabId);
        java.util.concurrent.Future<?> future = docGenExecutor.submit(() -> {
            log.info("文档生成任务开始执行: recordId={}", recordId);
            try {
                String result = documentService.generateAndSaveDocument(
                        recordId, request.getDocType(), request.getFormat(), entryIds, request.getVersionId(), customTabId,
                        request.getIncludeImages() != null ? request.getIncludeImages() : true,
                        request.getCompressImages() != null && request.getCompressImages());
                log.info("文档生成任务完成: recordId={}, result={}", recordId, result);
                if (result != null) {
                    DocGenRecord rec = documentService.getGenRecord(recordId);
                    if (rec != null && !"completed".equals(rec.getStatus())) {
                        documentService.updateGenRecordSuccess(recordId, result, new java.io.File(result).length());
                    }
                } else {
                    DocGenRecord rec = documentService.getGenRecord(recordId);
                    if (rec != null && "generating".equals(rec.getStatus())) {
                        log.warn("文档生成result为null但状态仍为generating，标记为错误: recordId={}", recordId);
                        documentService.updateGenRecordError(recordId, "生成被取消或未正常完成");
                    }
                }
            } catch (Exception e) {
                log.error("文档生成任务异常: recordId={}, error={}", recordId, e.getMessage(), e);
                try {
                    documentService.updateGenRecordError(recordId, e.getMessage());
                } catch (Exception ex) {
                    try { documentService.updateGenRecordError(recordId, "生成失败: " + e.getClass().getSimpleName()); } catch (Exception ignored) {}
                }
            }
        });
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                future.get(10, java.util.concurrent.TimeUnit.MINUTES);
            } catch (java.util.concurrent.TimeoutException e) {
                future.cancel(true);
                try { documentService.updateGenRecordError(recordId, "生成超时（超过10分钟），已自动取消"); } catch (Exception ignored) {}
            } catch (Exception ignored) {}
        });

        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "GENERATE", "文档生成", "生成" + ("word".equals(request.getFormat()) ? "Word" : "Excel") + "文档: " + request.getDocName(),
                record.getId(), "DocGenRecord");
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
