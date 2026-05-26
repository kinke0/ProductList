package com.superpower.modules.version.controller;

import com.superpower.common.Result;
import com.superpower.modules.version.entity.DataVersion;
import com.superpower.modules.version.service.DataVersionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/versions")
public class DataVersionController {

    private final DataVersionService versionService;

    public DataVersionController(DataVersionService versionService) {
        this.versionService = versionService;
    }

    @GetMapping
    public Result<List<DataVersion>> getAllVersions() {
        return Result.success(versionService.findAll());
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
        Long userId = 1L;
        return Result.success(versionService.releaseVersion(id, userId));
    }
}
