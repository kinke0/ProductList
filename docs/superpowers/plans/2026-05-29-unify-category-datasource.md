# 统一分类数据源修复计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 统一所有组件的L1/L2数据源为 base_category/base_domain 维度表，消除名称和排序不同步问题。

**Architecture:** base_category/base_domain 作为唯一维度表存储分类/业务域的名称和排序。DataEntry 通过 categoryId/domainId 外键关联。所有前端组件统一从 CategoryService.getTree() (GET /api/tree) 获取树结构。显示名称通过ID查维度表获取。

**Tech Stack:** Spring Boot + JPA (后端), Vue 3 + Element Plus (前端)

---

## 问题概述

当前4处组件读不同数据源导致不同步：

| 组件 | 当前数据源 | L1名称字段 | L2名称字段 |
|---|---|---|---|
| 图床层级树 | data_entry表 | colBizCategory | colBizDomain |
| 清单层级树(TreePanel) | data_entry表 | colProductSystem/label | colProductSystem/label |
| 清单分隔行 | data_entry表 | colBizCategory | colBizDomain |
| 表单下拉框 | base_category/base_domain表 | name | name |

## 修复后统一为

| 组件 | 数据源 | L1名称 | L2名称 | 排序 |
|---|---|---|---|---|
| 所有组件 | base_category/base_domain | BaseCategory.name | BaseDomain.name | BaseCategory/BaseDomain.sortOrder |

---

## File Structure

### 后端修改
- 修改: `src/main/java/com/superpower/modules/category/controller/CategoryController.java` — 增加CRUD端点
- 修改: `src/main/java/com/superpower/modules/category/service/CategoryService.java` — 增加CRUD方法
- 修改: `src/main/java/com/superpower/modules/data/service/DataEntryService.java` — getTree/sortByCategoryOrder改用维度表
- 修改: `src/main/java/com/superpower/modules/image/service/ImageResourceService.java` — getTree改用维度表
- 修改: `src/main/java/com/superpower/modules/data/service/DataEntryService.java` — cascadeLabelUpdate改为同步更新维度表+data_entry

### 前端修改
- 修改: `frontend/src/components/TreePanel.vue` — 改用getCategoryTree API
- 修改: `frontend/src/views/system/BaseDataManage.vue` — 改用CategoryService的CRUD API
- 修改: `frontend/src/api/data.js` — 无需修改，已有getCategoryTree
- 修改: `frontend/src/api/category.js` — 新建，提供Category CRUD API
- 修改: `frontend/src/views/system/ImageGallery.vue` — 树的数据源改用CategoryService

---

### Task 1: 后端 — CategoryService 增加 CRUD API

**Files:**
- Modify: `src/main/java/com/superpower/modules/category/controller/CategoryController.java`
- Modify: `src/main/java/com/superpower/modules/category/service/CategoryService.java`
- Modify: `src/main/java/com/superpower/modules/category/repository/BaseCategoryRepository.java`
- Modify: `src/main/java/com/superpower/modules/category/repository/BaseDomainRepository.java`

- [ ] **Step 1: 在 CategoryService 中增加 CRUD 方法**

在 `CategoryService.java` 中添加以下方法：

