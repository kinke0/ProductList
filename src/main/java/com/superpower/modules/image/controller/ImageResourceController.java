package com.superpower.modules.image.controller;

import com.superpower.common.Result;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.image.dto.ImageDirectoryNode;
import com.superpower.modules.image.entity.ImageResource;
import com.superpower.modules.image.service.ImageResourceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
            @RequestParam(value = "versionId", required = false) Long versionId) {
        String username = imageResourceService.getCurrentUsername();
        return Result.success(imageResourceService.upload(file, category, domain, product, versionId, username));
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

    @GetMapping("/{id}/references")
    public Result<List<DataEntry>> findReferences(@PathVariable Long id) {
        return Result.success(imageResourceService.findReferences(id));
    }
}