package com.superpower.modules.data.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.data.dto.DataEntryDTO;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import com.superpower.modules.version.entity.DataVersion;
import com.superpower.modules.version.repository.DataVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataEntryServiceTest {

    @Mock
    private DataEntryRepository entryRepository;

    @Mock
    private DataVersionRepository dataVersionRepository;

    @InjectMocks
    private DataEntryService dataEntryService;

    private DataEntry releasedEntry;
    private DataEntry draftEntry;

    @BeforeEach
    void setUp() {
        releasedEntry = new DataEntry();
        releasedEntry.setId(1L);
        releasedEntry.setVersionId(100L);
        releasedEntry.setLevel(3);
        releasedEntry.setSortOrder(1);
        releasedEntry.setIsLeaf(true);

        draftEntry = new DataEntry();
        draftEntry.setId(2L);
        draftEntry.setVersionId(200L);
        draftEntry.setLevel(3);
        draftEntry.setSortOrder(1);
        draftEntry.setIsLeaf(true);
    }

    @Test
    void shouldRejectUpdateWhenVersionReleased() {
        when(entryRepository.findById(1L)).thenReturn(Optional.of(releasedEntry));
        when(dataVersionRepository.findById(100L)).thenReturn(Optional.of(versionWithStatus(100L, "released")));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> dataEntryService.update(1L, new DataEntryDTO()));

        assertEquals("已发版版本不允许修改清单", exception.getMessage());
        verify(entryRepository, never()).save(any(DataEntry.class));
    }

    @Test
    void shouldRejectDeleteWhenVersionReleased() {
        when(entryRepository.findById(1L)).thenReturn(Optional.of(releasedEntry));
        when(dataVersionRepository.findById(100L)).thenReturn(Optional.of(versionWithStatus(100L, "released")));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> dataEntryService.delete(1L));

        assertEquals("已发版版本不允许修改清单", exception.getMessage());
        verify(entryRepository, never()).deleteById(1L);
    }

    @Test
    void shouldRejectUpdateSortWhenVersionReleased() {
        when(entryRepository.findById(1L)).thenReturn(Optional.of(releasedEntry));
        when(dataVersionRepository.findById(100L)).thenReturn(Optional.of(versionWithStatus(100L, "released")));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> dataEntryService.updateSort(List.of(Map.of("id", 1L, "sortOrder", 2))));

        assertEquals("已发版版本不允许修改清单", exception.getMessage());
        verify(entryRepository, never()).save(any(DataEntry.class));
    }

    @Test
    void shouldAllowCreateWhenVersionDraft() {
        DataEntryDTO dto = new DataEntryDTO();
        dto.setVersionId(200L);
        dto.setLevel(3);
        dto.setSortOrder(10);
        dto.setColProductSystem("门诊系统");

        when(dataVersionRepository.findById(200L)).thenReturn(Optional.of(versionWithStatus(200L, "draft")));
        when(entryRepository.save(any(DataEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DataEntry created = dataEntryService.create(dto);

        assertNotNull(created);
        assertEquals(200L, created.getVersionId());
        assertEquals(3, created.getLevel());
        assertEquals(10, created.getSortOrder());
        assertEquals("门诊系统", created.getColProductSystem());
    }

    @Test
    void shouldPassExpandedQueryParametersToRepository() {
        when(entryRepository.queryEntries(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(draftEntry));

        List<DataEntry> result = dataEntryService.query(200L, null, "名称", "启用", "张三", "方案A", "V1", "分类A", "域A");

        assertEquals(1, result.size());
        verify(entryRepository).queryEntries(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private DataVersion versionWithStatus(Long id, String status) {
        DataVersion version = new DataVersion();
        version.setId(id);
        version.setStatus(status);
        return version;
    }
}
