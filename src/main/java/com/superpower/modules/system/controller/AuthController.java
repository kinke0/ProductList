package com.superpower.modules.system.controller;

import com.superpower.common.BusinessException;
import com.superpower.common.Result;
import com.superpower.modules.system.dto.LoginRequest;
import com.superpower.modules.system.dto.LoginResponse;
import com.superpower.modules.system.entity.SysRole;
import com.superpower.modules.system.entity.SysUser;
import com.superpower.modules.system.service.OperationLogService;
import com.superpower.modules.system.service.SysUserService;
import com.superpower.security.JwtTokenProvider;
import com.superpower.security.OnlineTracker;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserService userService;
    private final OnlineTracker onlineTracker;
    private final OperationLogService logService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          SysUserService userService,
                          OnlineTracker onlineTracker,
                          OperationLogService logService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
        this.onlineTracker = onlineTracker;
        this.logService = logService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        SysUser user = userService.findByUsername(request.getUsername());
        SysRole role = user.getRole();
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getId(), role.getCode());

        user.setLastLoginAt(LocalDateTime.now());
        userService.save(user);
        onlineTracker.online(user.getId());
        logService.record(user.getId(), user.getUsername(), "LOGIN", "系统认证", "用户登录");

        return Result.success(new LoginResponse(token, user.getUsername(), user.getId(), user.getNickname(),
                role.getCode(), role.getName()));
    }

    @GetMapping("/me")
    public Result<?> getCurrentUser(Authentication authentication) {
        SysUser user = userService.findByUsername(authentication.getName());
        return Result.success(user);
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String nickname = body.get("nickname");
        if (username == null || username.isBlank()) throw new BusinessException("用户名不能为空");
        if (password == null || password.length() < 6) throw new BusinessException("密码至少6个字符");
        if (nickname == null || nickname.isBlank()) throw new BusinessException("姓名不能为空");
        userService.createUser(username, password, nickname, 2L);
        return Result.success();
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body, Authentication authentication) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || newPassword == null || newPassword.length() < 6)
            throw new BusinessException("密码不能为空且新密码至少6个字符");
        userService.changePassword(authentication.getName(), oldPassword, newPassword);
        return Result.success();
    }

    @PutMapping("/nickname")
    public Result<Void> changeNickname(@RequestBody Map<String, String> body, Authentication authentication) {
        String nickname = body.get("nickname");
        if (nickname == null || nickname.isBlank()) throw new BusinessException("姓名不能为空");
        userService.updateNickname(authentication.getName(), nickname);
        return Result.success();
    }
}
