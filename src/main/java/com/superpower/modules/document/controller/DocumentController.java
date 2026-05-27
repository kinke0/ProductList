package com.superpower.modules.document.controller;

import com.superpower.common.Result;
import com.superpower.modules.document.dto.DocGenerateRequest;
import com.superpower.modules.document.entity.DocGenRecord;
import com.superpower.modules.document.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/generate")
    public Result<DocGenRecord> generate(@Valid @RequestBody DocGenerateRequest request, Authentication auth) {
        Long userId = null;
        String userName = "";
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User user) {
            userName = user.getUsername();
        }

        List<Long> entryIds = (request.getEntryIds() != null && !request.getEntryIds().isEmpty())
                ? request.getEntryIds() : List.of();

        Long customTabId = request.getCustomTabId();

        DocGenRecord record = documentService.createGenRecord(
                request.getVersionId(), request.getDocType(), request.getFormat(), entryIds, userId, userName);

        Long recordId = record.getId();
        new Thread(() -> {
            try {
                documentService.generateAndSaveDocument(
                        recordId, request.getDocType(), request.getFormat(), entryIds, request.getVersionId(), customTabId);
            } catch (Exception e) {
                documentService.updateGenRecordError(recordId, e.getMessage());
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
    public Result<Void> deleteRecord(@PathVariable Long id) {
        documentService.deleteGenRecord(id);
        return Result.success();
    }

    @GetMapping("/test-word")
    public ResponseEntity<byte[]> testWord() throws Exception {
        byte[] data = documentService.generateTestWord();
        String filename = URLEncoder.encode("测试标题.docx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .body(data);
    }
}
