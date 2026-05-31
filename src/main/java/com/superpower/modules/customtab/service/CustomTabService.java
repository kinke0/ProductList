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
                                       String solution, String versionTag) {
        CustomTab tab = create(name, versionId, userId);
        List<DataEntry> entries = dataEntryRepository.queryEntries(
                versionId, null,
                (entryName != null && !entryName.isEmpty()) ? entryName : null,
                (productManager != null && !productManager.isEmpty()) ? productManager : null,
                (solution != null && !solution.isEmpty()) ? solution : null,
                (versionTag != null && !versionTag.isEmpty()) ? versionTag : null,
                null, null);
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
        List<CustomTabEntry> existing = customTabEntryRepository.findByCustomTabIdOrderBySortOrder(tabId);
        Set<Long> existingIds = existing.stream().map(CustomTabEntry::getEntryId).collect(Collectors.toSet());
        int maxSort = -1;
        for (CustomTabEntry te : existing) {
            if (te.getSortOrder() != null && te.getSortOrder() > maxSort) {
                maxSort = te.getSortOrder();
            }
        }
        for (Long entryId : entryIds) {
            if (existingIds.contains(entryId)) continue;
            CustomTabEntry entry = new CustomTabEntry();
            entry.setCustomTabId(tabId);
            entry.setEntryId(entryId);
            entry.setSortOrder(maxSort + 1);
            customTabEntryRepository.save(entry);
            maxSort++;
        }
    }

    private void fixNullSort(Long tabId, List<CustomTabEntry> existing) {
        int maxSort = -1;
        for (CustomTabEntry te : existing) {
            if (te.getSortOrder() != null && te.getSortOrder() > maxSort) {
                maxSort = te.getSortOrder();
            }
        }
        int order = 0;
        for (CustomTabEntry te : existing) {
            if (te.getSortOrder() == null) {
                te.setSortOrder(maxSort + 1 + order++);
                customTabEntryRepository.save(te);
            }
        }
    }

    private void reorderTabByHierarchy(Long tabId) {
        List<CustomTabEntry> allEntries = customTabEntryRepository.findByCustomTabIdOrderBySortOrder(tabId);
        List<Long> entryIds = allEntries.stream().map(CustomTabEntry::getEntryId).collect(Collectors.toList());
        if (entryIds.isEmpty()) return;
        List<DataEntry> dataEntries = dataEntryRepository.findAllById(entryIds);
        Map<Long, DataEntry> entryMap = new HashMap<>();
        for (DataEntry e : dataEntries) entryMap.put(e.getId(), e);

        List<DataEntry> sorted = new ArrayList<>();
        for (Long id : entryIds) {
            DataEntry e = entryMap.get(id);
            if (e != null) sorted.add(e);
        }
        sorted.sort(Comparator
            .comparing(DataEntry::getColBizCategory, Comparator.nullsLast(String::compareTo))
            .thenComparing(DataEntry::getColBizDomain, Comparator.nullsLast(String::compareTo))
            .thenComparing(DataEntry::getParentId, Comparator.nullsLast(Long::compareTo))
            .thenComparing(DataEntry::getLevel, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(DataEntry::getSortOrder, Comparator.nullsLast(Integer::compareTo)));

        int idx = 0;
        for (DataEntry e : sorted) {
            customTabEntryRepository.updateSortOrder(tabId, e.getId(), idx++);
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
