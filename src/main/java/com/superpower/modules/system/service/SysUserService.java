package com.superpower.modules.system.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.system.dto.UserDTO;
import com.superpower.modules.system.entity.SysRole;
import com.superpower.modules.system.entity.SysUser;
import com.superpower.modules.system.repository.SysRoleRepository;
import com.superpower.modules.system.repository.SysUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysUserService {

    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public SysUserService(SysUserRepository userRepository,
                          SysRoleRepository roleRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public SysUser findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    public List<SysUser> findAll() {
        return userRepository.findAll();
    }

    public UserDTO createUser(String username, String password, String nickname, Long roleId) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }
        SysRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("角色不存在"));

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setRole(role);
        user.setStatus(1);
        user = userRepository.save(user);

        return toDTO(user);
    }

    public void updateUser(Long id, String nickname, Long roleId, Integer status) {
        SysUser user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (nickname != null) user.setNickname(nickname);
        if (roleId != null) {
            SysRole role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new BusinessException("角色不存在"));
            user.setRole(role);
        }
        if (status != null) user.setStatus(status);
        userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        SysUser user = findByUsername(username);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("当前密码不正确");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void updateNickname(String username, String nickname) {
        SysUser user = findByUsername(username);
        user.setNickname(nickname);
        userRepository.save(user);
    }

    private UserDTO toDTO(SysUser user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setRoleId(user.getRole().getId());
        dto.setRoleName(user.getRole().getName());
        dto.setRoleCode(user.getRole().getCode());
        dto.setStatus(user.getStatus());
        return dto;
    }
}
