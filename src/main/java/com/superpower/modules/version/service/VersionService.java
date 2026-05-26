package com.superpower.modules.version.service;

import com.superpower.modules.version.entity.DataVersion;
import com.superpower.modules.version.repository.DataVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VersionService {
    private final DataVersionRepository versionRepository;

    public String getAccessStatus(Long versionId) {
        DataVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new RuntimeException("版本不存在"));
        return version.getStatus();
    }

    public boolean isEditable(Long versionId) {
        return "draft".equals(getAccessStatus(versionId));
    }
}
