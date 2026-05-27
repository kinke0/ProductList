package com.superpower.modules.customtab.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Data
@Entity
@Table(name = "custom_tab_entry")
@IdClass(CustomTabEntryId.class)
public class CustomTabEntry implements Serializable {
    @Id
    @Column(name = "custom_tab_id", nullable = false)
    private Long customTabId;

    @Id
    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
