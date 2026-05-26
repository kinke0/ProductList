# 自定义清单Tab功能实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 数据清单Tab右侧新增"添加清单"按钮，创建自定义清单Tab，支持从数据清单插入条目到自定义清单，自定义清单拥有完整的数据维护功能。

**Architecture:** 后端新增 CustomTab/CustomTabEntry 实体和 CRUD API；DataEntryRepository.queryEntries 扩展 customTabId 过滤；前端 CustomTabAPI 封装，DataListTab "生成文档"改为"插入待生成清单"，DataWorkbench 动态渲染自定义Tab。

**Tech Stack:** Spring Boot 3.2.0, Spring Data JPA, SQLite, Vue 3, Element Plus, SortableJS

---

## File Structure Map

### 后端新增
- Create: `src/main/java/com/superpower/modules/customtab/entity/CustomTab.java`
- Create: `src/main/java/com/superpower/modules/customtab/entity/CustomTabEntry.java`
- Create: `src/main/java/com/superpower/modules/customtab/entity/CustomTabEntryId.java`
- Create: `src/main/java/com/superpower/modules/customtab/repository/CustomTabRepository.java`
- Create: `src/main/java/com/superpower/modules/customtab/repository/CustomTabEntryRepository.java`
- Create: `src/main/java/com/superpower/modules/customtab/service/CustomTabService.java`
- Create: `src/main/java/com/superpower/modules/customtab/controller/CustomTabController.java`

### 后端修改
- Modify: `src/main/java/com/superpower/modules/data/repository/DataEntryRepository.java` — queryEntries 新增 customTabId 参数
- Modify: `src/main/java/com/superpower/modules/data/service/DataEntryService.java` — query 方法新增 customTabId 参数
- Modify: `src/main/java/com/superpower/modules/data/controller/DataEntryController.java` — /query/{versionId} 新增 customTabId 参数

### 前端新增
- Create: `frontend/src/api/customTab.js`

### 前端修改
- Modify: `frontend/src/api/data.js` — queryEntries 新增 customTabId 参数
- Modify: `frontend/src/components/DataListTab.vue` — "生成文档" → "插入待生成清单"，emit 变更，新增 customTabId prop
- Modify: `frontend/src/views/dashboard/DataWorkbench.vue` — 动态Tab渲染，"添加清单"按钮，插入弹窗

### 测试
- Create: `src/test/java/com/superpower/modules/customtab/service/CustomTabServiceTest.java`

### 验证
- Run: `mvn test`
- Run: `mvn package`
- Run: `npm run build`

---

### Task 1: 创建实体类

**Files:**
- Create: `src/main/java/com/superpower/modules/customtab/entity/CustomTab.java`
- Create: `src/main/java/com/superpower/modules/customtab/entity/CustomTabEntry.java`
- Create: `src/main/java/com/superpower/modules/customtab/entity/CustomTabEntryId.java`

- [ ] **Step 1: 创建 Composite Key 类 CustomTabEntryId**

Create new file:
```java
package com.superpower.modules.customtab.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomTabEntryId implements Serializable {
    private Long customTabId;
    private Long entryId;
}
```

- [ ] **Step 2: 创建 CustomTab 实体**

Create new file:
```java
package com.superpower.modules.customtab.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "custom_tab")
public class CustomTab {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

- [ ] **Step 3: 创建 CustomTabEntry 关联实体**

Create new file:
```java
package com.superpower.modules.customtab.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "custom_tab_entry")
@IdClass(com.superpower.modules.customtab.entity.CustomTabEntryId.class)
public class CustomTabEntry {
    @Id
    @Column(name = "custom_tab_id", nullable = false)
    private Long customTabId;

    @Id
    @Column(name = "entry_id", nullable = false)
    private Long entryId;
}
```

- [ ] **Step 4: 确保模块目录存在**

Run: `mkdir -p src/main/java/com/superpower/modules/customtab/{entity,repository,service,controller}`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/superpower/modules/customtab/
git commit -m "feat(customtab): add CustomTab and CustomTabEntry entities"
```

