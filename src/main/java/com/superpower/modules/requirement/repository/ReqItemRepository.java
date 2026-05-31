package com.superpower.modules.requirement.repository;

import com.superpower.modules.requirement.entity.ReqItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReqItemRepository extends JpaRepository<ReqItem, Long> {

    List<ReqItem> findByCreatedByOrderByCreatedAtDesc(Long createdBy);

    List<ReqItem> findAllByOrderByCreatedAtDesc();

    @Query("SELECT r FROM ReqItem r WHERE " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:createdBy IS NULL OR r.createdBy = :createdBy) AND " +
            "(:startDate IS NULL OR r.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR r.createdAt < :endDate) AND " +
            "(:category IS NULL OR r.category = :category) AND " +
            "(:domain IS NULL OR r.domain = :domain) AND " +
            "(:type IS NULL OR r.type = :type) AND " +
            "(:priority IS NULL OR r.priority = :priority) " +
            "ORDER BY r.createdAt DESC")
    List<ReqItem> findByFilters(@Param("status") String status,
                                @Param("createdBy") Long createdBy,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate,
                                @Param("category") String category,
                                @Param("domain") String domain,
                                @Param("type") String type,
                                @Param("priority") String priority);

    @Query("SELECT COUNT(r) FROM ReqItem r WHERE r.reqNo LIKE :prefix")
    long countByReqNoPrefix(@Param("prefix") String prefix);

    List<ReqItem> findAllByStatus(String status);
}
