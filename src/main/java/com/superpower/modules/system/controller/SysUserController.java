package com.superpower.modules.system.controller;

import com.superpower.common.Result;
import com.superpower.modules.system.dto.UserDTO;
import com.superpower.modules.system.entity.SysUser;
import com.superpower.modules.system.service.OperationLogService;
import com.superpower.modules.system.service.SysUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class SysUserController {

    private final SysUserService userService;
    private final OperationLogService logService;

    public SysUserController(SysUserService userService, OperationLogService logService) {
        this.userService = userService;
        this.logService = logService;
    }

    @GetMapping
    public Result<List<UserDTO>> getAllUsers() {
        return Result.success(userService.findAll().stream()
                .map(userService::toDTO).toList());
    }

    @PostMapping
    public Result<UserDTO> createUser(@RequestBody UserDTO dto, Authentication auth) {
        UserDTO created = userService.createUser(
                dto.getUsername(), dto.getUsername(), dto.getNickname(), dto.getRoleId());
        logService.record(getUserId(auth), auth.getName(), "CREATE", "用户管理",
                "创建用户 " + dto.getUsername(), created.getId(), "User");
        return Result.success(created);
    }

    @PutMapping("/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody UserDTO dto, Authentication auth) {
        userService.updateUser(id, dto.getNickname(), dto.getRoleId(), dto.getStatus());
        logService.record(getUserId(auth), auth.getName(), "UPDATE", "用户管理",
                "编辑用户: " + dto.getNickname(), id, "User");
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id, Authentication auth) {
        SysUser user = userService.findById(id);
        String name = user != null && user.getNickname() != null ? user.getNickname() : "#" + id;
        userService.deleteUser(id);
        logService.record(getUserId(auth), auth.getName(), "DELETE", "用户管理",
                "删除用户: " + name, id, "User");
        return Result.success();
    }

    private Long getUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            SysUser user = userService.findByUsername(ud.getUsername());
            return user != null ? user.getId() : null;
        }
        return null;
    }
}
