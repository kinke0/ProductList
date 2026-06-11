package com.superpower.modules.system.controller;

import com.superpower.common.AuthUtils;
import com.superpower.common.BusinessException;
import com.superpower.common.Result;
import com.superpower.modules.system.dto.ImageMigrationStatus;
import com.superpower.modules.system.dto.SqlExecutionResult;
import com.superpower.modules.system.service.MaintenanceService;
import com.superpower.modules.system.service.OperationLogService;
import com.superpower.modules.system.service.SysUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenance")
@PreAuthorize("hasRole('ADMIN')")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final OperationLogService logService;
    private final SysUserService sysUserService;

    public MaintenanceController(MaintenanceService maintenanceService, OperationLogService logService, SysUserService sysUserService) {
        this.maintenanceService = maintenanceService;
        this.logService = logService;
        this.sysUserService = sysUserService;
    }

    @PostMapping("/migrate-image")
    public Result<String> migrateImageAll(Authentication auth) {
        maintenanceService.checkCanMigrate();
        maintenanceService.executeMigrationAll();
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "EXECUTE", "非常规操作", "图片版本隔离迁移-全部");
        return Result.success("迁移任务已启动");
    }

    @PostMapping("/migrate-step/{step}")
    public Result<String> migrateStep(@PathVariable int step, Authentication auth) {
        if (step < 1 || step > 5) {
            throw new BusinessException("步骤编号必须为1-5");
        }
        maintenanceService.checkCanMigrate();
        maintenanceService.executeMigrationStep(step);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "EXECUTE", "非常规操作", "图片版本隔离迁移-步骤" + step);
        return Result.success("步骤" + step + "已启动");
    }

    @GetMapping("/migration-status")
    public Result<ImageMigrationStatus> getMigrationStatus() {
        return Result.success(maintenanceService.getMigrationStatus());
    }

    @PostMapping("/migration-reset")
    public Result<Void> resetMigration(Authentication auth) {
        maintenanceService.resetMigration();
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "EXECUTE", "非常规操作", "重置图片迁移状态");
        return Result.success();
    }

    @PostMapping("/sync-filenames")
    public Result<String> syncFilenames(Authentication auth) {
        maintenanceService.checkFilenameSync();
        maintenanceService.executeFilenameSync();
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "EXECUTE", "非常规操作", "文件名同步");
        return Result.success("文件名同步任务已启动");
    }

    @GetMapping("/sync-filenames-status")
    public Result<ImageMigrationStatus> getFilenameSyncStatus() {
        return Result.success(maintenanceService.getFilenameSyncStatus());
    }

    @PostMapping("/sync-filenames-reset")
    public Result<Void> resetFilenameSync(Authentication auth) {
        maintenanceService.resetFilenameSync();
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "EXECUTE", "非常规操作", "重置文件名同步状态");
        return Result.success();
    }

    @PostMapping("/fix-image-card-ids")
    public Result<String> fixImageCardIds(Authentication auth) {
        maintenanceService.checkFixId();
        maintenanceService.executeFixImageCardIds();
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "EXECUTE", "非常规操作", "修复图片卡片ID");
        return Result.success("修复任务已启动");
    }

    @GetMapping("/fix-image-card-ids-status")
    public Result<ImageMigrationStatus> getFixIdStatus() {
        return Result.success(maintenanceService.getFixIdStatus());
    }

    @PostMapping("/fix-image-card-ids-reset")
    public Result<Void> resetFixId(Authentication auth) {
        maintenanceService.resetFixId();
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "EXECUTE", "非常规操作", "重置图片卡片ID修复状态");
        return Result.success();
    }

    @PostMapping("/execute-sql")
    public Result<List<SqlExecutionResult>> executeSql(@RequestBody Map<String, String> body, Authentication auth) {
        String sql = body.get("sql");
        if (sql == null || sql.isBlank()) {
            throw new BusinessException("SQL语句不能为空");
        }
        List<SqlExecutionResult> results = maintenanceService.executeSql(sql);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "EXECUTE", "非常规操作", "执行SQL脚本");
        return Result.success(results);
    }
}