---

### Task 2: 创建 Repository 层

**Files:**
- Create: `src/main/java/com/superpower/modules/customtab/repository/CustomTabRepository.java`
- Create: `src/main/java/com/superpower/modules/customtab/repository/CustomTabEntryRepository.java`

- [ ] **Step 1: 创建 CustomTabRepository**

Create new file:
```java
package com.superpower.modules.customtab.repository;

import com.superpower.modules.customtab.entity.CustomTab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomTabRepository extends JpaRepository<CustomTab, Long> {
    List<CustomTab> findByVersionIdOrderByCreatedAtAsc(Long versionId);
    boolean existsByVersionIdAndName(Long versionId, String name);
}
```

- [ ] **Step 2: 创建 CustomTabEntryRepository**

Create new file:
```java
package com.superpower.modules.customtab.repository;

import com.superpower.modules.customtab.entity.CustomTabEntry;
import com.superpower.modules.customtab.entity.CustomTabEntryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomTabEntryRepository extends JpaRepository<CustomTabEntry, CustomTabEntryId> {
    List<CustomTabEntry> findByCustomTabId(Long customTabId);
    void deleteByCustomTabId(Long customTabId);

    @Modifying
    @Query("DELETE FROM CustomTabEntry e WHERE e.customTabId = :tabId AND e.entryId = :entryId")
    void deleteByCustomTabIdAndEntryId(@Param("tabId") Long tabId, @Param("entryId") Long entryId);
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/superpower/modules/customtab/repository/
git commit -m "feat(customtab): add CustomTab repository layer"
```

---

### Task 3: 创建 CustomTabService

**Files:**
- Create: `src/main/java/com/superpower/modules/customtab/service/CustomTabService.java`

- [ ] **Step 1: 实现 CustomTabService**

Create new file:
```java
package com.superpower.modules.customtab.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.customtab.entity.CustomTab;
import com.superpower.modules.customtab.entity.CustomTabEntry;
import com.superpower.modules.customtab.repository.CustomTabEntryRepository;
import com.superpower.modules.customtab.repository.CustomTabRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomTabService {

    private final CustomTabRepository customTabRepository;
    private final CustomTabEntryRepository customTabEntryRepository;

    public CustomTabService(CustomTabRepository customTabRepository,
                            CustomTabEntryRepository customTabEntryRepository) {
        this.customTabRepository = customTabRepository;
        this.customTabEntryRepository = customTabEntryRepository;
    }

    public List<CustomTab> findByVersionId(Long versionId) {
        return customTabRepository.findByVersionIdOrderByCreatedAtAsc(versionId);
    }

    @Transactional
    public CustomTab create(String name, Long versionId, Long userId) {
        if (customTabRepository.existsByVersionIdAndName(versionId, name)) {
            throw new BusinessException("清单名称已存在");
        }
        CustomTab tab = new CustomTab();
        tab.setName(name);
        tab.setVersionId(versionId);
        tab.setUserId(userId);
        return customTabRepository.save(tab);
    }

    @Transactional
    public void delete(Long id) {
        customTabEntryRepository.deleteByCustomTabId(id);
        customTabRepository.deleteById(id);
    }

    public CustomTab getById(Long id) {
        return customTabRepository.findById(id)
                .orElseThrow(() -> new BusinessException("清单不存在"));
    }

    @Transactional
    public void addEntries(Long tabId, List<Long> entryIds) {
        getById(tabId);
        for (Long entryId : entryIds) {
            CustomTabEntry entry = new CustomTabEntry();
            entry.setCustomTabId(tabId);
            entry.setEntryId(entryId);
            customTabEntryRepository.save(entry);
        }
    }

    @Transactional
    public void removeEntry(Long tabId, Long entryId) {
        customTabEntryRepository.deleteByCustomTabIdAndEntryId(tabId, entryId);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/superpower/modules/customtab/service/
git commit -m "feat(customtab): add CustomTabService with CRUD operations"
```

