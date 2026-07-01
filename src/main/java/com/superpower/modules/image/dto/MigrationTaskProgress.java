package com.superpower.modules.image.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class MigrationTaskProgress {
    private String taskId;
    private String status;
    private int totalEntries;
    private int processedEntries;
    private int successImages;
    private int failedImages;
    private String currentEntry;
    private List<MigrationResult.EntryFailDetail> failures = new ArrayList<>();
}
