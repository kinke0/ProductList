package com.superpower.modules.system.repository;

import com.superpower.modules.system.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    List<OperationLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<OperationLog> findAllByOrderByCreatedAtDesc();

    @Query("SELECT o FROM OperationLog o WHERE o.userId = :userId ORDER BY o.createdAt DESC LIMIT :limit")
    List<OperationLog> findRecentByUserId(Long userId, int limit);

    @Query("SELECT o FROM OperationLog o ORDER BY o.createdAt DESC LIMIT :limit")
    List<OperationLog> findRecent(int limit);
}
