package com.superpower.modules.option.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.option.entity.DataOption;
import com.superpower.modules.option.repository.DataOptionRepository;
import com.superpower.modules.version.entity.DataVersion;
import com.superpower.modules.version.repository.DataVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class DataOptionService {

    private final DataOptionRepository repository;
    private final DataVersionRepository versionRepository;

    public DataOptionService(DataOptionRepository repository,
                             DataVersionRepository versionRepository) {
        this.repository = repository;
        this.versionRepository = versionRepository;
    }

    public List<DataOption> getByType(Long versionId, String type) {
        return repository.findByTypeAndVersionIdOrderBySortOrder(type, versionId);
    }

    @Transactional
    public DataOption create(Long versionId, String type, String value) {
        ensureVersionEditable(versionId);
        List<DataOption> existing = repository.findByTypeAndVersionIdOrderBySortOrder(type, versionId);
        DataOption opt = new DataOption();
        opt.setType(type);
        opt.setValue(value);
        opt.setVersionId(versionId);
        opt.setSortOrder(existing.size());
        return repository.save(opt);
    }

    @Transactional
    public DataOption update(Long id, String value) {
        DataOption opt = repository.findById(id)
                .orElseThrow(() -> new BusinessException("选项不存在"));
        ensureVersionEditable(opt.getVersionId());
        opt.setValue(value);
        return repository.save(opt);
    }

    @Transactional
    public void delete(Long id) {
        DataOption opt = repository.findById(id)
                .orElseThrow(() -> new BusinessException("选项不存在"));
        ensureVersionEditable(opt.getVersionId());
        repository.deleteById(id);
    }

    private void ensureVersionEditable(Long versionId) {
        DataVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException("版本不存在"));
        if (!"draft".equals(version.getStatus())) {
            throw new BusinessException("已发版版本不允许修改清单");
        }
    }

    @Transactional
    public void updateSortOrders(List<Map<String, Object>> sortList) {
        for (Map<String, Object> item : sortList) {
            Long id = Long.valueOf(item.get("id").toString());
            Integer sortOrder = Integer.valueOf(item.get("sortOrder").toString());
            DataOption opt = repository.findById(id)
                    .orElseThrow(() -> new BusinessException("选项不存在"));
            ensureVersionEditable(opt.getVersionId());
            opt.setSortOrder(sortOrder);
            repository.save(opt);
        }
    }

    @Transactional
    public void copyOptions(Long sourceVersionId, Long targetVersionId) {
        List<DataOption> srcOptions = repository.findByVersionId(sourceVersionId);
        for (DataOption src : srcOptions) {
            DataOption opt = new DataOption();
            opt.setType(src.getType());
            opt.setValue(src.getValue());
            opt.setVersionId(targetVersionId);
            opt.setSortOrder(src.getSortOrder());
            repository.save(opt);
        }
    }
}
