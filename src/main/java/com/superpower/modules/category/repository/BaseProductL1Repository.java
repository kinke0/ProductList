package com.superpower.modules.category.repository;

import com.superpower.modules.category.entity.BaseProductL1;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaseProductL1Repository extends JpaRepository<BaseProductL1, Long> {
    List<BaseProductL1> findByVersionIdOrderBySortOrderAsc(Long versionId);
    long countByVersionId(Long versionId);

    @Modifying
    @Query("DELETE FROM BaseProductL1 e WHERE e.versionId = :versionId")
    void deleteByVersionId(@Param("versionId") Long versionId);
}
