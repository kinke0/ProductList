package com.superpower.modules.requirement.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "req_item")
public class ReqItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "req_no", nullable = false, unique = true, length = 20)
    private String reqNo;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String status = "提出";

    @Column(length = 10)
    private String priority = "中";

    @Column(length = 100)
    private String category;

    @Column(length = 100)
    private String domain;

    @Column(length = 50)
    private String type;

    @Column(name = "created_by")
    private Long createdBy;

    @Transient
    private String creatorName;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "released_version", length = 20)
    private String releasedVersion;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
