package com.superpower.modules.category.controller;

import com.superpower.common.Result;
import com.superpower.modules.category.entity.BaseProductL1;
import com.superpower.modules.category.entity.BaseProductL2;
import com.superpower.modules.category.service.ProductService;
import com.superpower.modules.data.dto.ExcelImportResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // L1 接口
    @GetMapping("/l1/list")
    public Result<List<BaseProductL1>> getL1List(@RequestParam Long versionId) {
        return Result.success(productService.getL1List(versionId));
    }

    @PostMapping("/l1")
    public Result<BaseProductL1> createL1(@RequestParam Long versionId, @RequestBody Map<String, String> body) {
        return Result.success(productService.createL1(versionId, body.get("name")));
    }

    @PutMapping("/l1/{id}")
    public Result<BaseProductL1> updateL1(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(productService.updateL1(id, body.get("name")));
    }

    @DeleteMapping("/l1/{id}")
    public Result<Void> deleteL1(@PathVariable Long id) {
        productService.deleteL1(id);
        return Result.success();
    }

    @PutMapping("/l1/sort")
    public Result<Void> updateL1Sort(@RequestParam Long versionId, @RequestBody List<Map<String, Object>> sortList) {
        productService.updateL1SortOrders(versionId, sortList);
        return Result.success();
    }

    // L2 接口
    @GetMapping("/l2/list")
    public Result<List<BaseProductL2>> getL2List(@RequestParam Long versionId, @RequestParam Long l1Id) {
        return Result.success(productService.getL2List(versionId, l1Id));
    }

    @PostMapping("/l2")
    public Result<BaseProductL2> createL2(@RequestParam Long versionId, @RequestParam Long l1Id, @RequestBody Map<String, String> body) {
        return Result.success(productService.createL2(versionId, l1Id, body.get("name")));
    }

    @PutMapping("/l2/{id}")
    public Result<BaseProductL2> updateL2(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(productService.updateL2(id, body.get("name")));
    }

    @DeleteMapping("/l2/{id}")
    public Result<Void> deleteL2(@PathVariable Long id) {
        productService.deleteL2(id);
        return Result.success();
    }

    @PutMapping("/l2/sort")
    public Result<Void> updateL2Sort(@RequestParam Long versionId, @RequestBody List<Map<String, Object>> sortList) {
        productService.updateL2SortOrders(versionId, sortList);
        return Result.success();
    }

    // 导入Excel
    @PostMapping("/import-excel")
    public Result<ExcelImportResult> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("versionId") Long versionId) {
        return Result.success(productService.importFromExcel(file, versionId));
    }
}
