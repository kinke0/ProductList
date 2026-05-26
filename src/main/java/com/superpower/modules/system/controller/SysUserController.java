package com.superpower.modules.system.controller;

import com.superpower.common.Result;
import com.superpower.modules.system.dto.UserDTO;
import com.superpower.modules.system.service.SysUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class SysUserController {

    private final SysUserService userService;

    public SysUserController(SysUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result<List<UserDTO>> getAllUsers() {
        return Result.success(userService.findAll().stream()
                .map(user -> {
                    UserDTO dto = new UserDTO();
                    dto.setId(user.getId());
                    dto.setUsername(user.getUsername());
                    dto.setNickname(user.getNickname());
                    dto.setRoleId(user.getRole().getId());
                    dto.setRoleName(user.getRole().getName());
                    dto.setRoleCode(user.getRole().getCode());
                    dto.setStatus(user.getStatus());
                    return dto;
                }).toList());
    }

    @PostMapping
    public Result<UserDTO> createUser(@RequestBody UserDTO dto) {
        return Result.success(userService.createUser(
                dto.getUsername(), dto.getUsername(), dto.getNickname(), dto.getRoleId()));
    }

    @PutMapping("/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody UserDTO dto) {
        userService.updateUser(id, dto.getNickname(), dto.getRoleId(), dto.getStatus());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }
}
