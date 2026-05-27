# 静态分类树版本化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将左侧层级树的数据源从 DataEntry 表切换为独立的 BaseCategory/BaseDomain 表，支持版本管理，并从 Excel 导入初始数据。

**Architecture:** 新增 base_category/base_domain 表（带 version_id），CategoryService 负责树构建和 Excel 导入，CategoryController 提供 GET /api/tree 接口，TreePanel 调用新接口。

**Tech Stack:** Spring Boot 3.2.0, Spring Data JPA, Apache POI, SQLite, Vue 3, Element Plus

---

## File Structure Map

### 后端新增
- Create: `src/main/java/com/superpower/modules/category/entity/BaseCategory.java`
- Create: `src/main/java/com/superpower/modules/category/entity/BaseDomain.java`
- Create: `src/main/java/com/superpower/modules/category/repository/BaseCategoryRepository.java`
- Create: `src/main/java/com/superpower/modules/category/repository/BaseDomainRepository.java`
- Create: `src/main/java/com/superpower/modules/category/service/CategoryService.java`
- Create: `src/main/java/com/superpower/modules/category/controller/CategoryController.java`

### 后端修改
- Modify: `src/main/java/com/superpower/modules/version/service/DataVersionService.java` — 创建新版本时复制分类数据

### 前端修改
- Modify: `frontend/src/api/data.js` — getTree 改为调用 `/tree`
- Modify: `frontend/src/components/TreePanel.vue` — 适配新接口返回格式

---

### Task 1: 创建分类实体类

**Files:**
- Create: `src/main/java/com/superpower/modules/category/entity/BaseCategory.java`
- Create: `src/main/java/com/superpower/modules/category/entity/BaseDomain.java`

- [ ] **Step 1: 确保目录存在**

Run: `mkdir -p src/main/java/com/superpower/modules/category/entity`

- [ ] **Step 2: 创建 BaseCategory 实体**

Create new file `src/main/java/com/superpower/modules/category/entity/BaseCategory.java`:
```java
package com.superpower.modules.category.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "base_category")
public class BaseCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
```

- [ ] **Step 3: 创建 BaseDomain 实体**

Create new file `src/main/java/com/superpower/modules/category/entity/BaseDomain.java`:
```java
package com.superpower.modules.category.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "base_domain")
public class BaseDomain {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/superpower/modules/category/entity/
git commit -m "feat(category): add BaseCategory and BaseDomain entities"
```

---

### Task 2: 创建 Repository 层

**Files:**
- Create: `src/main/java/com/superpower/modules/category/repository/BaseCategoryRepository.java`
- Create: `src/main/java/com/superpower/modules/category/repository/BaseDomainRepository.java`

- [ ] **Step 1: 确保目录存在**

Run: `mkdir -p src/main/java/com/superpower/modules/category/repository`

- [ ] **Step 2: 创建 BaseCategoryRepository**

Create new file `src/main/java/com/superpower/modules/category/repository/BaseCategoryRepository.java`:
```java
package com.superpower.modules.category.repository;

import com.superpower.modules.category.entity.BaseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaseCategoryRepository extends JpaRepository<BaseCategory, Long> {
    List<BaseCategory> findByVersionIdOrderBySortOrderAsc(Long versionId);
    void deleteByVersionId(Long versionId);
}
```

- [ ] **Step 3: 创建 BaseDomainRepository**

Create new file `src/main/java/com/superpower/modules/category/repository/BaseDomainRepository.java`:
```java
package com.superpower.modules.category.repository;

import com.superpower.modules.category.entity.BaseDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaseDomainRepository extends JpaRepository<BaseDomain, Long> {
    List<BaseDomain> findByVersionIdAndCategoryIdOrderBySortOrderAsc(Long versionId, Long categoryId);
    void deleteByVersionId(Long versionId);
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/superpower/modules/category/repository/
git commit -m "feat(category): add BaseCategory and BaseDomain repositories"
```

---

### Task 3: 创建 CategoryService

**Files:**
- Create: `src/main/java/com/superpower/modules/category/service/CategoryService.java`

