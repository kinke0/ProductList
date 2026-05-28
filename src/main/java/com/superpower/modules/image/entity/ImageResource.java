package com.superpower.modules.image.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "image_resource")
public class ImageResource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "stored_name", nullable = false)
    private String storedName;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "category", length = 200)
    private String category;

    @Column(name = "domain", length = 200)
    private String domain;

    @Column(name = "product", length = 200)
    private String product;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "size")
    private Long size;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "version_id")
    private Long versionId;
}