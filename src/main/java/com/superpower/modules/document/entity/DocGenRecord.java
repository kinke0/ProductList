package com.superpower.modules.document.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "doc_gen_record")
public class DocGenRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(name = "doc_type", length = 20)
    private String docType;

    @Column(length = 20)
    private String format;

    @Column(length = 20)
    private String status;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "generated_by")
    private Long generatedBy;

    @Column(name = "generated_by_name", length = 100)
    private String generatedByName;

    @Column(name = "entry_ids", length = 2000)
    private String entryIds;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "total_entries")
    private Integer totalEntries;

    @Column(name = "processed_entries")
    private Integer processedEntries;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