```java
// 在现有方法之后添加

public BaseCategory getCategoryById(Long id) {
    return categoryRepository.findById(id)
        .orElseThrow(() -> new BusinessException("分类不存在: " + id));
}

public BaseDomain getDomainById(Long id) {
    return domainRepository.findById(id)
        .orElseThrow(() -> new BusinessException("业务域不存在: " + id));
}

@Transactional
public BaseCategory createCategory(Long versionId, String name) {
    long count = categoryRepository.findByVersionIdOrderBySortOrderAsc(versionId).size();
    BaseCategory cat = new BaseCategory();
    cat.setVersionId(versionId);
    cat.setName(name);
    cat.setSortOrder((int) count);
    return categoryRepository.save(cat);
}

@Transactional
public BaseCategory updateCategory(Long id, String name) {
    BaseCategory cat = getCategoryById(id);
    String oldName = cat.getName();
    cat.setName(name);
    categoryRepository.save(cat);
    // 同步更新 data_entry 中所有引用旧名称的记录
    List<DataEntry> entries = dataEntryRepository.findByVersionIdAndColBizCategory(cat.getVersionId(), oldName);
    for (DataEntry e : entries) {
        e.setColBizCategory(name);
        if (e.getColProductSystem() != null && e.getColProductSystem().equals(oldName)) {
            e.setColProductSystem(name);
        }
        dataEntryRepository.save(e);
    }
    return cat;
}

@Transactional
public void deleteCategory(Long id) {
    BaseCategory cat = getCategoryById(id);
    long domainCount = domainRepository.findByVersionIdAndCategoryIdOrderBySortOrderAsc(cat.getVersionId(), cat.getId()).size();
    if (domainCount > 0) {
        throw new BusinessException("该分类下存在业务域，不可删除");
    }
    // 同时删除 data_entry 中对应的 level=1 记录
    dataEntryRepository.findByVersionIdAndLevelAndColBizCategory(cat.getVersionId(), 1, cat.getName())
        .forEach(e -> dataEntryRepository.delete(e));
    categoryRepository.delete(cat);
}

@Transactional
public BaseDomain createDomain(Long versionId, Long categoryId, String name) {
    BaseCategory cat = getCategoryById(categoryId);
    long count = domainRepository.findByVersionIdAndCategoryIdOrderBySortOrderAsc(versionId, categoryId).size();
    BaseDomain dom = new BaseDomain();
    dom.setVersionId(versionId);
    dom.setCategoryId(categoryId);
    dom.setName(name);
    dom.setSortOrder((int) count);
    domainRepository.save(dom);
    // 同时在 data_entry 中创建对应的 level=2 记录
    DataEntry entry = new DataEntry();
    entry.setVersionId(versionId);
    entry.setLevel(2);
    entry.setColBizCategory(cat.getName());
    entry.setColBizDomain(name);
    entry.setColProductSystem(name);
    entry.setCategoryId(categoryId);
    entry.setDomainId(dom.getId());
    entry.setIsLeaf(true);
    List<DataEntry> siblings = dataEntryRepository.findByVersionIdAndParentId(versionId, findL1EntryId(versionId, cat.getName()));
    int maxSort = siblings.stream().mapToInt(e -> e.getSortOrder() != null ? e.getSortOrder() : 0).max().orElse(-1);
    entry.setSortOrder(maxSort + 1);
    dataEntryRepository.save(entry);
    return dom;
}

@Transactional
public BaseDomain updateDomain(Long id, String name) {
    BaseDomain dom = getDomainById(id);
    String oldName = dom.getName();
    dom.setName(name);
    domainRepository.save(dom);
    // 同步更新 data_entry 中所有引用旧名称的记录
    List<DataEntry> entries = dataEntryRepository.findByVersionIdAndColBizDomain(dom.getVersionId(), oldName);
    for (DataEntry e : entries) {
        e.setColBizDomain(name);
        if (e.getColProductSystem() != null && e.getColProductSystem().equals(oldName)) {
            e.setColProductSystem(name);
        }
        dataEntryRepository.save(e);
    }
    return dom;
}

@Transactional
public void deleteDomain(Long id) {
    BaseDomain dom = getDomainById(id);
    dataEntryRepository.findByVersionIdAndLevelAndColBizDomain(dom.getVersionId(), 2, dom.getName())
        .forEach(e -> dataEntryRepository.delete(e));
    domainRepository.delete(dom);
}
```

**重要依赖：** CategoryService 需要注入 DataEntryRepository 和 DataEntry。需要添加以下 import 和注入：

```java
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
```

构造函数增加 `DataEntryRepository dataEntryRepository` 参数。

