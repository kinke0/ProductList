package com.superpower.modules.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "operation_log")
public class OperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 50)
    private String username;

    @Column(length = 50)
    private String action;

    @Column(length = 50)
    private String module;

    @Column(length = 500)
    private String description;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(length = 50)
    private String ip;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
