package com.superpower.modules.version.controller;

import com.superpower.common.Result;
import com.superpower.modules.system.entity.SysUser;
import com.superpower.modules.system.repository.SysUserRepository;
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

    public DataVersionController(DataVersionService versionService, SysUserRepository sysUserRepository) {
        this.versionService = versionService;
        this.sysUserRepository = sysUserRepository;
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
    public Result<DataVersion> createVersion() {
        return Result.success(versionService.createVersion());
    }

    @PostMapping("/{id}/release")
    public Result<DataVersion> releaseVersion(@PathVariable Long id, Authentication auth) {
        String username = auth.getName();
        SysUser user = sysUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return Result.success(versionService.releaseVersion(id, user.getId()));
    }

    @PostMapping("/{id}/rollback")
    public Result<DataVersion> rollbackVersion(@PathVariable Long id) {
        return Result.success(versionService.rollbackVersion(id));
    }
}
