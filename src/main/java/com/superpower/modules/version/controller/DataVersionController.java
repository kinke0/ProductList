package com.superpower.modules.version.controller;

import com.superpower.common.AuthUtils;
import com.superpower.common.Result;
import com.superpower.modules.system.entity.SysUser;
import com.superpower.modules.system.repository.SysUserRepository;
import com.superpower.modules.system.service.OperationLogService;
import com.superpower.modules.system.service.SysUserService;
import com.superpower.modules.version.entity.DataVersion;
import com.superpower.modules.version.service.DataVersionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/versions")
public class DataVersionController {

    private final DataVersionService versionService;
    private final SysUserRepository sysUserRepository;
    private final OperationLogService logService;
    private final SysUserService sysUserService;

    public DataVersionController(DataVersionService versionService, SysUserRepository sysUserRepository,
                                 OperationLogService logService, SysUserService sysUserService) {
        this.versionService = versionService;
        this.sysUserRepository = sysUserRepository;
        this.logService = logService;
        this.sysUserService = sysUserService;
    }

    @GetMapping
    public Result<List<Map<String, Object>>> getAllVersions() {
        List<DataVersion> versions = versionService.findAll();
        List<Map<String, Object>> result = versions.stream().map(v -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", v.getId());
            map.put("versionNo", v.getVersionNo());
            map.put("status", v.getStatus());
            map.put("releasedAt", v.getReleasedAt());
            map.put("releasedBy", v.getReleasedBy());
            map.put("rollbackCount", v.getRollbackCount());
            map.put("createdAt", v.getCreatedAt());
            map.put("updatedAt", v.getUpdatedAt());
            if (v.getReleasedBy() != null) {
                map.put("releasedByName", sysUserRepository.findById(v.getReleasedBy())
                        .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                        .orElse("未知"));
            } else {
                map.put("releasedByName", null);
            }
            return map;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    @GetMapping("/released")
    public Result<List<DataVersion>> getReleasedVersions() {
        return Result.success(versionService.findAllReleased());
    }

    @PostMapping
    public Result<DataVersion> createVersion(Authentication auth) {
        DataVersion v = versionService.createVersion();
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "CREATE", "版本管理", "创建清单版本 " + v.getVersionNo(), v.getId(), "DataVersion");
        return Result.success(v);
    }

    @PostMapping("/{id}/release")
    public Result<DataVersion> releaseVersion(@PathVariable Long id, Authentication auth) {
        String username = auth.getName();
        SysUser user = sysUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        DataVersion v = versionService.releaseVersion(id, user.getId());
        logService.record(user.getId(), username,
                "RELEASE", "版本管理", "发布清单版本 " + v.getVersionNo(), id, "DataVersion");
        return Result.success(v);
    }

    @PostMapping("/{id}/rollback")
    public Result<DataVersion> rollbackVersion(@PathVariable Long id, Authentication auth) {
        DataVersion v = versionService.rollbackVersion(id);
        logService.record(AuthUtils.getUserId(auth, sysUserService), AuthUtils.getUsername(auth),
                "ROLLBACK", "版本管理", "回滚清单版本 " + v.getVersionNo(), id, "DataVersion");
        return Result.success(v);
    }
}
