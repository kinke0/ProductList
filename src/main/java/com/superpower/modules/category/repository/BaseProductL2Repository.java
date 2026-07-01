package com.superpower.modules.category.repository;

import com.superpower.modules.category.entity.BaseProductL2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaseProductL2Repository extends JpaRepository<BaseProductL2, Long> {
    List<BaseProductL2> findByVersionIdAndL1IdOrderBySortOrderAsc(Long versionId, Long l1Id);
    List<BaseProductL2> findByVersionId(Long versionId);
    long countByVersionId(Long versionId);

    @Modifying
    @Query("DELETE FROM BaseProductL2 e WHERE e.versionId = :versionId")
    void deleteByVersionId(@Param("versionId") Long versionId);
}
