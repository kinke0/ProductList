package com.superpower.modules.image.controller;

import com.superpower.common.Result;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.image.dto.ImageDirectoryNode;
import com.superpower.modules.image.dto.MigrationResult;
import com.superpower.modules.image.dto.MigrationTaskProgress;
import com.superpower.modules.image.entity.ImageResource;
import com.superpower.modules.image.service.ImageResourceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageResourceController {

    private final ImageResourceService imageResourceService;

    public ImageResourceController(ImageResourceService imageResourceService) {
        this.imageResourceService = imageResourceService;
    }

    @PostMapping("/upload")
    public Result<ImageResource> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "domain", required = false) String domain,
            @RequestParam(value = "product", required = false) String product,
            @RequestParam(value = "versionId", required = false) Long versionId,
            @RequestParam(value = "filename", required = false) String filename) {
        String username = imageResourceService.getCurrentUsername();
        return Result.success(imageResourceService.upload(file, category, domain, product, versionId, username, filename));
    }

    @GetMapping("/tree")
    public Result<List<ImageDirectoryNode>> getTree(
            @RequestParam(value = "versionId", required = false) Long versionId) {
        return Result.success(imageResourceService.getTree(versionId));
    }

    @GetMapping
    public Result<List<ImageResource>> findAll(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "domain", required = false) String domain,
            @RequestParam(value = "product", required = false) String product,
            @RequestParam(value = "versionId", required = false) Long versionId) {
        return Result.success(imageResourceService.findAll(category, domain, product, versionId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        imageResourceService.delete(id);
        return Result.success();
    }

    @PostMapping("/batch-delete")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        imageResourceService.batchDelete(ids);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<ImageResource> update(@PathVariable Long id, @RequestBody ImageResource body) {
        return Result.success(imageResourceService.update(id, body));
    }

    @PostMapping("/migrate-external-images")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, String>> migrateExternalImages(@RequestBody List<Long> entryIds) {
        String taskId = imageResourceService.startMigration(entryIds);
        return Result.success(Map.of("taskId", taskId));
    }

    @GetMapping("/migrate-task/{taskId}")
    public Result<MigrationTaskProgress> getMigrationProgress(@PathVariable String taskId) {
        return Result.success(imageResourceService.getMigrationProgress(taskId));
    }

    @GetMapping("/{id}/references")
    public Result<List<DataEntry>> findReferences(@PathVariable Long id) {
        return Result.success(imageResourceService.findReferences(id));
    }
}