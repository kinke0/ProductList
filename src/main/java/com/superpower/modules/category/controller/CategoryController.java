package com.superpower.modules.category.controller;

import com.superpower.common.Result;
import com.superpower.modules.category.entity.BaseCategory;
import com.superpower.modules.category.entity.BaseDomain;
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

    @PostMapping("/category")
    public Result<BaseCategory> createCategory(@RequestParam Long versionId, @RequestBody Map<String, String> body) {
        return Result.success(categoryService.createCategory(versionId, body.get("name")));
    }

    @PutMapping("/category/{id}")
    public Result<BaseCategory> updateCategory(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(categoryService.updateCategory(id, body.get("name")));
    }

    @DeleteMapping("/category/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }

    @PostMapping("/domain")
    public Result<BaseDomain> createDomain(@RequestParam Long versionId, @RequestParam Long categoryId, @RequestBody Map<String, String> body) {
        return Result.success(categoryService.createDomain(versionId, categoryId, body.get("name")));
    }

    @PutMapping("/domain/{id}")
    public Result<BaseDomain> updateDomain(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(categoryService.updateDomain(id, body.get("name")));
    }

    @DeleteMapping("/domain/{id}")
    public Result<Void> deleteDomain(@PathVariable Long id) {
        categoryService.deleteDomain(id);
        return Result.success();
    }
}
