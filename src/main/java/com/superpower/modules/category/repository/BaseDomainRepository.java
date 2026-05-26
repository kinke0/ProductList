package com.superpower.modules.category.repository;

import com.superpower.modules.category.entity.BaseDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaseDomainRepository extends JpaRepository<BaseDomain, Long> {
    List<BaseDomain> findByVersionIdAndCategoryIdOrderBySortOrderAsc(Long versionId, Long categoryId);
}
