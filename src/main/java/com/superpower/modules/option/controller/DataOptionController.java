package com.superpower.modules.option.controller;

import com.superpower.common.Result;
import com.superpower.modules.option.entity.DataOption;
import com.superpower.modules.option.service.DataOptionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/options")
public class DataOptionController {

    private final DataOptionService service;

    public DataOptionController(DataOptionService service) {
        this.service = service;
    }

    @GetMapping("/{versionId}/{type}")
    public Result<List<DataOption>> getByType(@PathVariable Long versionId, @PathVariable String type) {
        return Result.success(service.getByType(versionId, type));
    }

    @PostMapping("/{versionId}/{type}")
    public Result<DataOption> create(@PathVariable Long versionId, @PathVariable String type,
                                     @RequestBody Map<String, String> body) {
        return Result.success(service.create(versionId, type, body.get("value")));
    }

    @PutMapping("/{id}")
    public Result<DataOption> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(service.update(id, body.get("value")));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success();
    }
}
