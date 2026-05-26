package com.superpower.modules.customtab.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.customtab.entity.CustomTab;
import com.superpower.modules.customtab.entity.CustomTabEntry;
import com.superpower.modules.customtab.repository.CustomTabEntryRepository;
import com.superpower.modules.customtab.repository.CustomTabRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public CustomTab getById(Long id) {
        return customTabRepository.findById(id)
                .orElseThrow(() -> new BusinessException("清单不存在"));
    }

    @Transactional
    public void addEntries(Long tabId, List<Long> entryIds) {
        getById(tabId);
        for (Long entryId : entryIds) {
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
