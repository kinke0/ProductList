package com.superpower.modules.option.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.option.entity.DataOption;
import com.superpower.modules.option.repository.DataOptionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataOptionService {

    private final DataOptionRepository repository;

    public DataOptionService(DataOptionRepository repository) {
        this.repository = repository;
    }

    public List<DataOption> getByType(String type) {
        return repository.findByTypeOrderBySortOrder(type);
    }

    public DataOption create(String type, String value) {
        List<DataOption> existing = repository.findByTypeOrderBySortOrder(type);
        boolean dup = existing.stream().anyMatch(o -> o.getValue().equals(value));
        if (dup) {
            throw new BusinessException("该选项已存在");
        }
        DataOption opt = new DataOption();
        opt.setType(type);
        opt.setValue(value);
        return repository.save(opt);
    }

    public DataOption update(Long id, String value) {
        DataOption opt = repository.findById(id)
                .orElseThrow(() -> new BusinessException("选项不存在"));
        opt.setValue(value);
        return repository.save(opt);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
