package com.superpower.modules.system.controller;

import com.superpower.common.Result;
import com.superpower.modules.system.entity.SysRole;
import com.superpower.modules.system.service.SysRoleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class SysRoleController {

    private final SysRoleService roleService;

    public SysRoleController(SysRoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public Result<List<SysRole>> getAllRoles() {
        return Result.success(roleService.findAll());
    }

    @PostMapping
    public Result<SysRole> createRole(@RequestBody SysRole role) {
        return Result.success(roleService.createRole(role.getName(), role.getCode(), role.getDescription()));
    }
}