还需要一个辅助方法 `findL1EntryId`:
```java
private Long findL1EntryId(Long versionId, String categoryName) {
    List<DataEntry> l1Entries = dataEntryRepository.findByVersionIdAndLevel(versionId, 1);
    for (DataEntry e : l1Entries) {
        if (categoryName.equals(e.getColBizCategory()) || categoryName.equals(e.getColProductSystem())) {
            return e.getId();
        }
    }
    return null;
}
```

- [ ] **Step 2: 在 BaseCategoryRepository 和 BaseDomainRepository 中增加所需查询方法**

`BaseCategoryRepository.java` — 无需新增，现有方法够用。

`BaseDomainRepository.java` — 需要新增：
```java
List<BaseDomain> findByVersionId(Long versionId); // 已有
```

`DataEntryRepository.java` — 需要新增查询方法：
```java
List<DataEntry> findByVersionIdAndColBizCategory(Long versionId, String colBizCategory);
List<DataEntry> findByVersionIdAndColBizDomain(Long versionId, String colBizDomain);
List<DataEntry> findByVersionIdAndLevelAndColBizCategory(Long versionId, Integer level, String colBizCategory);
List<DataEntry> findByVersionIdAndLevelAndColBizDomain(Long versionId, Integer level, String colBizDomain);
```

- [ ] **Step 3: 在 CategoryController 中增加 CRUD 端点**

```java
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
```

- [ ] **Step 4: 编译后端验证**

Run: `cd /Users/craneking/workspace/工程设计/superPowerTest && mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 2: 后端 — DataEntryService.getTree 和 sortByCategoryOrder 改用维度表

**Files:**
- Modify: `src/main/java/com/superpower/modules/data/service/DataEntryService.java`

- [ ] **Step 1: 修改 getTree/buildTree 方法，L1/L2的label使用维度表名称**

当前 `buildTree` 第71行 `node.setLabel(entry.getColProductSystem() != null ? entry.getColProductSystem() : entry.getColBizCategory())` 改为通过 categoryId/domainId 查维度表获取名称。

但这样逐条查询效率低。改为在 `getTree` 方法中批量预加载维度表数据：

```java
public List<TreeNodeDTO> getTree(Long versionId, String name, String status, String productManager,
                                 String solution, String versionTag) {
    List<DataEntry> entries = entryRepository.findByVersionIdAndLevelWithFilter(
            versionId, 1, name, status, productManager, solution, versionTag);

    // 预加载维度表数据
    Map<Long, BaseCategory> catMap = new HashMap<>();
    Map<Long, BaseDomain> domMap = new HashMap<>();
    baseCategoryRepository.findByVersionIdOrderBySortOrderAsc(versionId)
        .forEach(c -> catMap.put(c.getId(), c));
    baseDomainRepository.findByVersionId(versionId)
        .forEach(d -> domMap.put(d.getId(), d));

    return entries.stream().map(e -> buildTree(e, versionId, catMap, domMap)).toList();
}
```

修改 `buildTree` 签名和逻辑：

```java
private TreeNodeDTO buildTree(DataEntry entry, Long versionId, Map<Long, BaseCategory> catMap, Map<Long, BaseDomain> domMap) {
    TreeNodeDTO node = new TreeNodeDTO();
    node.setId(entry.getId());
    node.setParentId(entry.getParentId());
    node.setLevel(entry.getLevel());

    // 通过维度表ID获取名称
    String label;
    if (entry.getLevel() == 1 && entry.getCategoryId() != null && catMap.containsKey(entry.getCategoryId())) {
        label = catMap.get(entry.getCategoryId()).getName();
    } else if (entry.getLevel() == 2 && entry.getDomainId() != null && domMap.containsKey(entry.getDomainId())) {
        label = domMap.get(entry.getDomainId()).getName();
    } else {
        label = entry.getColProductSystem() != null ? entry.getColProductSystem() : entry.getColBizCategory();
    }
    node.setLabel(label);
    node.setSortOrder(entry.getSortOrder());

    if (entry.getLevel() < 2) {
        node.setIsLeaf(false);
        List<DataEntry> children = entryRepository.findByVersionIdAndParentIdOrderBySortOrder(versionId, entry.getId());
        if (!children.isEmpty()) {
            node.setChildren(children.stream().map(c -> buildTree(c, versionId, catMap, domMap)).toList());
        }
    } else {
        node.setIsLeaf(true);
    }
    return node;
}
```

- [ ] **Step 2: 修改 sortByCategoryOrder 方法，L2排序改用 base_domain.sortOrder**

当前第134-136行用 data_entry(level=2) 的 sortOrder，改为用 base_domain.sortOrder：

```java
private List<DataEntry> sortByCategoryOrder(List<DataEntry> entries, Long versionId) {
    List<BaseCategory> cats = baseCategoryRepository.findByVersionIdOrderBySortOrderAsc(versionId);
    Map<String, Integer> catOrder = new LinkedHashMap<>();
    for (int i = 0; i < cats.size(); i++) catOrder.put(cats.get(i).getName(), i);

    // 改用 base_domain 的 sortOrder
    List<BaseDomain> domains = baseDomainRepository.findByVersionId(versionId);
    domains.sort(Comparator.comparingInt(d -> d.getSortOrder() != null ? d.getSortOrder() : 0));
    Map<String, Integer> l2Order = new LinkedHashMap<>();
    for (int i = 0; i < domains.size(); i++) l2Order.put(domains.get(i).getName(), i);

    entries.sort(Comparator.comparingInt((DataEntry e) -> catOrder.getOrDefault(e.getColBizCategory(), Integer.MAX_VALUE))
            .thenComparingInt(e -> l2Order.getOrDefault(e.getColBizDomain(), Integer.MAX_VALUE))
            .thenComparingInt(e -> e.getLevel() != null ? e.getLevel() : 3)
            .thenComparing(e -> e.getParentId(), Comparator.nullsLast(Long::compareTo))
            .thenComparingInt(e -> e.getSortOrder() != null ? e.getSortOrder() : 0));
    return entries;
}
```

- [ ] **Step 3: 编译后端验证**

Run: `cd /Users/craneking/workspace/工程设计/superPowerTest && mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 3: 后端 — ImageResourceService.getTree 改用维度表

