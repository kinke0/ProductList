package com.superpower.modules.document.repository;

import com.superpower.modules.document.entity.DocGenRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocGenRecordRepository extends JpaRepository<DocGenRecord, Long> {
    List<DocGenRecord> findByVersionIdOrderByCreatedAtDesc(Long versionId);
}