---

### Task 4: 创建 CustomTabController

**Files:**
- Create: `src/main/java/com/superpower/modules/customtab/controller/CustomTabController.java`

- [ ] **Step 1: 实现 CustomTabController**

Create new file:
```java
package com.superpower.modules.customtab.controller;

import com.superpower.common.Result;
import com.superpower.modules.customtab.entity.CustomTab;
import com.superpower.modules.customtab.service.CustomTabService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/custom-tab")
public class CustomTabController {

    private final CustomTabService customTabService;

    public CustomTabController(CustomTabService customTabService) {
        this.customTabService = customTabService;
    }

    @GetMapping("/{versionId}")
    public Result<List<CustomTab>> list(@PathVariable Long versionId) {
        return Result.success(customTabService.findByVersionId(versionId));
    }

    @PostMapping
    public Result<CustomTab> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Long versionId = Long.valueOf(body.get("versionId").toString());
        Long userId = null;
        if (body.get("userId") != null) {
            userId = Long.valueOf(body.get("userId").toString());
        }
        return Result.success(customTabService.create(name, versionId, userId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        customTabService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/entries")
    public Result<Void> addEntries(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) body.get("entryIds");
        List<Long> entryIds = rawIds.stream().map(Long::valueOf).collect(java.util.stream.Collectors.toList());
        customTabService.addEntries(id, entryIds);
        return Result.success();
    }

    @DeleteMapping("/{id}/entries/{entryId}")
    public Result<Void> removeEntry(@PathVariable Long id, @PathVariable Long entryId) {
        customTabService.removeEntry(id, entryId);
        return Result.success();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/superpower/modules/customtab/controller/
git commit -m "feat(customtab): add CustomTabController REST API"
```

---

### Task 5: 扩展 DataEntryRepository 查询支持 customTabId

**Files:**
- Modify: `src/main/java/com/superpower/modules/data/repository/DataEntryRepository.java`

- [ ] **Step 1: 修改 queryEntries 方法签名，新增 customTabId 参数**

Replace the `queryEntries` method declaration:
```java
    @Query("SELECT e FROM DataEntry e WHERE e.versionId = :versionId " +
           "AND e.level >= 3 " +
           "AND (:customTabId IS NULL OR e.id IN (SELECT ce.entryId FROM CustomTabEntry ce WHERE ce.customTabId = :customTabId)) " +
           "AND (:name IS NULL OR e.colProductSystem LIKE %:name% " +
           "     OR e.colBizCategory LIKE %:name% " +
           "     OR e.colBizDomain LIKE %:name%) " +
           "AND (:status IS NULL OR e.colStatus = :status) " +
           "AND (:pm IS NULL OR e.colProductManager LIKE %:pm%) " +
           "AND (:solution IS NULL OR e.colOtherSolutionTag = :solution) " +
           "AND (:versionDivision IS NULL OR e.colVersionDivision LIKE %:versionDivision%) " +
           "AND (:bizCategory IS NULL OR e.colBizCategory = :bizCategory) " +
           "AND (:bizDomain IS NULL OR e.colBizDomain = :bizDomain) " +
           "ORDER BY e.level, e.sortOrder")
    List<DataEntry> queryEntries(@Param("versionId") Long versionId,
                                 @Param("customTabId") Long customTabId,
                                 @Param("name") String name,
                                 @Param("status") String status,
                                 @Param("pm") String productManager,
                                 @Param("solution") String solution,
                                 @Param("versionDivision") String versionDivision,
                                 @Param("bizCategory") String bizCategory,
                                 @Param("bizDomain") String bizDomain);
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/superpower/modules/data/repository/DataEntryRepository.java
git commit -m "feat(data): extend queryEntries with customTabId filtering"
```

---

### Task 6: 修改 DataEntryService 透传 customTabId

**Files:**
- Modify: `src/main/java/com/superpower/modules/data/service/DataEntryService.java`

