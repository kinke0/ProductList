package com.superpower.modules.category.repository;

import com.superpower.modules.category.entity.BaseDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaseDomainRepository extends JpaRepository<BaseDomain, Long> {
    List<BaseDomain> findByVersionIdAndCategoryIdOrderBySortOrderAsc(Long versionId, Long categoryId);
    List<BaseDomain> findByVersionId(Long versionId);
    long countByVersionId(Long versionId);

    @Modifying
    @Query("DELETE FROM BaseDomain e WHERE e.versionId = :versionId")
    void deleteByVersionId(@Param("versionId") Long versionId);
}
