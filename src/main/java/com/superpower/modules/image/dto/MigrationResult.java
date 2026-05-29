package com.superpower.modules.image.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class MigrationResult {
    private int successImages;
    private int failedImages;
    private List<EntryFailDetail> failures = new ArrayList<>();

    @Data
    public static class EntryFailDetail {
        private Long entryId;
        private String productName;
        private int failedImageCount;
        private int totalImageCount;
    }
}
