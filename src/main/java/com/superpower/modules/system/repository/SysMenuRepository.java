package com.superpower.modules.system.repository;

import com.superpower.modules.system.entity.SysMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SysMenuRepository extends JpaRepository<SysMenu, Long> {
    List<SysMenu> findByParentIdOrderBySortOrder(Long parentId);
}
