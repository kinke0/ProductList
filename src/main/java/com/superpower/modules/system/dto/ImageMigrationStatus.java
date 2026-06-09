package com.superpower.modules.system.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ImageMigrationStatus {
    private String status;
    private int currentStep;
    private int totalSteps = 5;
    private int processedCount;
    private int totalCount;
    private List<StepResult> steps = new ArrayList<>();
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public void initSteps() {
        steps.clear();
        steps.add(new StepResult(1, "备份数据库", "PENDING"));
        steps.add(new StepResult(2, "复制图片到版本目录", "PENDING"));
        steps.add(new StepResult(3, "更新数据库URL引用", "PENDING"));
        steps.add(new StepResult(4, "去重清理重复记录", "PENDING"));
        steps.add(new StepResult(5, "清理旧文件", "PENDING"));
    }

    @Data
    public static class StepResult {
        private int step;
        private String name;
        private String status;
        private int successCount;
        private int failCount;
        private int skipCount;
        private String message;
        private long durationMs;

        public StepResult() {}

        public StepResult(int step, String name, String status) {
            this.step = step;
            this.name = name;
            this.status = status;
        }
    }
}
