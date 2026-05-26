package com.superpower.modules.version.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.category.service.CategoryService;
import com.superpower.modules.data.repository.DataEntryRepository;
import com.superpower.modules.option.service.DataOptionService;
import com.superpower.modules.version.entity.DataVersion;
import com.superpower.modules.version.repository.DataVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DataVersionService {

    private final DataVersionRepository versionRepository;
    private final DataEntryRepository entryRepository;
    private final CategoryService categoryService;
    private final DataOptionService optionService;

    public DataVersionService(DataVersionRepository versionRepository,
                              DataEntryRepository entryRepository,
                              CategoryService categoryService,
                              DataOptionService optionService) {
        this.versionRepository = versionRepository;
        this.entryRepository = entryRepository;
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
        version.setStatus("released");
        version.setReleasedAt(LocalDateTime.now());
        version.setReleasedBy(userId);
        return versionRepository.save(version);
    }
}
