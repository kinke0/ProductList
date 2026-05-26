package com.superpower.modules.system.service;

import com.superpower.modules.system.entity.SysRole;
import com.superpower.modules.system.repository.SysRoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysRoleService {

    private final SysRoleRepository roleRepository;

    public SysRoleService(SysRoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<SysRole> findAll() {
        return roleRepository.findAll();
    }

    public SysRole findByCode(String code) {
        return roleRepository.findByCode(code)
                .orElse(null);
    }

    public SysRole createRole(String name, String code, String description) {
        SysRole role = new SysRole();
        role.setName(name);
        role.setCode(code);
        role.setDescription(description);
        return roleRepository.save(role);
    }
}
