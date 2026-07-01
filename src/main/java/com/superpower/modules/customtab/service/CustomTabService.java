package com.superpower.modules.customtab.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.customtab.entity.CustomTab;
import com.superpower.modules.customtab.entity.CustomTabEntry;
import com.superpower.modules.customtab.repository.CustomTabEntryRepository;
import com.superpower.modules.customtab.repository.CustomTabRepository;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomTabService {

    private final CustomTabRepository customTabRepository;
    private final CustomTabEntryRepository customTabEntryRepository;
    private final DataEntryRepository dataEntryRepository;

    public CustomTabService(CustomTabRepository customTabRepository,
                            CustomTabEntryRepository customTabEntryRepository,
                            DataEntryRepository dataEntryRepository) {
        this.customTabRepository = customTabRepository;
        this.customTabEntryRepository = customTabEntryRepository;
        this.dataEntryRepository = dataEntryRepository;
    }

    @Transactional
    public CustomTab createWithFilter(String name, Long versionId, Long userId,
                                       String entryName, List<String> statusList, String productManager,
                                       String solution, List<String> versionTags) {
        CustomTab tab = create(name, versionId, userId);
        String singleVersionTag = (versionTags != null && versionTags.size() == 1) ? versionTags.get(0) : null;
        List<DataEntry> entries = dataEntryRepository.queryEntries(
                versionId, null,
                (entryName != null && !entryName.isEmpty()) ? entryName : null,
                (productManager != null && !productManager.isEmpty()) ? productManager : null,
                (solution != null && !solution.isEmpty()) ? solution : null,
                singleVersionTag,
                null, null);
        if (versionTags != null && versionTags.size() > 1) {
            entries = entries.stream()
                    .filter(e -> e.getColVersionDivision() != null && versionTags.stream().anyMatch(t -> e.getColVersionDivision().contains(t)))
                    .toList();
        }
        if (statusList != null && !statusList.isEmpty()) {
            entries = entries.stream()
                    .filter(e -> e.getColStatus() != null && statusList.stream().anyMatch(s -> e.getColStatus().contains(s)))
                    .toList();
        }
        List<Long> entryIds = entries.stream().map(DataEntry::getId).collect(Collectors.toList());
        if (!entryIds.isEmpty()) {
            addEntries(tab.getId(), entryIds);
        }
        return tab;
    }

    public List<CustomTab> findByVersionId(Long versionId) {
        return customTabRepository.findByVersionIdOrderByCreatedAtAsc(versionId);
    }

    @Transactional
    public CustomTab create(String name, Long versionId, Long userId) {
        if (customTabRepository.existsByVersionIdAndName(versionId, name)) {
            throw new BusinessException("清单名称已存在");
        }
        CustomTab tab = new CustomTab();
        tab.setName(name);
        tab.setVersionId(versionId);
        tab.setUserId(userId);
        return customTabRepository.save(tab);
    }

    @Transactional
    public void delete(Long id) {
        customTabEntryRepository.deleteByCustomTabId(id);
        customTabRepository.deleteById(id);
    }

    @Transactional
    public CustomTab rename(Long id, String name) {
        CustomTab tab = getById(id);
        if (customTabRepository.existsByVersionIdAndName(tab.getVersionId(), name)) {
            throw new BusinessException("清单名称已存在");
        }
        tab.setName(name);
        return customTabRepository.save(tab);
    }

    public CustomTab getById(Long id) {
        return customTabRepository.findById(id)
                .orElseThrow(() -> new BusinessException("清单不存在"));
    }

    @Transactional
    public void addEntries(Long tabId, List<Long> entryIds) {
        getById(tabId);
        Set<Long> existingIds = customTabEntryRepository.findByCustomTabId(tabId)
                .stream().map(CustomTabEntry::getEntryId).collect(Collectors.toSet());
        for (Long entryId : entryIds) {
            if (existingIds.contains(entryId)) continue;
            CustomTabEntry entry = new CustomTabEntry();
            entry.setCustomTabId(tabId);
            entry.setEntryId(entryId);
            customTabEntryRepository.save(entry);
        }
    }

    @Transactional
    public void removeEntry(Long tabId, Long entryId) {
        customTabEntryRepository.deleteByCustomTabIdAndEntryId(tabId, entryId);
    }
}