**Files:**
- Modify: `src/main/java/com/superpower/modules/image/service/ImageResourceService.java`

- [ ] **Step 1: 修改 getTree 方法，分类和业务域从 base_category/base_domain 读取**

当前方法从 data_entry 提取 colBizCategory/colBizDomain 去重构建树（第298-323行），改为从维度表读取：

将第291-339行替换为：

```java
// 从维度表获取分类和业务域结构
List<BaseCategory> baseCategories = baseCategoryRepository.findByVersionIdOrderBySortOrderAsc(versionId);
Map<String, String> catNames = new LinkedHashMap<>();
Map<String, List<String>> catToDomains = new LinkedHashMap<>();
Map<String, List<String>> domToProducts = new LinkedHashMap<>();

for (BaseCategory cat : baseCategories) {
    String catName = cat.getName();
    catNames.put(catName, catName);

    List<BaseDomain> domains = baseDomainRepository.findByVersionIdAndCategoryIdOrderBySortOrderAsc(versionId, cat.getId());
    for (BaseDomain dom : domains) {
        String domName = dom.getName();
        catToDomains.computeIfAbsent(catName, k -> new ArrayList<>());
        String domListKey = catName + "||" + domName;
        if (catToDomains.get(catName).stream().noneMatch(d -> d.equals(domName))) {
            catToDomains.get(catName).add(domName);
        }

        // 产品系统从 data_entry 获取
        List<DataEntry> level3Entries = dataEntryRepository.findByVersionIdAndDomainIdAndLevel(versionId, dom.getId(), 3);
        for (DataEntry e : level3Entries) {
            String prod = e.getColProductSystem();
            if (prod != null && !prod.trim().isEmpty()) {
                prod = prod.trim();
                domToProducts.computeIfAbsent(domListKey, k -> new ArrayList<>());
                if (!domToProducts.get(domListKey).contains(prod)) {
                    domToProducts.get(domListKey).add(prod);
                }
            }
        }
    }
}

// 排序已由维度表的 sortOrder 保证，不需要额外排序逻辑
List<String> orderedCategories = new ArrayList<>(catNames.keySet());
```

