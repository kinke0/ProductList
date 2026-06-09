package com.superpower.modules.version.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.category.service.CategoryService;
import com.superpower.modules.data.repository.DataEntryRepository;
import com.superpower.modules.image.entity.ImageResource;
import com.superpower.modules.image.repository.ImageResourceRepository;
import com.superpower.modules.option.service.DataOptionService;
import com.superpower.modules.version.entity.DataVersion;
import com.superpower.modules.version.repository.DataVersionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DataVersionService {

    private final DataVersionRepository versionRepository;
    private final DataEntryRepository entryRepository;
    private final ImageResourceRepository imageResourceRepository;
    private final CategoryService categoryService;
    private final DataOptionService optionService;

    @Value("${app.image-storage-path:./uploads/images}")
    private String storagePath;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataVersionService.class);

    public DataVersionService(DataVersionRepository versionRepository,
                              DataEntryRepository entryRepository,
                              ImageResourceRepository imageResourceRepository,
                              CategoryService categoryService,
                              DataOptionService optionService) {
        this.versionRepository = versionRepository;
        this.entryRepository = entryRepository;
        this.imageResourceRepository = imageResourceRepository;
        this.categoryService = categoryService;
        this.optionService = optionService;
    }

    public List<DataVersion> findAllReleased() {
        return versionRepository.findAllReleased();
    }

    public List<DataVersion> findAll() {
        return versionRepository.findAll();
    }

    public DataVersion findById(Long id) {
        return versionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("版本不存在"));
    }

    @Transactional
    public DataVersion createVersion() {
        if (versionRepository.existsByStatus("draft")) {
            throw new BusinessException("已存在编辑中的版本，请先封板发布后再创建新版本");
        }

        String newVersionNo = "1.0";
        DataVersion latest = versionRepository.findTopByOrderByCreatedAtDesc().orElse(null);
        if (latest != null) {
            String lastNo = latest.getVersionNo();
            String[] parts = lastNo.split("\\.");
            int minor = Integer.parseInt(parts[1]) + 1;
            newVersionNo = parts[0] + "." + minor;
        }

        DataVersion version = new DataVersion();
        version.setVersionNo(newVersionNo);
        version.setStatus("draft");
        version = versionRepository.save(version);

        // 复制上一版本数据
        if (latest != null && "released".equals(latest.getStatus())) {
            List<DataEntry> entries = entryRepository.findByVersionId(latest.getId());
            HashMap<Long, Long> idMap = new HashMap<>();
            HashMap<Long, Long> oldParentMap = new HashMap<>();
            for (DataEntry entry : entries) {
                oldParentMap.put(entry.getId(), entry.getParentId());
                DataEntry copy = entry.cloneWithoutId();
                copy.setVersionId(version.getId());
                copy.setParentId(entry.getParentId());
                DataEntry saved = entryRepository.save(copy);
                idMap.put(entry.getId(), saved.getId());
            }

            // 第二步：更新 parentId 为新版本的对应 ID
            for (Map.Entry<Long, Long> e : idMap.entrySet()) {
                Long oldId = e.getKey();
                Long newId = e.getValue();
                Long oldParent = oldParentMap.get(oldId);
                if (oldParent != null) {
                    Long newParent = idMap.get(oldParent);
                    if (newParent != null) {
                        DataEntry entry = entryRepository.findById(newId).orElse(null);
                        if (entry != null) {
                            entry.setParentId(newParent);
                            entryRepository.save(entry);
                        }
                    }
                }
            }
        }

        if (latest != null && "released".equals(latest.getStatus())) {
            categoryService.copyFromVersion(latest.getId(), version.getId());
        }

        if (latest != null && "released".equals(latest.getStatus())) {
            optionService.copyOptions(latest.getId(), version.getId());
        }

        if (latest != null && "released".equals(latest.getStatus())) {
            List<ImageResource> images = imageResourceRepository.findByVersionIdOrderByCreatedAtDesc(latest.getId());
            for (ImageResource img : images) {
                ImageResource copy = new ImageResource();
                copy.setFilename(img.getFilename());
                copy.setStoredName(img.getStoredName());
                copy.setCategory(img.getCategory());
                copy.setDomain(img.getDomain());
                copy.setProduct(img.getProduct());
                copy.setSize(img.getSize());
                copy.setMimeType(img.getMimeType());
                copy.setUploadedBy(img.getUploadedBy());
                copy.setVersionId(version.getId());
                copy.setWidth(img.getWidth());
                copy.setHeight(img.getHeight());

                String oldUrlPrefix = "/api/images/file/" + latest.getId() + "/";
                String newUrlPrefix = "/api/images/file/" + version.getId() + "/";
                String reqOldPrefix = "/api/requirements/file/" + latest.getId() + "/";
                String reqNewPrefix = "/api/requirements/file/" + version.getId() + "/";

                if (img.getUrl() != null && img.getUrl().startsWith(oldUrlPrefix)) {
                    copy.setUrl(newUrlPrefix + img.getUrl().substring(oldUrlPrefix.length()));
                } else if (img.getUrl() != null && img.getUrl().startsWith(reqOldPrefix)) {
                    copy.setUrl(reqNewPrefix + img.getUrl().substring(reqOldPrefix.length()));
                } else {
                    copy.setUrl(img.getUrl());
                }

                String oldPathPrefix = storagePath.replace("\\", "/");
                if (!oldPathPrefix.endsWith("/")) oldPathPrefix += "/";
                oldPathPrefix += latest.getId() + "/";
                String newPathPrefix = storagePath.replace("\\", "/");
                if (!newPathPrefix.endsWith("/")) newPathPrefix += "/";
                newPathPrefix += version.getId() + "/";

                if (img.getPath() != null) {
                    String normalizedPath = img.getPath().replace("\\", "/");
                    if (normalizedPath.startsWith(oldPathPrefix)) {
                        String relative = normalizedPath.substring(oldPathPrefix.length());
                        Path newPath = Paths.get(storagePath, String.valueOf(version.getId()), relative);
                        Path oldPath = Paths.get(img.getPath());
                        try {
                            Files.createDirectories(newPath.getParent());
                            if (Files.exists(oldPath)) {
                                Files.copy(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (IOException e) {
                            log.warn("复制图片文件失败: {} -> {}: {}", oldPath, newPath, e.getMessage());
                        }
                        copy.setPath(newPath.toString());
                    } else {
                        copy.setPath(img.getPath());
                    }
                }

                imageResourceRepository.save(copy);
            }
        }

        if (latest != null && "released".equals(latest.getStatus())) {
            String oldUrlPrefix = "/api/images/file/" + latest.getId() + "/";
            String newUrlPrefix = "/api/images/file/" + version.getId() + "/";
            List<DataEntry> newEntries = entryRepository.findByVersionId(version.getId());
            for (DataEntry entry : newEntries) {
                boolean changed = false;
                String desc = entry.getColFeatureDesc();
                if (desc != null && desc.contains(oldUrlPrefix)) {
                    entry.setColFeatureDesc(desc.replace(oldUrlPrefix, newUrlPrefix));
                    changed = true;
                }
                String cp1 = entry.getColControlPointImg1();
                if (cp1 != null && cp1.contains(oldUrlPrefix)) {
                    entry.setColControlPointImg1(cp1.replace(oldUrlPrefix, newUrlPrefix));
                    changed = true;
                }
                String cp2 = entry.getColControlPointImg2();
                if (cp2 != null && cp2.contains(oldUrlPrefix)) {
                    entry.setColControlPointImg2(cp2.replace(oldUrlPrefix, newUrlPrefix));
                    changed = true;
                }
                String cp3 = entry.getColControlPointImg3();
                if (cp3 != null && cp3.contains(oldUrlPrefix)) {
                    entry.setColControlPointImg3(cp3.replace(oldUrlPrefix, newUrlPrefix));
                    changed = true;
                }
                String cpDoc = entry.getColControlPointDoc();
                if (cpDoc != null && cpDoc.contains(oldUrlPrefix)) {
                    entry.setColControlPointDoc(cpDoc.replace(oldUrlPrefix, newUrlPrefix));
                    changed = true;
                }
                if (changed) {
                    entryRepository.save(entry);
                }
            }
        }

        return version;
    }

    @Transactional
    public DataVersion getOrCreateInitialVersion() {
        List<DataVersion> all = versionRepository.findAll();
        if (!all.isEmpty()) {
            return all.get(0);
        }
        return createVersion();
    }

    @Transactional
    public DataVersion releaseVersion(Long versionId, Long userId) {
        DataVersion version = findById(versionId);
        if (!"draft".equals(version.getStatus())) {
            throw new BusinessException("版本状态不正确");
        }
        if (version.getRollbackCount() != null && version.getRollbackCount() > 0) {
            String baseNo = version.getVersionNo();
            int dotCount = 0;
            for (char c : baseNo.toCharArray()) {
                if (c == '.') dotCount++;
            }
            if (dotCount < 2) {
                version.setVersionNo(baseNo + ".1");
            } else {
                int lastDot = baseNo.lastIndexOf('.');
                String prefix = baseNo.substring(0, lastDot + 1);
                String suffix = baseNo.substring(lastDot + 1);
                int patch = Integer.parseInt(suffix) + 1;
                version.setVersionNo(prefix + patch);
            }
        }
        version.setStatus("released");
        version.setReleasedAt(LocalDateTime.now());
        version.setReleasedBy(userId);
        return versionRepository.save(version);
    }

    @Transactional
    public DataVersion rollbackVersion(Long versionId) {
        DataVersion version = findById(versionId);
        if (!"released".equals(version.getStatus())) {
            throw new BusinessException("只能退回已发布的版本");
        }
        if (versionRepository.existsByStatus("draft")) {
            throw new BusinessException("已存在编辑中的版本，请先处理后再退回");
        }
        version.setStatus("draft");
        version.setRollbackCount(version.getRollbackCount() != null ? version.getRollbackCount() + 1 : 1);
        version.setReleasedAt(null);
        version.setReleasedBy(null);
        return versionRepository.save(version);
    }
}