- [ ] **Step 1: 修改 query 方法签名，新增 customTabId 参数**

Replace the `query` method:
```java
    public List<DataEntry> query(Long versionId, Long customTabId, String name, String status, String productManager,
                                 String solution, String versionDivision, String bizCategory, String bizDomain) {
        return entryRepository.queryEntries(versionId, customTabId, name, status, productManager,
                solution, versionDivision, bizCategory, bizDomain);
    }
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/superpower/modules/data/service/DataEntryService.java
git commit -m "feat(data): forward customTabId in DataEntryService.query"
```

---

### Task 7: 修改 DataEntryController 接收 customTabId

**Files:**
- Modify: `src/main/java/com/superpower/modules/data/controller/DataEntryController.java`

- [ ] **Step 1: 修改 /query/{versionId} 方法，新增 customTabId 参数**

Replace the `query` method:
```java
    @GetMapping("/query/{versionId}")
    public Result<List<DataEntry>> query(
            @PathVariable Long versionId,
            @RequestParam(required = false) Long customTabId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String productManager,
            @RequestParam(required = false) String solution,
            @RequestParam(required = false) String versionTag,
            @RequestParam(required = false) String bizCategory,
            @RequestParam(required = false) String bizDomain) {
        return Result.success(dataEntryService.query(versionId, customTabId, name, status, productManager,
                solution, versionTag, bizCategory, bizDomain));
    }
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/superpower/modules/data/controller/DataEntryController.java
git commit -m "feat(data): accept customTabId in DataEntryController.query"
```

---

### Task 8: 后端编译验证

**Files:**
- Test: 后端编译

- [ ] **Step 1: 编译后端确保无误**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 2: Commit**

```bash
git add .
git commit -m "build: verify backend compilation for customtab feature"
```

---

### Task 9: 创建前端 API

**Files:**
- Create: `frontend/src/api/customTab.js`
- Modify: `frontend/src/api/data.js`

- [ ] **Step 1: 创建 customTab.js API**

Create new file:
```js
import request from '../utils/request'

export function getCustomTabs(versionId) {
  return request.get(`/custom-tab/${versionId}`)
}

export function createCustomTab(data) {
  return request.post('/custom-tab', data)
}

export function deleteCustomTab(id) {
  return request.delete(`/custom-tab/${id}`)
}

export function addEntriesToTab(tabId, entryIds) {
  return request.post(`/custom-tab/${tabId}/entries`, { entryIds })
}

export function removeEntryFromTab(tabId, entryId) {
  return request.delete(`/custom-tab/${tabId}/entries/${entryId}`)
}
```

- [ ] **Step 2: 修改 data.js 的 queryEntries 支持 customTabId**

In `frontend/src/api/data.js`, the `queryEntries` function already accepts `params` and passes them as query params — no change needed. The new `customTabId` param will be passed through automatically by the caller.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/customTab.js
git commit -m "feat(frontend): add customTab API module"
```

---

### Task 10: 修改 DataListTab 组件

**Files:**
- Modify: `frontend/src/components/DataListTab.vue`

- [ ] **Step 1: 新增 customTabId prop**

Add after `isEditing` prop definition (around line 382):
```js
const props = defineProps({
  versionId: { type: Number, required: true },
  selectedNode: { type: Object, default: null },
  isEditing: { type: Boolean, default: true },
  customTabId: { type: Number, default: null }
})
```

- [ ] **Step 2: 修改 emit 从 generateDoc 改为 insertToList**

Replace the emit definition:
```js
const emit = defineEmits(['insertToList'])
```

- [ ] **Step 3: 修改 handleQuery 使用 customTabId**

Replace the `handleQuery` function body:
```js
async function handleQuery() {
  try {
    const res = await queryEntries(props.versionId, {
      customTabId: props.customTabId || undefined,
      name: queryForm.name || undefined,
      status: queryForm.status || undefined,
      productManager: queryForm.productManager || undefined,
      solution: queryForm.solution || undefined,
      versionTag: queryForm.versionDiv || undefined,
      bizCategory: props.selectedNode?.id !== 'all' ? (props.selectedNode?.categoryLabel || undefined) : undefined,
      bizDomain: props.selectedNode?.id !== 'all' ? (props.selectedNode?.domainLabel || undefined) : undefined
    })
    const entries = res.data || []
    tableData.value = buildTree(entries)
    nextTick(initSortable)
  } catch (e) {
    console.error('查询数据失败:', e)
    tableData.value = []
  }
}
```

- [ ] **Step 4: 将"生成文档"按钮改为"插入待生成清单"**

Replace the toolbar-right section (lines 37-41):
```vue
      <div class="toolbar-right">
        <el-button v-if="props.selectedNode?.level === 2" type="primary" size="small" :disabled="!props.isEditing" @click="openNewDialog">新建</el-button>
        <el-button type="success" size="small" @click="emit('insertToList', selectedIds)">
          插入待生成清单
        </el-button>
      </div>