- [ ] **Step 1: 确保目录存在**

Run: `mkdir -p src/main/java/com/superpower/modules/category/service`

- [ ] **Step 2: 创建 CategoryService**

Create new file `src/main/java/com/superpower/modules/category/service/CategoryService.java`:
```java
package com.superpower.modules.category.service;

import com.superpower.modules.category.entity.BaseCategory;
import com.superpower.modules.category.entity.BaseDomain;
import com.superpower.modules.category.repository.BaseCategoryRepository;
import com.superpower.modules.category.repository.BaseDomainRepository;
import com.superpower.modules.data.dto.TreeNodeDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CategoryService {

    private final BaseCategoryRepository categoryRepository;
    private final BaseDomainRepository domainRepository;

    public CategoryService(BaseCategoryRepository categoryRepository,
                           BaseDomainRepository domainRepository) {
        this.categoryRepository = categoryRepository;
        this.domainRepository = domainRepository;
    }

    public List<TreeNodeDTO> getTree(Long versionId) {
        List<BaseCategory> categories = categoryRepository.findByVersionIdOrderBySortOrderAsc(versionId);
        List<TreeNodeDTO> result = new ArrayList<>();
        for (BaseCategory cat : categories) {
            TreeNodeDTO node = new TreeNodeDTO();
            node.setId(cat.getId());
            node.setParentId(null);
            node.setLevel(1);
            node.setLabel(cat.getName());
            node.setSortOrder(cat.getSortOrder());
            node.setIsLeaf(false);

            List<BaseDomain> domains = domainRepository.findByVersionIdAndCategoryIdOrderBySortOrderAsc(versionId, cat.getId());
            List<TreeNodeDTO> children = new ArrayList<>();
            for (BaseDomain dom : domains) {
                TreeNodeDTO child = new TreeNodeDTO();
                child.setId(dom.getId());
                child.setParentId(cat.getId());
                child.setLevel(2);
                child.setLabel(dom.getName());
                child.setSortOrder(dom.getSortOrder());
                child.setIsLeaf(true);
                children.add(child);
            }
            node.setChildren(children);
            result.add(node);
        }
        return result;
    }

    private static final Pattern ORDER_PATTERN = Pattern.compile("^(\\d+)");

    private int extractSortOrder(String name) {
        if (name == null) return 0;
        Matcher m = ORDER_PATTERN.matcher(name.trim());
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    @Transactional
    public void importFromExcel(Long versionId, String excelPath) throws Exception {
        try (FileInputStream fis = new FileInputStream(excelPath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);

            LinkedHashMap<String, Integer> categoryOrder = new LinkedHashMap<>();
            LinkedHashMap<String, LinkedHashMap<String, Integer>> categoryDomains = new LinkedHashMap<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Cell catCell = row.getCell(5); // 业务分类
                Cell domCell = row.getCell(6); // 业务域
                if (catCell == null || domCell == null) continue;
                String catName = catCell.getStringCellValue().trim();
                String domName = domCell.getStringCellValue().trim();
                if (catName.isEmpty() || domName.isEmpty()) continue;

                categoryOrder.putIfAbsent(catName, extractSortOrder(catName));
                categoryDomains.computeIfAbsent(catName, k -> new LinkedHashMap<>())
                        .putIfAbsent(domName, extractSortOrder(domName));
            }

            categoryOrder.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .forEach(catEntry -> {
                        String catName = catEntry.getKey();
                        BaseCategory cat = new BaseCategory();
                        cat.setVersionId(versionId);
                        cat.setName(catName);
                        cat.setSortOrder(catEntry.getValue());
                        cat = categoryRepository.save(cat);

                        LinkedHashMap<String, Integer> domains = categoryDomains.get(catName);
                        domains.entrySet().stream()
                                .sorted(Map.Entry.comparingByValue())
                                .forEach(domEntry -> {
                                    BaseDomain dom = new BaseDomain();
                                    dom.setVersionId(versionId);
                                    dom.setCategoryId(cat.getId());
                                    dom.setName(domEntry.getKey());
                                    dom.setSortOrder(domEntry.getValue());
                                    domainRepository.save(dom);
                                });
                    });
        }
    }

    @Transactional
    public void copyFromVersion(Long sourceVersionId, Long targetVersionId) {
        Map<Long, Long> catIdMap = new HashMap<>();
        List<BaseCategory> srcCategories = categoryRepository.findByVersionIdOrderBySortOrderAsc(sourceVersionId);
        for (BaseCategory src : srcCategories) {
            BaseCategory cat = new BaseCategory();
            cat.setVersionId(targetVersionId);
            cat.setName(src.getName());
            cat.setSortOrder(src.getSortOrder());
            cat = categoryRepository.save(cat);
            catIdMap.put(src.getId(), cat.getId());

            List<BaseDomain> domains = domainRepository.findByVersionIdAndCategoryIdOrderBySortOrderAsc(sourceVersionId, src.getId());
            for (BaseDomain srcDom : domains) {
                BaseDomain dom = new BaseDomain();
                dom.setVersionId(targetVersionId);
                dom.setCategoryId(cat.getId());
                dom.setName(srcDom.getName());
                dom.setSortOrder(srcDom.getSortOrder());
                domainRepository.save(dom);
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/superpower/modules/category/service/
git commit -m "feat(category): add CategoryService with tree, import, and copy"
```

