package com.superpower.modules.customtab.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.customtab.entity.CustomTab;
import com.superpower.modules.customtab.repository.CustomTabEntryRepository;
import com.superpower.modules.customtab.repository.CustomTabRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomTabServiceTest {

    @Mock
    private CustomTabRepository customTabRepository;

    @Mock
    private CustomTabEntryRepository customTabEntryRepository;

    @InjectMocks
    private CustomTabService customTabService;

    @Test
    void create_shouldThrowWhenNameExists() {
        when(customTabRepository.existsByVersionIdAndName(1L, "门诊清单")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> customTabService.create("门诊清单", 1L, 1L));

        assertEquals("清单名称已存在", ex.getMessage());
        verify(customTabRepository, never()).save(any());
    }

    @Test
    void create_shouldSaveWhenNameIsNew() {
        when(customTabRepository.existsByVersionIdAndName(1L, "新清单")).thenReturn(false);
        when(customTabRepository.save(any(CustomTab.class))).thenAnswer(inv -> {
            CustomTab t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        CustomTab result = customTabService.create("新清单", 1L, 1L);

        assertEquals("新清单", result.getName());
        assertEquals(1L, result.getVersionId());
        assertEquals(10L, result.getId());
    }

    @Test
    void addEntries_shouldSaveAllEntries() {
        CustomTab tab = new CustomTab();
        tab.setId(5L);
        tab.setName("测试清单");
        when(customTabRepository.findById(5L)).thenReturn(Optional.of(tab));

        customTabService.addEntries(5L, List.of(100L, 200L, 300L));

        verify(customTabEntryRepository, times(3)).save(any());
    }

    @Test
    void delete_shouldCleanUpEntriesAndTab() {
        customTabService.delete(5L);

        verify(customTabEntryRepository).deleteByCustomTabId(5L);
        verify(customTabRepository).deleteById(5L);
    }
}
