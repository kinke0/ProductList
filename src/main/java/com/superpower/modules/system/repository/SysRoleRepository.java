package com.superpower.modules.system.repository;

import com.superpower.modules.system.entity.SysRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SysRoleRepository extends JpaRepository<SysRole, Long> {
    Optional<SysRole> findByCode(String code);
}