需要新增 DataEntryRepository 方法：`findByVersionIdAndDomainIdAndLevel(Long versionId, Long domainId, Integer level)`

- [ ] **Step 2: 修改图床点击节点传参逻辑**

当前 ImageGallery.vue 第118-136行的 `onNodeClick` 通过 label 路径解析 category/domain。改为使用维度表的数据后，label 带数量后缀（如 "5. 智慧医疗 (3)"），需要去掉后缀再传参。

在后端 ImageResourceService.getTree 中，可以在 ImageDirectoryNode 中额外存储纯名称（不含数量后缀）。或者在前端 `onNodeClick` 中去掉后缀。

前端方案更简单：修改 `onNodeClick` 中的路径解析，去掉 ` (数字)` 后缀：

```javascript
function onNodeClick(data, node) {
  const path = []
  let n = node
  while (n && n.data && n.data.label) { path.unshift(n.data.label); n = n.parent }
  // 去掉数量后缀，如 "5. 智慧医疗 (3)" -> "5. 智慧医疗"
  const cleanPath = path.map(p => p.replace(/\s*\(\d+\)$/, ''))
  if (cleanPath.length >= 3) {
    selectedCategory.value = cleanPath[0]
    selectedDomain.value = cleanPath[1]
    selectedProduct.value = cleanPath[2]
  } else if (cleanPath.length === 2) {
    selectedCategory.value = cleanPath[0]
    selectedDomain.value = cleanPath[1]
    selectedProduct.value = null
  } else {
    selectedCategory.value = cleanPath[0]
    selectedDomain.value = null
    selectedProduct.value = null
  }
  selectedNode.value = data
  loadImages()
}
```

- [ ] **Step 3: 编译后端验证**

Run: `cd /Users/craneking/workspace/工程设计/superPowerTest && mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 4: 前端 — TreePanel 改用 getCategoryTree API

**Files:**
- Modify: `frontend/src/components/TreePanel.vue`

- [ ] **Step 1: 修改 TreePanel 的 API 调用**

当前第40行 `import { getTree } from '../api/data'`，改为从 category API 获取：

```javascript
import { getCategoryTree } from '../api/data'
```

第58行改为：
```javascript
const res = await getCategoryTree(val)
```

这样 TreePanel 显示的分类/业务域名称直接来自 base_category/base_domain，与表单下拉框完全一致。

- [ ] **Step 2: 验证前端构建**

Run: `cd /Users/craneking/workspace/工程设计/superPowerTest/frontend && npm run build 2>&1 | tail -5`
Expected: ✓ built in Xs

---

### Task 5: 前端 — BaseDataManage 改用 CategoryService CRUD API

**Files:**
- Create: `frontend/src/api/category.js`
- Modify: `frontend/src/views/system/BaseDataManage.vue`

- [ ] **Step 1: 创建 category.js API 文件**

```javascript
import request from '../utils/request'

export function getCategoryTree(versionId) {
  return request.get('/tree', { params: { versionId } })
}

export function createCategory(versionId, name) {
  return request.post('/category', { name }, { params: { versionId } })
}

export function updateCategory(id, name) {
  return request.put(`/category/${id}`, { name })
}

export function deleteCategory(id) {
  return request.delete(`/category/${id}`)
}

export function createDomain(versionId, categoryId, name) {
  return request.post('/domain', { name }, { params: { versionId, categoryId } })
}

export function updateDomain(id, name) {
  return request.put(`/domain/${id}`, { name })
}

export function deleteDomain(id) {
  return request.delete(`/domain/${id}`)
}

