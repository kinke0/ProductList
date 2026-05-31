package com.superpower.modules.approval.repository;

import com.superpower.modules.approval.entity.ApprovalLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ApprovalLogRepository extends JpaRepository<ApprovalLog, Long> {
    List<ApprovalLog> findByEntryIdOrderByCreatedAtDesc(Long entryId);

    Optional<ApprovalLog> findTopByEntryIdAndActionOrderByCreatedAtDesc(Long entryId, String action);
}
