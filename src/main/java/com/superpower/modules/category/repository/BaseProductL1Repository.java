package com.superpower.modules.category.repository;

import com.superpower.modules.category.entity.BaseProductL1;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaseProductL1Repository extends JpaRepository<BaseProductL1, Long> {
    List<BaseProductL1> findByVersionIdOrderBySortOrderAsc(Long versionId);
}
