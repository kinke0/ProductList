package com.superpower.modules.option.repository;

import com.superpower.modules.option.entity.DataOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DataOptionRepository extends JpaRepository<DataOption, Long> {
    List<DataOption> findByTypeOrderBySortOrder(String type);
}
