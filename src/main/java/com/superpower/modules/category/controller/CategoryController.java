package com.superpower.modules.category.controller;

import com.superpower.common.Result;
import com.superpower.modules.category.service.CategoryService;
import com.superpower.modules.data.dto.TreeNodeDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/tree")
    public Result<List<TreeNodeDTO>> getTree(@RequestParam Long versionId) {
        return Result.success(categoryService.getTree(versionId));
    }

    @PutMapping("/category/sort")
    public Result<Void> updateSort(@RequestParam Long versionId, @RequestBody List<Map<String, Object>> sortList) {
        categoryService.updateSortOrders(versionId, sortList);
        return Result.success();
    }
}
