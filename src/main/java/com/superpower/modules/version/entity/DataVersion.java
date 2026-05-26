package com.superpower.modules.version.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "data_version")
public class DataVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_no", nullable = false, length = 20)
    private String versionNo;

    @Column(nullable = false, length = 20)
    private String status = "draft";

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "released_by")
    private Long releasedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
