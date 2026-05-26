package com.superpower.modules.system.controller;

import com.superpower.common.Result;
import com.superpower.modules.system.dto.LoginRequest;
import com.superpower.modules.system.dto.LoginResponse;
import com.superpower.modules.system.entity.SysRole;
import com.superpower.modules.system.entity.SysUser;
import com.superpower.modules.system.service.SysUserService;
import com.superpower.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserService userService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          SysUserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        SysUser user = userService.findByUsername(request.getUsername());
        SysRole role = user.getRole();
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getId(), role.getCode());

        return Result.success(new LoginResponse(token, user.getUsername(), user.getNickname(),
                role.getCode(), role.getName()));
    }

    @GetMapping("/me")
    public Result<?> getCurrentUser(Authentication authentication) {
        SysUser user = userService.findByUsername(authentication.getName());
        return Result.success(user);
    }
}