```

- [ ] **Step 5: 移除不再使用的 generateDoc emit 引用（如果存在）**

Note: `emit('generateDoc', selectedIds)` 已替换为 `emit('insertToList', selectedIds)`，无需额外操作。

- [ ] **Step 6: Commit**

```bash
git add frontend/src/components/DataListTab.vue
git commit -m "feat(frontend): replace generateDoc with insertToList in DataListTab"
```

---

### Task 11: 修改 DataWorkbench 动态Tab渲染

**Files:**
- Modify: `frontend/src/views/dashboard/DataWorkbench.vue`

- [ ] **Step 1: 替换 el-tabs 为动态渲染**

Replace the right-panel tab section (lines 54-67):
```vue
      <div class="right-panel">
        <div class="tabs-wrapper">
          <el-tabs v-model="activeTab" style="height: 100%; display: flex; flex-direction: column;">
            <el-tab-pane label="统计视图" name="stats">
              <StatsTab :version-id="selectedVersion.id" />
            </el-tab-pane>
            <el-tab-pane label="数据清单" name="list">
              <DataListTab
                :version-id="selectedVersion.id"
                :selected-node="selectedNode"
                :is-editing="selectedVersion.status === 'draft'"
                @insert-to-list="onInsertToList"
              />
            </el-tab-pane>
            <el-tab-pane
              v-for="tab in customTabs"
              :key="'custom-' + tab.id"
              :label="tab.name"
              :name="'custom-' + tab.id"
              :closable="true"
              @tab-remove="onRemoveTab(tab)"
            >
              <DataListTab
                :version-id="selectedVersion.id"
                :selected-node="null"
                :is-editing="selectedVersion.status === 'draft'"
                :custom-tab-id="tab.id"
                @insert-to-list="onInsertToList"
              />
            </el-tab-pane>
          </el-tabs>
          <el-button class="add-list-btn" size="small" @click="onAddList">添加清单</el-button>
        </div>
      </div>
```

- [ ] **Step 2: 新增 script 中的数据和逻辑**

Replace the script section with updated content (add new imports, data, and functions):

Add new import after existing imports:
```js
import { getCustomTabs, createCustomTab, deleteCustomTab, addEntriesToTab } from '../../api/customTab'
```

Add new refs after selectedNode:
```js
const customTabs = ref([])
const showInsertDialog = ref(false)
const insertEntryIds = ref([])
```

Update onMounted to load custom tabs:
```js
onMounted(async () => {
  try {
    const res = await getVersions()
    versions.value = res.data || []
  } catch (e) {
    console.error('加载版本列表失败:', e)
  }
})
```

Add watch for selectedVersion to reload custom tabs:
```js
watch(selectedVersion, async (version) => {
  if (version) {
    await loadCustomTabs()
  }
})
```

Add new functions after onTreeSelect:
```js
async function loadCustomTabs() {
  if (!selectedVersion.value) return
  try {
    const res = await getCustomTabs(selectedVersion.value.id)
    customTabs.value = res.data || []
  } catch (e) {
    console.error('加载自定义清单失败:', e)
  }
}

