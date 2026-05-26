package com.superpower.modules.category.repository;

import com.superpower.modules.category.entity.BaseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaseCategoryRepository extends JpaRepository<BaseCategory, Long> {
    List<BaseCategory> findByVersionIdOrderBySortOrderAsc(Long versionId);
}
