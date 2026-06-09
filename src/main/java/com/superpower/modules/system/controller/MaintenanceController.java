package com.superpower.modules.system.controller;

import com.superpower.common.BusinessException;
import com.superpower.common.Result;
import com.superpower.modules.system.dto.ImageMigrationStatus;
import com.superpower.modules.system.service.MaintenanceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/maintenance")
@PreAuthorize("hasRole('ADMIN')")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @PostMapping("/migrate-image")
    public Result<String> migrateImageAll() {
        maintenanceService.checkCanMigrate();
        maintenanceService.executeMigrationAll();
        return Result.success("迁移任务已启动");
    }

    @PostMapping("/migrate-step/{step}")
    public Result<String> migrateStep(@PathVariable int step) {
        if (step < 1 || step > 5) {
            throw new BusinessException("步骤编号必须为1-5");
        }
        maintenanceService.checkCanMigrate();
        maintenanceService.executeMigrationStep(step);
        return Result.success("步骤" + step + "已启动");
    }

    @GetMapping("/migration-status")
    public Result<ImageMigrationStatus> getMigrationStatus() {
        return Result.success(maintenanceService.getMigrationStatus());
    }

    @PostMapping("/migration-reset")
    public Result<Void> resetMigration() {
        maintenanceService.resetMigration();
        return Result.success();
    }

    @PostMapping("/sync-filenames")
    public Result<String> syncFilenames() {
        maintenanceService.checkFilenameSync();
        maintenanceService.executeFilenameSync();
        return Result.success("文件名同步任务已启动");
    }

    @GetMapping("/sync-filenames-status")
    public Result<ImageMigrationStatus> getFilenameSyncStatus() {
        return Result.success(maintenanceService.getFilenameSyncStatus());
    }

    @PostMapping("/sync-filenames-reset")
    public Result<Void> resetFilenameSync() {
        maintenanceService.resetFilenameSync();
        return Result.success();
    }

    @PostMapping("/fix-image-card-ids")
    public Result<String> fixImageCardIds() {
        maintenanceService.checkFixId();
        maintenanceService.executeFixImageCardIds();
        return Result.success("修复任务已启动");
    }

    @GetMapping("/fix-image-card-ids-status")
    public Result<ImageMigrationStatus> getFixIdStatus() {
        return Result.success(maintenanceService.getFixIdStatus());
    }

    @PostMapping("/fix-image-card-ids-reset")
    public Result<Void> resetFixId() {
        maintenanceService.resetFixId();
        return Result.success();
    }
}
