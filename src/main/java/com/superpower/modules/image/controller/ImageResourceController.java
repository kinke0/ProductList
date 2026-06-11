package com.superpower.modules.image.controller;

import com.superpower.common.AuthUtils;
import com.superpower.common.Result;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.image.dto.ImageDirectoryNode;
import com.superpower.modules.image.dto.MigrationResult;
import com.superpower.modules.image.dto.MigrationTaskProgress;
import com.superpower.modules.image.entity.ImageResource;
import com.superpower.modules.image.repository.ImageResourceRepository;
import com.superpower.modules.image.service.ImageResourceService;
import com.superpower.modules.requirement.entity.ReqItem;
import com.superpower.modules.system.service.OperationLogService;
import com.superpower.modules.system.service.SysUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageResourceController {

    private final ImageResourceService imageResourceService;
    private final ImageResourceRepository imageResourceRepository;
    private final OperationLogService logService;
    private final SysUserService sysUserService;

    public ImageResourceController(ImageResourceService imageResourceService, ImageResourceRepository imageResourceRepository,
                                   OperationLogService logService, SysUserService sysUserService) {
        this.imageResourceService = imageResourceService;
        this.imageResourceRepository = imageResourceRepository;
        this.logService = logService;
        this.sysUserService = sysUserService;
    }

    private String imgTitle(ImageResource img) {
        return img.getFilename() != null && !img.getFilename().isBlank()
                ? img.getFilename() : "#" + img.getId();
    }

    private String imgPath(ImageResource img) {
        StringBuilder sb = new StringBuilder();
        if (img.getCategory() != null) sb.append(img.getCategory());
        if (img.getDomain() != null) sb.append("/").append(img.getDomain());
        return sb.length() > 0 ? " (" + sb + ")" : "";
    }

    @PostMapping("/upload")
    public Result<ImageResource> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "domain", required = false) String domain,
            @RequestParam(value = "product", required = false) String product,
            @RequestParam(value = "versionId", required = false) Long versionId,
            @RequestParam(value = "filename", required = false) String filename, Authentication auth) {
        String username = imageResourceService.getCurrentUsername();
        ImageResource img = imageResourceService.upload(file, category, domain, product, versionId, username, filename);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "UPLOAD", "图床管理", "上传图片: " + imgTitle(img) + imgPath(img), img.getId(), "Image");
        return Result.success(img);
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
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "versionId", required = false) Long versionId,
            @RequestParam(value = "includeReferenced", defaultValue = "true") boolean includeReferenced) {
        return Result.success(imageResourceService.findAll(category, domain, product, productId, versionId, includeReferenced));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id, Authentication auth) {
        ImageResource img = imageResourceRepository.findById(id).orElse(null);
        String title = img != null ? imgTitle(img) + imgPath(img) : "#" + id;
        imageResourceService.delete(id);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "DELETE", "图床管理", "删除图片: " + title, id, "Image");
        return Result.success();
    }

    @PostMapping("/batch-delete")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> batchDelete(@RequestBody List<Long> ids, Authentication auth) {
        imageResourceService.batchDelete(ids);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "DELETE", "图床管理", "批量删除 " + ids.size() + " 张图片");
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<ImageResource> update(@PathVariable Long id, @RequestBody ImageResource body, Authentication auth) {
        ImageResource img = imageResourceService.update(id, body);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "UPDATE", "图床管理", "重命名图片: " + imgTitle(img) + imgPath(img), id, "Image");
        return Result.success(img);
    }

    @PutMapping("/{id}/file")
    public Result<ImageResource> replaceFile(@PathVariable Long id,
                                             @RequestParam("file") MultipartFile file, Authentication auth) {
        ImageResource img = imageResourceService.replaceFile(id, file);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "UPDATE", "图床管理", "替换图片文件: " + imgTitle(img) + imgPath(img), id, "Image");
        return Result.success(img);
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

    @GetMapping("/{id}/all-references")
    public Result<List<Map<String, Object>>> findAllVersionReferences(@PathVariable Long id) {
        return Result.success(imageResourceService.findAllVersionReferences(id));
    }

    @PostMapping("/batch-references")
    public Result<Map<Long, List<DataEntry>>> findReferencesBatch(@RequestBody List<Long> ids) {
        return Result.success(imageResourceService.findReferencesBatch(ids));
    }

    @GetMapping("/{id}/req-references")
    public Result<List<ReqItem>> findReqReferences(@PathVariable Long id) {
        return Result.success(imageResourceService.findReqReferences(id));
    }
}