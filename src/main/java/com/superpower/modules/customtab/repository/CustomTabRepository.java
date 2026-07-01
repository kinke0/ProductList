package com.superpower.modules.customtab.repository;

import com.superpower.modules.customtab.entity.CustomTab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomTabRepository extends JpaRepository<CustomTab, Long> {
    List<CustomTab> findByVersionIdOrderByCreatedAtAsc(Long versionId);
    boolean existsByVersionIdAndName(Long versionId, String name);
    long countByVersionId(Long versionId);

    @Modifying
    @Query("DELETE FROM CustomTab e WHERE e.versionId = :versionId")
    void deleteByVersionId(@Param("versionId") Long versionId);
}
