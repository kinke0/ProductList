package com.superpower.modules.customtab.repository;

import com.superpower.modules.customtab.entity.CustomTab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomTabRepository extends JpaRepository<CustomTab, Long> {
    List<CustomTab> findByVersionIdOrderByCreatedAtAsc(Long versionId);
    boolean existsByVersionIdAndName(Long versionId, String name);
}