async function onAddList() {
  try {
    const { value } = await ElMessageBox.prompt('请输入清单名称', '添加清单', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValidator: (val) => val && val.trim() ? true : '名称不能为空'
    })
    if (value) {
      await createCustomTab({
        name: value.trim(),
        versionId: selectedVersion.value.id
      })
      ElMessage.success('清单创建成功')
      await loadCustomTabs()
      if (customTabs.value.length > 0) {
        const last = customTabs.value[customTabs.value.length - 1]
        activeTab.value = 'custom-' + last.id
      }
    }
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e?.response?.data?.message || '创建失败')
    }
  }
}

async function onRemoveTab(tab) {
  try {
    await ElMessageBox.confirm(`确认删除清单"${tab.name}"？`, '确认', { type: 'warning' })
    await deleteCustomTab(tab.id)
    ElMessage.success('已删除')
    activeTab.value = 'list'
    await loadCustomTabs()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

function onInsertToList(entryIds) {
  if (!entryIds || entryIds.length === 0) {
    ElMessage.warning('请先勾选条目')
    return
  }
  if (customTabs.value.length === 0) {
    ElMessage.warning('请先创建自定义清单')
    return
  }
  insertEntryIds.value = entryIds
  showInsertDialog.value = true
}

async function onConfirmInsert(selectedTabId) {
  try {
    await addEntriesToTab(selectedTabId, insertEntryIds.value)
    ElMessage.success('插入成功')
    showInsertDialog.value = false
  } catch (e) {
    ElMessage.error('插入失败')
  }
}
```

- [ ] **Step 3: 添加插入目标清单选择弹窗（template 中添加）**

Add after version switch dialog (after `</el-dialog>` on line 48):
```vue
    <el-dialog v-model="showInsertDialog" title="选择目标清单" width="400px">
      <el-table :data="customTabs" highlight-current-row @current-change="onSelectInsertTarget" style="cursor:pointer;">
        <el-table-column prop="name" label="清单名称" />
      </el-table>
      <template #footer>
        <el-button @click="showInsertDialog = false">取消</el-button>
      </template>
    </el-dialog>
```

- [ ] **Step 4: 添加 onSelectInsertTarget 方法**

Add after `onConfirmInsert`:
```js
const insertTargetTabId = ref(null)

function onSelectInsertTarget(tab) {
  if (tab) {
    onConfirmInsert(tab.id)
  }
}
```

- [ ] **Step 5: 添加样式**

Add after `.readonly-tip ` style:
```css
.tabs-wrapper {
  position: relative;
  height: 100%;
  display: flex;
}
.tabs-wrapper :deep(.el-tabs) {
  flex: 1;
}
.add-list-btn {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 1;
}
```

- [ ] **Step 6: 处理 DataWorkbench 自身 generateDoc 调用兼容**

原有的 `onGenerateDoc` 函数（响应 dataList tab 的 generateDoc emit）已不再需要，因为 DataListTab 不再 emit generateDoc。保留该函数但不绑定（或移除 onGenerateDoc 引用）。

- [ ] **Step 7: Commit**

```bash
git add frontend/src/views/dashboard/DataWorkbench.vue
git commit -m "feat(frontend): dynamic custom tab rendering in DataWorkbench"
```

---

### Task 12: 编写后端单元测试

**Files:**
- Create: `src/test/java/com/superpower/modules/customtab/service/CustomTabServiceTest.java`

- [ ] **Step 1: 创建 CustomTabServiceTest**

Create new file:
```java
package com.superpower.modules.customtab.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.customtab.entity.CustomTab;
import com.superpower.modules.customtab.repository.CustomTabEntryRepository;
import com.superpower.modules.customtab.repository.CustomTabRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomTabServiceTest {

    @Mock
    private CustomTabRepository customTabRepository;

    @Mock
    private CustomTabEntryRepository customTabEntryRepository;

    @InjectMocks
    private CustomTabService customTabService;

    @Test
    void create_shouldThrowWhenNameExists() {
        when(customTabRepository.existsByVersionIdAndName(1L, "门诊清单")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> customTabService.create("门诊清单", 1L, 1L));

        assertEquals("清单名称已存在", ex.getMessage());
        verify(customTabRepository, never()).save(any());
    }

    @Test
    void create_shouldSaveWhenNameIsNew() {
        when(customTabRepository.existsByVersionIdAndName(1L, "新清单")).thenReturn(false);
        when(customTabRepository.save(any(CustomTab.class))).thenAnswer(inv -> {
            CustomTab t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        CustomTab result = customTabService.create("新清单", 1L, 1L);

        assertEquals("新清单", result.getName());
        assertEquals(1L, result.getVersionId());
        assertEquals(10L, result.getId());
    }

    @Test
    void addEntries_shouldSaveAllEntries() {
        CustomTab tab = new CustomTab();
        tab.setId(5L);
        tab.setName("测试清单");
        when(customTabRepository.findById(5L)).thenReturn(Optional.of(tab));

        customTabService.addEntries(5L, List.of(100L, 200L, 300L));

        verify(customTabEntryRepository, times(3)).save(any());
    }

    @Test
    void delete_shouldCleanUpEntriesAndTab() {
        customTabService.delete(5L);

        verify(customTabEntryRepository).deleteByCustomTabId(5L);
        verify(customTabRepository).deleteById(5L);
    }
}
```

- [ ] **Step 2: 运行测试确认通过**

Run: `mvn test -Dtest=CustomTabServiceTest`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/superpower/modules/customtab/service/CustomTabServiceTest.java
git commit -m "test(customtab): add unit tests for CustomTabService"
```

---

### Task 13: 运行完整验证

**Files:**
- Test: 全量验证

- [ ] **Step 1: 运行后端全部测试**

Run: `mvn test`
Expected: PASS (all tests green)

- [ ] **Step 2: 运行后端打包**

Run: `mvn package`
Expected: BUILD SUCCESS

- [ ] **Step 3: 运行前端构建**

Run: `npm run build`
Workdir: `frontend/`
Expected: vite build 成功，无错误

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "chore: full verification - backend tests and frontend build pass"
```

---

### Task 14: 人工验收脚本

**Files:**
- Test: 手动联调

- [ ] **Step 1: 基础验收**

```
1. 登录系统，选择 draft 版本进入工作台。
2. 在"数据清单"Tab 右侧点击"添加清单"按钮。
3. 输入名称（如"门诊功能清单"）创建的清单Tab。
4. 查看新Tab已添加到"数据清单"右侧，初始无数据。
5. 切换到"数据清单"Tab，在左侧树选择某个业务域，勾选几条条目。
6. 点击"插入待生成清单"，在弹出的清单选择对话框中点击刚才创建的清单。
7. 切换到自定义清单Tab，验证已插入的条目正常显示（树形结构）。
8. 在自定义清单中测试筛选、编辑、删除功能。
9. 删除自定义清单Tab，确认数据清单Tab不受影响。
10. 刷新页面，验证自定义清单数据持久化。
```

- [ ] **Step 2: 边界场景**

```
1. 未勾选条目点击"插入待生成清单" → toast"请先勾选条目"
2. 未创建任何自定义清单时点击"插入待生成清单" → toast"请先创建自定义清单"
3. 创建同名清单 → 后端返回错误，前端toast"清单名称已存在"
4. 切换到 released 版本 → 自定义清单Tab中的操作按钮应禁用
```

---

## 实施总结

**计划文件位置：** `docs/superpowers/plans/2026-05-26-custom-list-tab-plan.md`

**涉及文件：** 13个新建/修改文件，覆盖后端 entity/repository/service/controller 四层 + 前端 api/component/页面三层。