---

### Task 4: 创建 CategoryController

**Files:**
- Create: `src/main/java/com/superpower/modules/category/controller/CategoryController.java`

- [ ] **Step 1: 确保目录存在**

Run: `mkdir -p src/main/java/com/superpower/modules/category/controller`

- [ ] **Step 2: 创建 CategoryController**

Create new file `src/main/java/com/superpower/modules/category/controller/CategoryController.java`:
```java
package com.superpower.modules.category.controller;

import com.superpower.common.Result;
import com.superpower.modules.category.service.CategoryService;
import com.superpower.modules.data.dto.TreeNodeDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/superpower/modules/category/controller/
git commit -m "feat(category): add CategoryController GET /api/tree"
```

---

### Task 5: 首次启动时导入 Excel

**Files:**
- Modify: `src/main/java/com/superpower/SuperPowerApplication.java`

- [ ] **Step 1: 阅读 SuperPowerApplication.java**

Read the existing file to understand the current structure.

- [ ] **Step 2: 添加启动后导入逻辑**

After the `SpringApplication.run()` call (or as a `@PostConstruct` or `CommandLineRunner`), invoke import if the category tables are empty.

In `SuperPowerApplication.java`, add a `CommandLineRunner` bean:

```java
import com.superpower.modules.category.service.CategoryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

// Inside the class, add:

@Bean
CommandLineRunner initCategories(CategoryService categoryService,
                                  com.superpower.modules.category.repository.BaseCategoryRepository catRepo,
                                  com.superpower.modules.version.repository.DataVersionRepository versionRepo,
                                  com.superpower.modules.version.service.DataVersionService versionService) {
    return args -> {
        // Only import if no categories exist
        if (catRepo.count() == 0) {
            // Ensure version 1 exists (first version)
            java.util.List<com.superpower.modules.version.entity.DataVersion> versions = versionRepo.findAll();
            Long versionId;
            if (versions.isEmpty()) {
                var v = versionService.createVersion();
                versionId = v.getId();
            } else {
                versionId = versions.get(0).getId();
            }
            try {
                categoryService.importFromExcel(versionId, "docs/添翼产品清单.xlsx");
                System.out.println("Excel categories imported successfully");
            } catch (Exception e) {
                System.err.println("Failed to import categories from Excel: " + e.getMessage());
            }
        }
    };
}
```

