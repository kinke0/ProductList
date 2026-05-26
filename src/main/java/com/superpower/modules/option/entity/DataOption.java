package com.superpower.modules.option.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_option")
public class DataOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 200)
    private String value;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "version_id")
    private Long versionId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