export function updateCategorySort(versionId, sortList) {
  return request.put('/category/sort', sortList, { params: { versionId } })
}
```

- [ ] **Step 2: 重写 BaseDataManage.vue 使用维度表API**

核心改动：
1. 导入改为 `import { getCategoryTree, createCategory, updateCategory, deleteCategory, createDomain, updateDomain, deleteDomain, updateCategorySort } from '../../api/category'`
2. `loadL1()` 调用 `getCategoryTree(versionId)` 而非 `getTree(versionId)`
3. L1数据结构变为 `{ id, label, sortOrder, children: [{ id, label, sortOrder, parentId }] }`
4. 保存L1时调用 `createCategory`/`updateCategory`
5. 保存L2时调用 `createDomain`/`updateDomain`
6. 删除L1调用 `deleteCategory`，删除L2调用 `deleteDomain`
7. 排序调用 `updateCategorySort`
8. L2列表从L1节点的children获取（已由CategoryService.getTree()返回）

新的 `loadL1`：
```javascript
async function loadL1() {
  const res = await getCategoryTree(versionId)
  l1List.value = res.data || []
}
```

新的 `loadL2`：
```javascript
async function loadL2(l1Id) {
  const l1 = l1List.value.find(c => c.id === l1Id)
  l2List.value = l1?.children || []
}
```

新的 `saveL1`：
```javascript
async function saveL1() {
  if (!l1Form.value.colBizCategory) {
    ElMessage.warning('请输入名称')
    return
  }
  if (isNewL1.value) {
    await createCategory(versionId, l1Form.value.colBizCategory)
    ElMessage.success('创建成功')
  } else {
    await updateCategory(editingL1Id.value, l1Form.value.colBizCategory)
    ElMessage.success('保存成功')
  }
  l1Dialog.value = false
  await loadL1()
}
```

新的 `saveL2`：
```javascript
async function saveL2() {
  if (!l2Form.value.colBizDomain) {
    ElMessage.warning('请输入名称')
    return
  }
  if (isNewL2.value) {
    await createDomain(versionId, selectedL1.value.id, l2Form.value.colBizDomain)
    ElMessage.success('创建成功')
  } else {
    await updateDomain(editingL2Id.value, l2Form.value.colBizDomain)
    ElMessage.success('保存成功')
  }
  l2Dialog.value = false
  await loadL1()
  await loadL2(selectedL1.value.id)
}
```

新的 `deleteL1`：
```javascript
async function deleteL1(row) {
  try {
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    if (selectedL1.value?.id === row.id) {
      selectedL1.value = null
      l2List.value = []
    }
    await loadL1()
  } catch (e) {
    const msg = e?.response?.data?.message || '删除失败'
    ElMessage.warning(msg)
  }
}
```

新的 `deleteL2`：
```javascript
async function deleteL2(row) {
  try {
    await deleteDomain(row.id)
    ElMessage.success('删除成功')
    await loadL1()
    await loadL2(selectedL1.value.id)
  } catch (e) {
    const msg = e?.response?.data?.message || '删除失败'
    ElMessage.warning(msg)
  }
}
```

新的排序（moveUp/moveDown/reorderList）改用 `updateCategorySort`：

```javascript
async function reorderList(type, fromIdx, toIdx) {
  const list = type === 'l1' ? l1List : l2List
  const items = [...list.value]
  const [moved] = items.splice(fromIdx, 1)
  items.splice(toIdx, 0, moved)
  const sortList = items.map((item, i) => ({
    type: type === 'l1' ? 'category' : 'domain',
    id: item.id,
    sortOrder: i
  }))
  await updateCategorySort(versionId, sortList)
  if (type === 'l1') await loadL1()
  else { await loadL1(); await loadL2(selectedL1.value?.id) }
}
```

模板中显示名称字段也需要调整。L1数据从 TreeNodeDTO 来，字段名是 `label` 而非 `colBizCategory`：

模板中第33行 `{{ row.colBizCategory || row.label }}` 改为 `{{ row.label }}`
模板中第74行 `{{ row.colBizDomain || row.label }}` 改为 `{{ row.label }}`
模板中第50行 `selectedL1.colBizCategory` 改为 `selectedL1.label`

openL1Dialog 中第266行 `l1Form.value = { colBizCategory: row.colBizCategory || row.label }` 改为 `l1Form.value = { colBizCategory: row.label }`
openL2Dialog 中第324行 `l2Form.value = { colBizDomain: row.colBizDomain || row.label }` 改为 `l2Form.value = { colBizDomain: row.label }`

L2创建时不再需要传 `colBizCategory: selectedL1.value.colBizCategory`，因为 CategoryService 会自动关联。

- [ ] **Step 3: 验证前端构建**

Run: `cd /Users/craneking/workspace/工程设计/superPowerTest/frontend && npm run build 2>&1 | tail -5`
Expected: ✓ built in Xs

---

### Task 6: 前端 — 分隔行名称同步修复

**Files:**
- Modify: `frontend/src/components/DataListTab.vue`

- [ ] **Step 1: 修改分隔行名称来源**

当前分隔行（第115行）显示 `row.colBizCategory - row.colBizDomain`，这来自 data_entry 表的冗余文本字段。

由于后端 `sortByCategoryOrder` 和 `getTree` 已改用维度表，而 data_entry 的 colBizCategory/colBizDomain 在更新时已通过级联同步保持一致，所以分隔行的名称应该是同步的。

但为了确保完全同步，后端的 CategoryService CRUD 方法（Task 1）在修改名称时已经同步更新了 data_entry 的 colBizCategory/colBizDomain。所以分隔行无需额外修改。

**验证点：** 确保 DataListTab 加载数据时，query API 返回的 DataEntry 的 colBizCategory/colBizDomain 与维度表名称一致。

---

### Task 7: 后端 — 修改 cascadeLabelUpdate 确保维度表为权威源

**Files:**
- Modify: `src/main/java/com/superpower/modules/data/service/DataEntryService.java`

- [ ] **Step 1: 修改 cascadeLabelUpdate 逻辑**

当前 cascadeLabelUpdate 在更新 data_entry 的 L1/L2 名称时会同步更新 base_category/base_domain。但方向应该是：维度表为权威源，data_entry 是从属。

由于 BaseDataManage 现在直接操作维度表（Task 5），cascadeLabelUpdate 的场景变为：当 L3+ 条目修改 colBizCategory/colBizDomain 时，需要反向同步到维度表。但这种场景应该被禁止（L3+ 不应修改分类/域名称）。

保留现有的级联逻辑不变（它在 DataEntryService.update 中被调用），因为其他地方（如 DataListTab 编辑）仍可能修改 data_entry 的分类/域名称。级联方向保持：data_entry 变更 -> 同步 base_category/base_domain。

---

### Task 8: 综合验证

- [ ] **Step 1: 编译后端**

Run: `cd /Users/craneking/workspace/工程设计/superPowerTest && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 构建前端**

Run: `cd /Users/craneking/workspace/工程设计/superPowerTest/frontend && npm run build`
Expected: ✓ built

- [ ] **Step 3: 重启后端服务**

Run: `cd /Users/craneking/workspace/工程设计/superPowerTest && mvn spring-boot:run -q &`
验证: `curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/`

- [ ] **Step 4: 重启前端服务**

Run: `cd /Users/craneking/workspace/工程设计/superPowerTest/frontend && npx vite --host &`
验证: `curl -s -o /dev/null -w "%{http_code}" http://localhost:5173/`

- [ ] **Step 5: 功能验证**

1. 业务分类维护页面：创建/编辑/删除 L1/L2，验证名称和排序生效
2. 数据清单页面：验证层级导航名称与业务分类维护一致
3. 数据清单页面：验证分隔行名称与业务分类维护一致
4. 编辑表单：验证 L1/L2 下拉框与业务分类维护一致
5. 图床页面：验证层级树名称与业务分类维护一致
