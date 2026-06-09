package com.superpower.modules.system.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import com.superpower.modules.image.entity.ImageResource;
import com.superpower.modules.image.repository.ImageResourceRepository;
import com.superpower.modules.requirement.entity.ReqItem;
import com.superpower.modules.requirement.repository.ReqItemRepository;
import com.superpower.modules.system.dto.ImageMigrationStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MaintenanceService {

    private final ImageResourceRepository imageResourceRepository;
    private final DataEntryRepository dataEntryRepository;
    private final ReqItemRepository reqItemRepository;

    @Value("${app.image-storage-path:./uploads/images}")
    private String storagePath;

    private ImageMigrationStatus migrationStatus;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MaintenanceService.class);

    public MaintenanceService(ImageResourceRepository imageResourceRepository,
                              DataEntryRepository dataEntryRepository,
                              ReqItemRepository reqItemRepository) {
        this.imageResourceRepository = imageResourceRepository;
        this.dataEntryRepository = dataEntryRepository;
        this.reqItemRepository = reqItemRepository;
    }

    public synchronized ImageMigrationStatus getMigrationStatus() {
        if (migrationStatus == null) {
            migrationStatus = new ImageMigrationStatus();
            migrationStatus.setStatus("NOT_STARTED");
            migrationStatus.initSteps();
        }
        return migrationStatus;
    }

    public void checkCanMigrate() {
        ImageMigrationStatus status = getMigrationStatus();
        if ("RUNNING".equals(status.getStatus())) {
            throw new BusinessException("迁移任务正在执行中，请等待完成");
        }
    }

    @Async
    public void executeMigrationAll() {
        executeMigrationSteps(null);
    }

    @Async
    public void executeMigrationStep(int step) {
        executeMigrationSteps(step);
    }

    private void executeMigrationSteps(Integer targetStep) {
        synchronized (this) {
            if (targetStep == null || migrationStatus == null || "NOT_STARTED".equals(migrationStatus.getStatus())) {
                migrationStatus = new ImageMigrationStatus();
                migrationStatus.initSteps();
            }
            migrationStatus.setStatus("RUNNING");
            if (migrationStatus.getStartedAt() == null) {
                migrationStatus.setStartedAt(LocalDateTime.now());
            }
        }

        try {
            int[] steps = targetStep != null ? new int[]{targetStep} : new int[]{1, 2, 3, 4, 5};
            for (int step : steps) {
                if (targetStep == null) {
                    for (int i = 1; i < step; i++) {
                        ImageMigrationStatus.StepResult sr = findStepResult(i);
                        if (!"COMPLETED".equals(sr.getStatus())) {
                            migrationStatus.setStatus("FAILED");
                            migrationStatus.setErrorMessage("步骤" + i + "未完成，无法执行步骤" + step);
                            migrationStatus.setCompletedAt(LocalDateTime.now());
                            return;
                        }
                    }
                }
                executeSingleStep(step);
                ImageMigrationStatus.StepResult result = findStepResult(step);
                if ("FAILED".equals(result.getStatus())) {
                    migrationStatus.setStatus("FAILED");
                    migrationStatus.setErrorMessage("步骤" + step + "失败: " + result.getMessage());
                    migrationStatus.setCompletedAt(LocalDateTime.now());
                    return;
                }
            }
            migrationStatus.setStatus("COMPLETED");
            migrationStatus.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("迁移执行异常", e);
            migrationStatus.setStatus("FAILED");
            migrationStatus.setErrorMessage(e.getMessage());
            migrationStatus.setCompletedAt(LocalDateTime.now());
        }
    }

    private void executeSingleStep(int step) {
        ImageMigrationStatus.StepResult sr = findStepResult(step);
        sr.setStatus("RUNNING");
        long start = System.currentTimeMillis();
        try {
            switch (step) {
                case 1 -> stepBackupDatabase(sr);
                case 2 -> stepCopyFiles(sr);
                case 3 -> stepUpdateDatabase(sr);
                case 4 -> stepDedup(sr);
                case 5 -> stepCleanupOldFiles(sr);
                default -> sr.setStatus("SKIPPED");
            }
        } catch (Exception e) {
            sr.setStatus("FAILED");
            sr.setMessage(e.getMessage());
            log.error("步骤{}执行失败", step, e);
        }
        sr.setDurationMs(System.currentTimeMillis() - start);
    }

    private ImageMigrationStatus.StepResult findStepResult(int step) {
        return migrationStatus.getSteps().stream()
                .filter(s -> s.getStep() == step)
                .findFirst()
                .orElseThrow();
    }

    private void updateProgress(int processed, int total) {
        migrationStatus.setProcessedCount(processed);
        migrationStatus.setTotalCount(total);
    }

    private void stepBackupDatabase(ImageMigrationStatus.StepResult sr) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        Path source = Paths.get("superpower.db");
        Path target = Paths.get("superpower.db.bak." + timestamp);

        if (!Files.exists(source)) {
            sr.setStatus("COMPLETED");
            sr.setMessage("数据库文件不存在（可能使用内嵌 Derby），跳过备份");
            sr.setSkipCount(1);
            return;
        }

        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            sr.setSuccessCount(1);
            sr.setMessage("备份到 " + target.getFileName());
            sr.setStatus("COMPLETED");
        } catch (IOException e) {
            throw new RuntimeException("数据库备份失败: " + e.getMessage());
        }
    }

    private void stepCopyFiles(ImageMigrationStatus.StepResult sr) {
        List<ImageResource> allImages = imageResourceRepository.findAll();
        int total = allImages.size();
        sr.setSkipCount(0);
        sr.setSuccessCount(0);
        sr.setFailCount(0);
        int processed = 0;
        updateProgress(0, total);

        for (ImageResource img : allImages) {
            processed++;
            updateProgress(processed, total);
            try {
                Path oldPath = Paths.get(img.getPath());
                if (!Files.exists(oldPath)) {
                    sr.setSkipCount(sr.getSkipCount() + 1);
                    continue;
                }

                String relativePath = extractRelativePath(img.getPath(), img.getUrl());
                if (relativePath == null) {
                    sr.setSkipCount(sr.getSkipCount() + 1);
                    continue;
                }

                Path newPath = Paths.get(storagePath, String.valueOf(img.getVersionId()), relativePath);
                if (Files.exists(newPath)) {
                    sr.setSkipCount(sr.getSkipCount() + 1);
                    continue;
                }

                Files.createDirectories(newPath.getParent());
                Files.copy(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
                sr.setSuccessCount(sr.getSuccessCount() + 1);
            } catch (Exception e) {
                sr.setFailCount(sr.getFailCount() + 1);
                log.warn("复制文件失败 id={}: {}", img.getId(), e.getMessage());
            }
        }
        sr.setStatus("COMPLETED");
        sr.setMessage("成功 " + sr.getSuccessCount() + "，跳过 " + sr.getSkipCount() + "，失败 " + sr.getFailCount());
    }

    private String extractRelativePath(String path, String url) {
        String urlPrefix = "/api/images/file/";
        if (url != null && url.startsWith(urlPrefix)) {
            String after = url.substring(urlPrefix.length());
            int slashIdx = after.indexOf('/');
            if (slashIdx > 0) {
                String possibleVid = after.substring(0, slashIdx);
                try {
                    Long.parseLong(possibleVid);
                    return after.substring(slashIdx + 1);
                } catch (NumberFormatException ignored) {}
            }
            return after;
        }
        String pathPrefix = storagePath;
        if (!pathPrefix.endsWith("/") && !pathPrefix.endsWith("\\")) {
            pathPrefix += "/";
        }
        String normalized = path.replace("\\", "/");
        String normalizedPrefix = pathPrefix.replace("\\", "/");
        if (normalized.startsWith(normalizedPrefix)) {
            String rel = normalized.substring(normalizedPrefix.length());
            int slashIdx = rel.indexOf('/');
            if (slashIdx > 0) {
                String possibleVid = rel.substring(0, slashIdx);
                try {
                    Long.parseLong(possibleVid);
                    return rel.substring(slashIdx + 1);
                } catch (NumberFormatException ignored) {}
            }
            return rel;
        }
        if (normalized.startsWith("./")) {
            String check = normalized.substring(2);
            if (check.startsWith("uploads/images/")) {
                String rel = check.substring("uploads/images/".length());
                int slashIdx = rel.indexOf('/');
                if (slashIdx > 0) {
                    String possibleVid = rel.substring(0, slashIdx);
                    try {
                        Long.parseLong(possibleVid);
                        return rel.substring(slashIdx + 1);
                    } catch (NumberFormatException ignored) {}
                }
                return rel;
            }
        }
        return null;
    }

    @Transactional
    public void stepUpdateDatabase(ImageMigrationStatus.StepResult sr) {
        List<ImageResource> allImages = imageResourceRepository.findAll();
        int total = allImages.size();
        sr.setSuccessCount(0);
        sr.setFailCount(0);
        sr.setSkipCount(0);
        int processed = 0;
        updateProgress(0, total);

        String urlPrefix = "/api/images/file/";
        String pathPrefix = storagePath.replace("\\", "/");
        if (!pathPrefix.endsWith("/")) pathPrefix += "/";

        for (ImageResource img : allImages) {
            processed++;
            updateProgress(processed, total);
            try {
                String expectedUrl = urlPrefix + img.getVersionId() + "/";
                if (img.getUrl() != null && img.getUrl().startsWith(expectedUrl)) {
                    sr.setSkipCount(sr.getSkipCount() + 1);
                    continue;
                }

                String relativePath = extractRelativePath(img.getPath(), img.getUrl());
                if (relativePath == null) {
                    sr.setFailCount(sr.getFailCount() + 1);
                    continue;
                }

                String newPath = pathPrefix + img.getVersionId() + "/" + relativePath;
                String newUrl = urlPrefix + img.getVersionId() + "/" + relativePath;

                img.setPath(newPath);
                img.setUrl(newUrl);
                imageResourceRepository.save(img);
                sr.setSuccessCount(sr.getSuccessCount() + 1);
            } catch (Exception e) {
                sr.setFailCount(sr.getFailCount() + 1);
                log.warn("更新image_resource失败 id={}: {}", img.getId(), e.getMessage());
            }
        }

        updateDataEntryUrls(sr, total);
        updateReqItemUrls(sr);

        sr.setStatus("COMPLETED");
        sr.setMessage("image_resource更新 " + sr.getSuccessCount() + "，失败 " + sr.getFailCount());
    }

    private void updateDataEntryUrls(ImageMigrationStatus.StepResult sr, int baseProgress) {
        List<DataEntry> entries = dataEntryRepository.findAll();
        int total = entries.size();
        int entryUpdated = 0;
        for (int i = 0; i < entries.size(); i++) {
            DataEntry entry = entries.get(i);
            updateProgress(baseProgress + i, baseProgress + total);
            boolean changed = false;
            String desc = entry.getColFeatureDesc();
            if (desc != null && desc.contains("/api/images/file/")) {
                String newDesc = replaceImageUrlByVersion(desc, entry.getVersionId());
                if (!desc.equals(newDesc)) {
                    entry.setColFeatureDesc(newDesc);
                    changed = true;
                }
            }
            String cp1 = entry.getColControlPointImg1();
            if (cp1 != null && cp1.contains("/api/images/file/")) {
                String newCp1 = replaceImageUrlByVersion(cp1, entry.getVersionId());
                if (!cp1.equals(newCp1)) {
                    entry.setColControlPointImg1(newCp1);
                    changed = true;
                }
            }
            String cp2 = entry.getColControlPointImg2();
            if (cp2 != null && cp2.contains("/api/images/file/")) {
                String newCp2 = replaceImageUrlByVersion(cp2, entry.getVersionId());
                if (!cp2.equals(newCp2)) {
                    entry.setColControlPointImg2(newCp2);
                    changed = true;
                }
            }
            String cp3 = entry.getColControlPointImg3();
            if (cp3 != null && cp3.contains("/api/images/file/")) {
                String newCp3 = replaceImageUrlByVersion(cp3, entry.getVersionId());
                if (!cp3.equals(newCp3)) {
                    entry.setColControlPointImg3(newCp3);
                    changed = true;
                }
            }
            String cpDoc = entry.getColControlPointDoc();
            if (cpDoc != null && cpDoc.contains("/api/images/file/")) {
                String newCpDoc = replaceImageUrlByVersion(cpDoc, entry.getVersionId());
                if (!cpDoc.equals(newCpDoc)) {
                    entry.setColControlPointDoc(newCpDoc);
                    changed = true;
                }
            }
            if (changed) {
                dataEntryRepository.save(entry);
                entryUpdated++;
            }
        }
        sr.setMessage(sr.getMessage() + "；data_entry更新 " + entryUpdated);
    }

    private void updateReqItemUrls(ImageMigrationStatus.StepResult sr) {
        sr.setMessage(sr.getMessage() + "；req_item无需更新（需求图片不纳入版本隔离）");
    }

    private String replaceImageUrlByVersion(String text, Long versionId) {
        if (text == null || !text.contains("/api/images/file/")) return text;
        String expectedPrefix = "/api/images/file/" + versionId + "/";
        if (text.contains(expectedPrefix)) return text;
        return text.replace("/api/images/file/", expectedPrefix);
    }

    @Transactional
    public void stepDedup(ImageMigrationStatus.StepResult sr) {
        List<ImageResource> allImages = imageResourceRepository.findAll();
        Map<String, List<ImageResource>> groups = new LinkedHashMap<>();
        for (ImageResource img : allImages) {
            String key = img.getVersionId() + "||" + img.getFilename() + "||"
                    + (img.getCategory() != null ? img.getCategory() : "") + "||"
                    + (img.getDomain() != null ? img.getDomain() : "") + "||"
                    + (img.getProduct() != null ? img.getProduct() : "");
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(img);
        }

        int deleted = 0;
        for (List<ImageResource> group : groups.values()) {
            if (group.size() <= 1) continue;
            group.sort(Comparator.comparingLong(ImageResource::getId));
            for (int i = 1; i < group.size(); i++) {
                imageResourceRepository.deleteById(group.get(i).getId());
                deleted++;
            }
        }
        sr.setSuccessCount(deleted);
        sr.setStatus("COMPLETED");
        sr.setMessage("删除重复记录 " + deleted + " 条");
    }

    private void stepCleanupOldFiles(ImageMigrationStatus.StepResult sr) {
        Path imageDir = Paths.get(storagePath);
        int deleted = 0;
        int skipped = 0;
        try {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(imageDir)) {
                for (Path entry : stream) {
                    if (!Files.isDirectory(entry)) continue;
                    String name = entry.getFileName().toString();
                    try {
                        Long.parseLong(name);
                        skipped++;
                    } catch (NumberFormatException e) {
                        deleteDirectoryRecursive(entry);
                        deleted++;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("清理旧文件失败: " + e.getMessage());
        }
        sr.setStatus("COMPLETED");
        sr.setMessage("删除旧目录 " + deleted + " 个，跳过版本目录 " + skipped + " 个");
    }

    private void deleteDirectoryRecursive(Path dir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    deleteDirectoryRecursive(entry);
                } else {
                    Files.deleteIfExists(entry);
                }
            }
        }
        Files.deleteIfExists(dir);
    }

    public synchronized void resetMigration() {
        migrationStatus = null;
    }

    private ImageMigrationStatus filenameSyncStatus;

    public synchronized ImageMigrationStatus getFilenameSyncStatus() {
        if (filenameSyncStatus == null) {
            filenameSyncStatus = new ImageMigrationStatus();
            filenameSyncStatus.setStatus("NOT_STARTED");
            filenameSyncStatus.setTotalSteps(1);
            filenameSyncStatus.getSteps().clear();
            filenameSyncStatus.getSteps().add(new ImageMigrationStatus.StepResult(1, "同步图片文件名", "PENDING"));
        }
        return filenameSyncStatus;
    }

    public void checkFilenameSync() {
        ImageMigrationStatus status = getFilenameSyncStatus();
        if ("RUNNING".equals(status.getStatus())) {
            throw new BusinessException("文件名同步任务正在执行中，请等待完成");
        }
    }

    @Async
    public void executeFilenameSync() {
        synchronized (this) {
            filenameSyncStatus = new ImageMigrationStatus();
            filenameSyncStatus.setStatus("RUNNING");
            filenameSyncStatus.setStartedAt(LocalDateTime.now());
            filenameSyncStatus.setTotalSteps(1);
            filenameSyncStatus.getSteps().clear();
            filenameSyncStatus.getSteps().add(new ImageMigrationStatus.StepResult(1, "同步图片文件名", "PENDING"));
        }
        ImageMigrationStatus.StepResult sr = filenameSyncStatus.getSteps().get(0);
        sr.setStatus("RUNNING");
        long start = System.currentTimeMillis();
        int successCount = 0, skipCount = 0, failCount = 0;

        try {
            List<ImageResource> allImages = imageResourceRepository.findAll();
            int total = allImages.size();
            filenameSyncStatus.setTotalCount(total);

            for (int i = 0; i < allImages.size(); i++) {
                ImageResource img = allImages.get(i);
                filenameSyncStatus.setProcessedCount(i + 1);

                try {
                    String oldStoredName = img.getStoredName();
                    if (oldStoredName == null || img.getFilename() == null) {
                        skipCount++;
                        continue;
                    }
                    String ext = "";
                    int dotIdx = oldStoredName.lastIndexOf('.');
                    if (dotIdx > 0) ext = oldStoredName.substring(dotIdx);
                    String expectedStoredName = img.getFilename().replaceAll("\\.[^.]+$", "").replaceAll("[\\\\/:*?\"<>|]", "_").trim() + ext;

                    if (expectedStoredName.equals(oldStoredName)) {
                        skipCount++;
                        continue;
                    }

                    Path oldPath = Paths.get(img.getPath());
                    if (!Files.exists(oldPath)) {
                        skipCount++;
                        continue;
                    }

                    Path newPath = oldPath.resolveSibling(expectedStoredName);
                    if (Files.exists(newPath) && !newPath.equals(oldPath)) {
                        skipCount++;
                        continue;
                    }

                    String oldUrl = img.getUrl();
                    Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
                    img.setStoredName(expectedStoredName);
                    img.setPath(newPath.toString());

                    String newUrl = oldUrl;
                    if (oldUrl != null && oldUrl.contains(oldStoredName)) {
                        newUrl = oldUrl.substring(0, oldUrl.lastIndexOf(oldStoredName)) + expectedStoredName;
                    }
                    img.setUrl(newUrl);
                    imageResourceRepository.save(img);

                    if (oldUrl != null && !oldUrl.equals(newUrl)) {
                        syncFileReferences(oldUrl, newUrl, img.getFilename());
                    }

                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.warn("同步文件名失败 id={}: {}", img.getId(), e.getMessage());
                }
            }

            sr.setSuccessCount(successCount);
            sr.setSkipCount(skipCount);
            sr.setFailCount(failCount);
            sr.setStatus("COMPLETED");
            sr.setMessage("成功 " + successCount + "，跳过 " + skipCount + "，失败 " + failCount);
            filenameSyncStatus.setStatus("COMPLETED");
        } catch (Exception e) {
            sr.setStatus("FAILED");
            sr.setMessage(e.getMessage());
            filenameSyncStatus.setStatus("FAILED");
            filenameSyncStatus.setErrorMessage(e.getMessage());
            log.error("文件名同步失败", e);
        }
        filenameSyncStatus.setCompletedAt(LocalDateTime.now());
        sr.setDurationMs(System.currentTimeMillis() - start);
    }

    private void syncFileReferences(String oldUrl, String newUrl, String displayName) {
        List<DataEntry> entries = dataEntryRepository.findAll();
        for (DataEntry e : entries) {
            boolean changed = false;
            String desc = e.getColFeatureDesc();
            if (desc != null && desc.contains(oldUrl)) {
                e.setColFeatureDesc(desc.replace(oldUrl, newUrl));
                changed = true;
            }
            String cp1 = e.getColControlPointImg1();
            if (cp1 != null && cp1.contains(oldUrl)) {
                e.setColControlPointImg1(cp1.replace(oldUrl, newUrl));
                changed = true;
            }
            String cp2 = e.getColControlPointImg2();
            if (cp2 != null && cp2.contains(oldUrl)) {
                e.setColControlPointImg2(cp2.replace(oldUrl, newUrl));
                changed = true;
            }
            String cp3 = e.getColControlPointImg3();
            if (cp3 != null && cp3.contains(oldUrl)) {
                e.setColControlPointImg3(cp3.replace(oldUrl, newUrl));
                changed = true;
            }
            String cpDoc = e.getColControlPointDoc();
            if (cpDoc != null && cpDoc.contains(oldUrl)) {
                e.setColControlPointDoc(cpDoc.replace(oldUrl, newUrl));
                changed = true;
            }
            if (changed) dataEntryRepository.save(e);
        }
    }

    public synchronized void resetFilenameSync() {
        filenameSyncStatus = null;
    }

    private ImageMigrationStatus fixIdStatus;

    public synchronized ImageMigrationStatus getFixIdStatus() {
        if (fixIdStatus == null) {
            fixIdStatus = new ImageMigrationStatus();
            fixIdStatus.setStatus("NOT_STARTED");
            fixIdStatus.setTotalSteps(1);
            fixIdStatus.getSteps().clear();
            fixIdStatus.getSteps().add(new ImageMigrationStatus.StepResult(1, "修复图片引用ID", "PENDING"));
        }
        return fixIdStatus;
    }

    public void checkFixId() {
        ImageMigrationStatus status = getFixIdStatus();
        if ("RUNNING".equals(status.getStatus())) {
            throw new BusinessException("修复任务正在执行中，请等待完成");
        }
    }

    @Async
    public void executeFixImageCardIds() {
        synchronized (this) {
            fixIdStatus = new ImageMigrationStatus();
            fixIdStatus.setStatus("RUNNING");
            fixIdStatus.setStartedAt(LocalDateTime.now());
            fixIdStatus.setTotalSteps(1);
            fixIdStatus.getSteps().clear();
            fixIdStatus.getSteps().add(new ImageMigrationStatus.StepResult(1, "修复图片引用ID", "PENDING"));
        }
        ImageMigrationStatus.StepResult sr = fixIdStatus.getSteps().get(0);
        sr.setStatus("RUNNING");
        long start = System.currentTimeMillis();

        try {
            Set<Long> existingIds = new HashSet<>();
            for (ImageResource img : imageResourceRepository.findAll()) {
                existingIds.add(img.getId());
            }

            Map<String, Long> storedNameToId = new HashMap<>();
            for (ImageResource img : imageResourceRepository.findAll()) {
                String key = img.getVersionId() + "|" + img.getStoredName();
                storedNameToId.put(key, img.getId());
            }

            List<DataEntry> allEntries = dataEntryRepository.findAll();
            int total = allEntries.size();
            fixIdStatus.setTotalCount(total);
            int fixed = 0, checked = 0;

            for (int i = 0; i < allEntries.size(); i++) {
                DataEntry entry = allEntries.get(i);
                fixIdStatus.setProcessedCount(i + 1);
                String desc = entry.getColFeatureDesc();
                if (desc == null || !desc.contains("data-id=")) continue;
                checked++;

                java.util.regex.Matcher m = java.util.regex.Pattern.compile("data-id=\"(\\d+)\"").matcher(desc);
                StringBuffer sb = new StringBuffer();
                boolean changed = false;
                while (m.find()) {
                    long oldId = Long.parseLong(m.group(1));
                    if (existingIds.contains(oldId)) {
                        m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(m.group()));
                        continue;
                    }
                    String urlPattern = "data-url=\"([^\"]+)\"";
                    java.util.regex.Matcher urlM = java.util.regex.Pattern.compile(
                            "data-url=\"([^\"]+)\"[^>]*data-id=\"" + oldId + "\"[^>]*>|data-id=\"" + oldId + "\"[^>]*data-url=\"([^\"]+)\""
                    ).matcher(desc);
                    String dataUrl = null;
                    if (urlM.find()) {
                        dataUrl = urlM.group(1) != null ? urlM.group(1) : urlM.group(2);
                    }
                    Long newId = null;
                    if (dataUrl != null) {
                        String storedName = extractStoredNameFromUrl(dataUrl);
                        if (storedName != null) {
                            String key = entry.getVersionId() + "|" + storedName;
                            newId = storedNameToId.get(key);
                        }
                    }
                    if (newId != null) {
                        m.appendReplacement(sb, "data-id=\"" + newId + "\"");
                        changed = true;
                        fixed++;
                    } else {
                        m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(m.group()));
                    }
                }
                m.appendTail(sb);
                if (changed) {
                    entry.setColFeatureDesc(sb.toString());
                    dataEntryRepository.save(entry);
                }
            }

            sr.setSuccessCount(fixed);
            sr.setSkipCount(checked - fixed);
            sr.setFailCount(0);
            sr.setStatus("COMPLETED");
            sr.setMessage("检查 " + checked + " 条，修复 " + fixed + " 个引用ID");
            fixIdStatus.setStatus("COMPLETED");
        } catch (Exception e) {
            sr.setStatus("FAILED");
            sr.setMessage(e.getMessage());
            fixIdStatus.setStatus("FAILED");
            fixIdStatus.setErrorMessage(e.getMessage());
            log.error("修复图片引用ID失败", e);
        }
        fixIdStatus.setCompletedAt(LocalDateTime.now());
        sr.setDurationMs(System.currentTimeMillis() - start);
    }

    private String extractStoredNameFromUrl(String url) {
        if (url == null) return null;
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash < 0) return null;
        String name = url.substring(lastSlash + 1);
        int hashIdx = name.indexOf('#');
        if (hashIdx > 0) name = name.substring(0, hashIdx);
        int queryIdx = name.indexOf('?');
        if (queryIdx > 0) name = name.substring(0, queryIdx);
        return name.isEmpty() ? null : java.net.URLDecoder.decode(name, java.nio.charset.StandardCharsets.UTF_8);
    }

    public synchronized void resetFixId() {
        fixIdStatus = null;
    }
}
