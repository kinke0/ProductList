package com.superpower.modules.customtab.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.customtab.entity.CustomTab;
import com.superpower.modules.customtab.entity.CustomTabEntry;
import com.superpower.modules.customtab.repository.CustomTabEntryRepository;
import com.superpower.modules.customtab.repository.CustomTabRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CustomTabService {

    private final CustomTabRepository customTabRepository;
    private final CustomTabEntryRepository customTabEntryRepository;

    public CustomTabService(CustomTabRepository customTabRepository,
                            CustomTabEntryRepository customTabEntryRepository) {
        this.customTabRepository = customTabRepository;
        this.customTabEntryRepository = customTabEntryRepository;
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
        List<CustomTabEntry> existing = customTabEntryRepository.findByCustomTabIdOrderBySortOrder(tabId);
        int maxSort = -1;
        for (CustomTabEntry te : existing) {
            if (te.getSortOrder() != null && te.getSortOrder() > maxSort) {
                maxSort = te.getSortOrder();
            }
        }
        Set<Long> existingIds = existing.stream().map(CustomTabEntry::getEntryId).collect(java.util.stream.Collectors.toSet());
        for (int i = 0; i < entryIds.size(); i++) {
            Long entryId = entryIds.get(i);
            if (existingIds.contains(entryId)) continue;
            CustomTabEntry entry = new CustomTabEntry();
            entry.setCustomTabId(tabId);
            entry.setEntryId(entryId);
            entry.setSortOrder(maxSort + 1 + i);
            customTabEntryRepository.save(entry);
        }
        int order = 0;
        for (CustomTabEntry te : existing) {
            if (te.getSortOrder() == null) {
                te.setSortOrder(maxSort + 1 + entryIds.size() + order++);
                customTabEntryRepository.save(te);
            }
        }
    }

    @Transactional
    public void removeEntry(Long tabId, Long entryId) {
        customTabEntryRepository.deleteByCustomTabIdAndEntryId(tabId, entryId);
    }

    @Transactional
    public void updateSortOrders(Long tabId, List<Map<String, Object>> sortList) {
        for (Map<String, Object> item : sortList) {
            Long entryId = Long.valueOf(item.get("entryId").toString());
            Integer sortOrder = Integer.valueOf(item.get("sortOrder").toString());
            customTabEntryRepository.updateSortOrder(tabId, entryId, sortOrder);
        }
    }
}
