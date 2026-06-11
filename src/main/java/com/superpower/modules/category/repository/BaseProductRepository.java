package com.superpower.modules.category.repository;

import com.superpower.modules.category.entity.BaseProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaseProductRepository extends JpaRepository<BaseProduct, Long> {
    List<BaseProduct> findByVersionIdAndDomainIdOrderBySortOrderAsc(Long versionId, Long domainId);
    List<BaseProduct> findByVersionId(Long versionId);
    long countByVersionId(Long versionId);

    @Modifying
    @Query("DELETE FROM BaseProduct e WHERE e.versionId = :versionId")
    void deleteByVersionId(@Param("versionId") Long versionId);
}
