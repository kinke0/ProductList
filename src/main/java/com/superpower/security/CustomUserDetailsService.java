package com.superpower.security;

import com.superpower.modules.system.entity.SysUser;
import com.superpower.modules.system.repository.SysUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserRepository userRepository;

    public CustomUserDetailsService(SysUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser sysUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        String roleCode = sysUser.getRole() != null ? sysUser.getRole().getCode() : "ROLE_USER";

        return User.builder()
                .username(sysUser.getUsername())
                .password(sysUser.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + roleCode)))
                .disabled(sysUser.getStatus() != 1)
                .build();
    }
}