- [ ] **Step 3: 编译并重启验证**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/superpower/SuperPowerApplication.java
git commit -m "feat(category): auto-import Excel categories on first startup"
```

---

### Task 6: 修改 DataVersionService 支持分类复制

**Files:**
- Modify: `src/main/java/com/superpower/modules/version/service/DataVersionService.java`

- [ ] **Step 1: 阅读 DataVersionService.java**

Read the file, find the `createVersion()` method. It already copies DataEntry records. We need to also copy categories/domains.

- [ ] **Step 2: 添加分类复制逻辑**

At the end of `createVersion()`, after copying DataEntry records, add:

```java
// 复制分类数据
CategoryService categoryService = // need to inject
```

Since `DataVersionService` uses explicit constructor injection, add `CategoryService` to the constructor and call `categoryService.copyFromVersion(latest.getId(), version.getId())` at the end of `createVersion()`.

Modified constructor:
```java
private final DataVersionRepository versionRepository;
private final DataEntryRepository entryRepository;
private final CategoryService categoryService;

public DataVersionService(DataVersionRepository versionRepository,
                          DataEntryRepository entryRepository,
                          CategoryService categoryService) {
    this.versionRepository = versionRepository;
    this.entryRepository = entryRepository;
    this.categoryService = categoryService;
}
```

Add at the end of `createVersion()` method, after the parent ID remapping block:
```java
// 复制分类数据到新版本
categoryService.copyFromVersion(latest.getId(), version.getId());
```

Also add import:
```java
import com.superpower.modules.category.service.CategoryService;
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/superpower/modules/version/service/DataVersionService.java
git commit -m "feat(version): copy category data when creating new version"
```

---

### Task 7: 修改前端接口和数据展示

**Files:**
- Modify: `frontend/src/api/data.js`
- Modify: `frontend/src/components/TreePanel.vue`

- [ ] **Step 1: 修改 data.js 中的 getTree**

Read `frontend/src/api/data.js`. Change the `getTree` function to call the new endpoint:

```js
export function getTree(versionId) {
  return request.get('/tree', { params: { versionId } })
}
```

- [ ] **Step 2: 修改 TreePanel.vue 适配新数据格式**

Read `frontend/src/components/TreePanel.vue`. The new tree returns `TreeNodeDTO` objects with `id`, `label`, `level`, `children`, `parentId`. The existing TreePanel already uses these fields. The key change is that `id` values from the new tree will be category/domain IDs (not DataEntry IDs).

Verify that the `onNodeClick` logic with `findAncestor` works with the new `parentId` structure. The `parentId` for domain nodes points to the category ID, and `parentId` for category nodes is null. The `findAncestor` function walks up via `parentId` through `nodeMap`.

The TreePanel should work without changes since both old and new APIs return `TreeNodeDTO[]` with the same fields.

If any adaptation needed, ensure the tree still emits `categoryLabel` and `domainLabel` via the `findAncestor` logic.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/data.js
git commit -m "feat(frontend): call new /api/tree endpoint"
```

---

### Task 8: 运行完整验证

**Files:**
- Test: 全量验证

- [ ] **Step 1: 运行后端编译打包**

Run: `mvn package`
Expected: BUILD SUCCESS

- [ ] **Step 2: 运行前端构建**

Run: `npm run build`
Workdir: `frontend/`
Expected: BUILD SUCCESS

- [ ] **Step 3: 重启后端并验证**

```bash
# Kill existing process
lsof -ti:8080 | xargs kill -9 2>/dev/null
# Start new instance (first startup will import Excel)
mvn spring-boot:run
```

- [ ] **Step 4: 验证API**

```bash
curl -s "http://localhost:8080/api/tree?versionId=1"
```
Expected: JSON array of TreeNodeDTO with categories and domains

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "chore: full verification build and test"
```

---

### Task 9: 人工验收

- [ ] **Step 1: 基础验收**

```
1. 启动应用（首次启动自动从Excel导入分类数据）。
2. 打开前端页面，验证左侧树显示正确的业务分类和域。
3. 选择不同分类/域，确认右侧数据清单正常过滤。
4. 创建新版本，验证新版本的分类树继承自上一版本。
```

- [ ] **Step 2: 边界场景**

```
1. 重启应用后分类数据持久化（不重复导入）。
2. 删除数据库中所有分类后重启，应重新导入。
3. 在多版本间切换，树正确更新。
```

---

## 实施总结

**涉及文件：** 8个新建文件 + 3个修改文件。覆盖 entity/repository/service/controller 四层 + 前端 API/组件。
