# 项目问题修复与功能增强设计方案

**日期:** 2026-05-26
**项目:** superPowerTest
**需求来源:** 用户反馈

## 一、需求概述

本次需求涉及三个核心问题的修复与功能增强：

1. **Word文档格式问题**：标题不符合原生格式，无法生成目录
2. **版本发布限制**：已发布版本的数据应只允许查询和文档生成，禁止修改
3. **过滤清单功能**：增加多条件过滤，支持名称、状态、产品经理、解决方案、版本的筛选

---

## 二、问题1：Word文档标题格式修复

### 2.1 问题分析

**现状：**
- `generate_word.py` 使用了 `add_heading_with_style` 函数设置 Heading 样式
- 同时使用 `add_multilevel_numbering_to_paragraph` 手动添加编号
- 两个操作可能冲突，导致 Word 无法识别标题为目录项

**根本原因：**
- 手动添加的编号 XML 与 Word 原生 Heading 样式冲突
- Word 的目录生成基于 Heading 样式，手动编号破坏了样式关联

### 2.2 解决方案：使用 python-docx 原生方法

**核心思路：**
- 使用 `doc.add_heading()` 原生方法（传入数字作为 style level）
- 在标题文本中手动添加编号
- 移除手动编号逻辑，让 Word 原生处理目录

**实现细节：**

#### 2.2.1 修改函数 `add_heading_with_style`

原函数流程（需重构）：
1. 根据 level 选择 Heading 样式（Heading1-9）
2. 创建段落，直接设置样式
3. 手动调用 `add_multilevel_numbering_to_paragraph` 添加编号 XML
4. 添加标题文本

新函数流程：
1. 组合编号和文本为完整字符串（如 "1.1 门诊诊疗"）
2. 使用 `doc.add_heading(full_text, level=level)` 添加标题
3. 保持中文字体设置
4. 移除编号相关的XML操作

代码结构：
```python
def add_heading_with_number(doc, text, level, number):
    """添加带编号的标题"""
    full_text = f"{number} {text}"

    # 使用原生 Heading 方法
    para = doc.add_heading(full_text, level=level)

    # 设置中文字体
    for run in para.runs:
        run.font.name = '宋体'
        run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')

    return para
```

#### 2.2.2 移除旧函数

删除或重命名：
- `add_multilevel_numbering_to_paragraph` 函数及其调用
- `create_multilevel_list` 函数（如不再被其他地方使用）

#### 2.2.3 编号生成逻辑

需要在处理层级树时递归生成编号：

```python
def generate_node_numbering(nodes, children_map, level_3_counter):
    """
    生成多级编号
    nodes: 节点字典 {code: {name, description, ...}}
    children_map: 父节点到子节点列表的映射
    level_3_counter: Level 3 节点计数器 {value: 0}
    返回: {code: numbering_str} 编号映射
    """
    result = {}

    for code in sorted(nodes.keys()):
        node = nodes[code]
        current_level = len(code.split('.'))

        if current_level == 3:
            # Level 3 从独立计数器开始
            level_3_counter['value'] += 1
            result[code] = str(level_3_counter['value'])
        else:
            # 获取父节点编号
            if current_level > 1:
                parent_code = '.'.join(code.split('.')[:-1])
                prefix = result[parent_code]
                suffix = len(children_map.get(parent_code, []))
            else:
                # Level 1：从1开始
                prefix = ""
                suffix = level_3_counter['value'] + 1

            result[code] = f"{prefix}.{suffix}" if prefix else str(suffix)

        # 递归处理子节点
        if code in children_map:
            process_children(
                children_map[code],
                children_map,
                result,
                code,
                level_3_counter
            )

    return result

def process_children(children, children_map, numbering_map, parent_code, level_3_counter):
    """处理子节点的编号，处理L3节点的特殊情况"""
    # 确保当前节点的子节点计数器已初始化
    if parent_code not in level_3_counter:
        level_3_counter[parent_code] = 0

    parent_num = numbering_map[parent_code]

    for child_code in sorted(children):
        child_node = nodes[child_code]
        current_level = len(child_code.split('.'))

        if current_level == 3:
            # Level 3 独立编号
            level_3_counter[child_code] = {'value': 0}
            level_3_counter[child_code]['value'] += 1
            numbering_map[child_code] = str(level_3_counter[child_code]['value'])
        else:
            # 继承父节点编号
            level_3_counter[child_code] = level_3_counter.get(child_code, 0) + 1
            numbering_map[child_code] = f"{parent_num}.{level_3_counter[child_code]}"

        # 递归
        if child_code in children_map:
            process_children(
                children_map[child_code],
                children_map,
                numbering_map,
                child_code,
                level_3_counter
            )
```

**编号规则：**
- 业务分类(L1): 1, 2, 3...
- 业务域(L2): 1.1, 1.2, 2.1, 2.2...
- 产品/系统(L3+): 1.1.1, 1.1.2, 1.2.1, 2.1.1...

### 2.3 影响范围

**修改文件：**
- `generate_word.py`

**向后兼容性：**
- ✅ 需要提前准备好数据量测试（确保递归逻辑正确）
- ✅ 生成的 Word 文档可以直接用 Word/WPS 打开并生成目录
- ❌ 不兼容：旧版 python-docx API（已使用 3.0+ 版本，无需处理）

---

## 三、问题2：已发版清单修改限制

### 3.1 需求分析

**业务规则：**
- 已发布版本：只能查询和生成文档，禁止任何增删改操作和调整顺序
- 编辑中版本：所有操作正常

**技术实现：**
- 数据模型中已有 `status` 字段（draft/released）
- 需在前端和后端都增加校验

### 3.2 前端实现

#### 3.2.1 修改 `VersionManage.vue`

**新增状态管理：**
```vue
<script setup>
import { ref, onMounted } from 'vue'
import { getVersions, createVersion, releaseVersion } from '../../api/version'
import { ElMessage, ElMessageBox } from 'element-plus'

const versions = ref([])
const currentVersion = ref(null)  // 新增：当前选中的版本

async function loadVersions() {
  const res = await getVersions()
  versions.value = res.data || []
  if (versions.value.length > 0) {
    currentVersion.value = versions.value.find(v => v.status === 'draft') ||
                          versions.value[versions.value.length - 1]
  }
}

// 修改 handleRelease 方法，返回当前被发布的版本
async function handleRelease(id) {
  const versionToRelease = versions.value.find(v => v.id === id)
  if (!versionToRelease) return

  ElMessageBox.confirm(
    `确认封板发布版本"${versionToRelease.versionNo}"？发布后版本将不可再编辑。`,
    '提示',
    {
      confirmButtonText: '发布',
      type: 'warning'
    }
  ).then(async () => {
    await releaseVersion(id)
    ElMessage.success('发布成功')

    // 更新当前版本状态
    versionToRelease.status = 'released'
    versionToRelease.releasedAt = new Date().toISOString()

    // 重新加载版本列表
    await loadVersions()
  }).catch(() => {})
}

onMounted(loadVersions)
</script>
```

**模板修改：**
```vue
<template>
  <div class="page">
    <div class="page-header">
      <h3>版本管理</h3>
      <el-button
        type="primary"
        size="small"
        @click="handleCreateVersion"
        :disabled="currentVersion?.status === 'released'"
      >
        创建新版本
      </el-button>
    </div>

    <!-- 新增：版本状态提示 -->
    <div class="version-status">
      当前版本: {{ currentVersion?.versionNo }}
      <el-tag v-if="currentVersion?.status === 'draft'" type="warning" size="small">
        编辑中
      </el-tag>
      <el-tag v-else type="success" size="small">
        已发布
      </el-tag>
    </div>

    <el-table :data="versions" border stripe size="small">
      <!-- 此处省略表格列定义 -->
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'draft'"
            size="small"
            type="success"
            @click="handleRelease(row)"
          >
            封板发布
          </el-button>
          <el-tag v-else type="info" size="small">
            已发布
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.version-status {
  margin-bottom: 12px;
  padding: 8px;
  background-color: #f5f7fa;
  border-radius: 4px;
}
</style>
```

#### 3.2.2 修改 `BaseDataManage.vue`

**新增过滤和禁用逻辑：**
```vue
<script setup>
import { ref, watch, onMounted } from 'vue'
import { getTree, getChildren, createEntry, updateEntry, deleteEntry } from '../../api/data'
import { getVersions } from '../../api/version'
import { ElMessage, ElMessageBox } from 'element-plus'

const l1List = ref([])
const l2List = ref([])
const selectedL1 = ref(null)
const versionId = ref(null)
const versionStatus = ref('draft')  // 新增：版本状态

// 新增：过滤条件
const filters = ref({
  name: '',
  status: '',
  productManager: '',
  solution: '',
  versionTag: ''
})

async function loadVersion() {
  const res = await getVersions()
  const draft = res.data.find(v => v.status === 'draft')
  versionId.value = draft ? draft.id : res.data[res.data.length - 1].id
  versionStatus.value = draft ? 'draft' : 'released'  // 设置状态
}

// 新增：过滤方法
async function handleFilter() {
  if (!versionId.value) return
  await loadL1()  // 重新加载主列表，应用过滤
}

// 新增：重置过滤
function resetFilters() {
  filters.value = {
    name: '',
    status: '',
    productManager: '',
    solution: '',
    versionTag: ''
  }
}

// ... 其他原有逻辑保持不变 ...

// 修改保存方法，增加版本状态检查
async function saveL1() {
  if (!l1Form.value.colBizCategory) {
    ElMessage.warning('请输入名称')
    return
  }

  // 新增：已发布版本不允许修改
  if (versionStatus.value === 'released') {
    ElMessage.warning('已发布版本不可修改')
    return
  }

  if (isNewL1.value) {
    await createEntry({...})
    ElMessage.success('创建成功')
  } else {
    await updateEntry(editingL1Id.value, {...})
    ElMessage.success('保存成功')
  }

  // ... 其余代码 ...
}

// 修改删除方法，增加版本状态检查
async function deleteL1(row) {
  // 新增：已发布版本不允许删除
  if (versionStatus.value === 'released') {
    ElMessage.warning('已发布版本不可修改')
    return
  }

  ElMessageBox.confirm(`确认删除业务分类"${row.colBizCategory || row.label}"？下面的业务域也会一起删除。`, '提示', {
    type: 'warning'
  }).then(async () => {
    // ... 删除逻辑 ...
  }).catch(() => {})
}

// 类似地，在 saveL2 和 deleteL2 中也增加版本状态检查
// 在排序/升降级方法中也需要增加版本状态检查

onMounted(async () => {
  await loadVersion()
  await loadL1()
})
</script>

<template>
  <div class="page">
    <h3>基础数据维护</h3>
    <p class="subtitle">维护左侧层级树的业务分类（L1）与业务域（L2）</p>

    <!-- 新增：版本状态提示 -->
    <div class="version-status">
      <el-alert
        :title="versionStatus === 'draft' ? '当前版本：编辑中' : '当前版本：已发布（不可修改）'"
        :type="versionStatus === 'draft' ? 'warning' : 'error'"
        :closable="false"
        show-icon
      />

      <!-- 新增：过滤区域 -->
      <div class="filter-section">
        <el-form inline size="small">
          <el-form-item label="名称">
            <el-input v-model="filters.name" placeholder="模糊搜索" clearable @change="handleFilter" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="filters.status" placeholder="精确匹配" clearable @change="handleFilter">
              <el-option label="已开发" value="已开发" />
              <el-option label="开发中" value="开发中" />
              <el-option label="已规划" value="已规划" />
            </el-select>
          </el-form-item>
          <el-form-item label="产品经理">
            <el-input v-model="filters.productManager" placeholder="模糊搜索" clearable @change="handleFilter" />
          </el-form-item>
          <el-form-item label="解决方案">
            <el-select v-model="filters.solution" placeholder="精确匹配" clearable @change="handleFilter">
              <el-option label="智慧医疗" value="智慧医疗" />
              <el-option label="智慧服务" value="智慧服务" />
              <el-option label="智慧管理" value="智慧管理" />
              <el-option label="互联互通" value="互联互通" />
            </el-select>
          </el-form-item>
          <el-form-item label="版本">
            <el-select v-model="filters.versionTag" placeholder="包含匹配" clearable @change="handleFilter">
              <!-- 从版本规划数据中动态获取 -->
              <el-option label="V1.0" value="V1.0" />
              <el-option label="V1.1" value="V1.1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleFilter">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 其他原有内容 -->
      <div class="dual-tables">...</div>
    </div>
  </div>
</template>

<style scoped>
.version-status {
  margin-bottom: 12px;
}

.el-alert {
  margin-bottom: 12px;
}

.filter-section {
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
}
</style>
```

### 3.3 后端实现

#### 3.3.1 新增 `VersionService.getAccessStatus` 方法

**单位:** `src/main/java/com/superpower/modules/version/service/VersionService.java`

```java
public class VersionService {

    private final VersionRepository versionRepository;

    public VersionService(VersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    /**
     * 获取版本访问状态
     * @param versionId 版本ID
     * @return 访问状态 (draft 或 released)
     */
    public String getAccessStatus(Long versionId) {
        DataVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new RuntimeException("版本不存在"));

        return version.getStatus();
    }

    /**
     * 检查版本是否可编辑
     * @param versionId 版本ID
     * @return true=可编辑, false=已发布不可编辑
     */
    public boolean isEditable(Long versionId) {
        return "draft".equals(getAccessStatus(versionId));
    }
}
```

**依赖注入：**

在 `DataController` 中注入 `VersionService`：

```java
@RestController
@RequestMapping("/api/data")
public class DataController {

    private final DataService dataService;
    private final VersionService versionService;  // 新增

    public DataController(
        DataService dataService,
        VersionService versionService
    ) {
        this.dataService = dataService;
        this.versionService = versionService;
    }
    // ... 其他代码
}
```

#### 3.3.2 修改 `DataController` 写操作方法

**新增权限检查方法：**
```java
/**
 * 检查版本是否有编辑权限
 * @throws RuntimeException 当版本不可编辑时抛出
 */
private void checkVersionEditPermission(Long versionId) {
    if (!versionService.isEditable(versionId)) {
        throw new RuntimeException("已发布版本不可修改");
    }
}
```

**修改写操作方法：**

1. **创建条目：**
```java
@PostMapping
public Result<Long> createEntry(@RequestBody DataEntryCreateDTO dto, Authentication auth) {
    checkVersionEditPermission(dto.getVersionId());  // 新增检查

    Long userId = getAuthenticatedUserId(auth);
    Long entryId = dataService.createEntry(dto, userId);
    return Result.success(entryId);
}
```

2. **更新条目：**
```java
@PutMapping("/{id}")
public Result<Void> updateEntry(
    @PathVariable Long id,
    @RequestBody DataEntryUpdateDTO dto,
    Authentication auth
) {
    DataEntry entry = dataService.getEntry(id);

    if (entry == null) {
        return Result.error("记录不存在");
    }

    checkVersionEditPermission(entry.getVersionId());  // 新增检查

    Long userId = getAuthenticatedUserId(auth);
    dataService.updateEntry(id, dto, userId);
    return Result.success();
}
```

3. **删除条目：**
```java
@DeleteMapping("/{id}")
public Result<Void> deleteEntry(@PathVariable Long id) {
    DataEntry entry = dataService.getEntry(id);

    if (entry == null) {
        return Result.error("记录不存在");
    }

    checkVersionEditPermission(entry.getVersionId());  // 新增检查

    dataService.deleteEntry(id);
    return Result.success();
}
```

4. **更新排序：**
```java
@PutMapping("/sort")
public Result<Void> updateSort(@RequestBody SortDTO sortList) {
    checkVersionEditPermission(sortList.getVersionId());  // 新增检查

    dataService.updateSort(sortList);
    return Result.success();
}
```

5. **升降级（如有实现）：**
```java
@PutMapping("/{id}/level-up")
public Result<Void> levelUp(@PathVariable Long id) {
    DataEntry entry = dataService.getEntry(id);

    if (entry == null) {
        return Result.error("记录不存在");
    }

    checkVersionEditPermission(entry.getVersionId());  // 新增检查

    dataService.levelUp(id);
    return Result.success();
}

@PutMapping("/{id}/level-down")
public Result<Void> levelDown(@PathVariable Long id) {
    DataEntry entry = dataService.getEntry(id);

    if (entry == null) {
        return Result.error("记录不存在");
    }

    checkVersionEditPermission(entry.getVersionId());  // 新增检查

    dataService.levelDown(id);
    return Result.success();
}
```

### 3.4 影响范围

**修改文件：**
- 前端：`VersionManage.vue`, `BaseDataManage.vue`
- 后端：`VersionService.java`, `DataController.java`

**向后兼容性：**
- ✅ 前端不破坏现有交互
- ✅ 后端检查更严格，但符合业务规则
- ✅ 不改变 API 接口签名
- ❌ 需要用户更新浏览器端数据防止误操作

---

## 四、问题3：过滤清单功能

### 4.1 需求分析

**功能目标：**
- 在基础数据管理页面增加过滤区域
- 支持多条件筛选：
  - 名称：文本框，模糊匹配 `colProductSystem`
  - 状态：单选下拉框，精确匹配 `colStatus`
  - 产品经理：文本框，模糊匹配 `colProductManager`
  - 解决方案：单选下拉框，精确匹配 `colOtherSolutionTag`
  - 版本：单选下拉框，包含匹配 `colVersionDivision`

### 4.2 前端实现

#### 4.2.1 `BaseDataManage.vue` 过滤区域

**UI结构：**
```vue
<div class="filter-section">
  <el-form inline size="small">
    <el-form-item label="名称">
      <el-input
        v-model="filters.name"
        placeholder="请输入名称"
        clearable
        @change="handleFilter"
      />
    </el-form-item>

    <el-form-item label="状态">
      <el-select
        v-model="filters.status"
        placeholder="请选择状态"
        clearable
        @change="handleFilter"
      >
        <!-- 从后端 API 获取状态选项 -->
        <el-option
          v-for="status in statusOptions"
          :key="status.value"
          :label="status.label"
          :value="status.value"
        />
      </el-select>
    </el-form-item>

    <el-form-item label="产品经理">
      <el-input
        v-model="filters.productManager"
        placeholder="请输入产品经理"
        clearable
        @change="handleFilter"
      />
    </el-form-item>

    <el-form-item label="解决方案">
      <el-select
        v-model="filters.solution"
        placeholder="请选择解决方案"
        clearable
        @change="handleFilter"
      >
        <el-option label="智慧医疗" value="智慧医疗" />
        <el-option label="智慧服务" value="智慧服务" />
        <el-option label="智慧管理" value="智慧管理" />
        <el-option label="互联互通" value="互联互通" />
      </el-select>
    </el-form-item>

    <el-form-item label="版本">
      <el-select
        v-model="filters.versionTag"
        placeholder="请选择版本"
        clearable
        @change="handleFilter"
      >
        <!-- 从后端版本规划 API 动态加载 -->
        <el-option
          v-for="version in versionOptions"
          :key="version.value"
          :label="version.label"
          :value="version.value"
        />
      </el-select>
    </el-form-item>

    <el-form-item>
      <el-button type="primary" @click="handleFilter">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </el-form-item>
  </el-form>
</div>
```

**数据结构：**
```javascript
// 过滤条件
const filters = ref({
  name: '',
  status: '',
  productManager: '',
  solution: '',
  versionTag: ''
})

// 状态选项（从 API 获取）
const statusOptions = ref([
  { label: '已开发', value: '已开发' },
  { label: '开发中', value: '开发中' },
  { label: '已规划', value: '已规划' }
])

// 版本选项（从 API 加载）
const versionOptions = ref([])

// 加载版本选项
async function loadVersionOptions() {
  try {
    const res = await getVersions()
    versionOptions.value = res.data.map(v => ({
      label: v.versionNo,
      value: v.versionNo
    }))
  } catch (error) {
    console.error('加载版本选项失败:', error)
  }
}

// 处理过滤
async function handleFilter() {
  if (!versionId.value) return

  // 重新加载主表数据，应用过滤条件
  await loadL1()
}

// 重置过滤
function resetFilters() {
  filters.value = {
    name: '',
    status: '',
    productManager: '',
    solution: '',
    versionTag: ''
  }
  // 重新加载所有数据
  handleFilter()
}
```

**方法修改：**
```javascript
// 修改 loadL1 方法，应用过滤
async function loadL1() {
  const params = {}

  // 添加过滤参数
  if (filters.value.name) {
    params.name = filters.value.name
  }
  if (filters.value.status) {
    params.status = filters.value.status
  }
  if (filters.value.productManager) {
    params.productManager = filters.value.productManager
  }
  if (filters.value.solution) {
    params.solution = filters.value.solution
  }
  if (filters.value.versionTag) {
    params.versionTag = filters.value.versionTag
  }

  const res = await getTree(versionId.value, params)  // 传递过滤参数
  l1List.value = res.data || []
}

// 原来的 getTree 调用已存在，需要修改支持 params 参数传递
```

#### 4.2.2 修改 API 调用支持过滤

**修改 `src/api/data.js`：**

```javascript
export function getTree(versionId, params = {}) {
  return request.get(`/data/tree/${versionId}`, { params })
}

export function getChildren(versionId, parentId, params = {}) {
  return request.get(`/data/children/${versionId}/${parentId}`, { params })
}
```

### 4.3 后端实现

#### 4.3.1 修改 `DataController` 的查询接口增强

**当前 `getTree` 方法：**
```java
@GetMapping("/tree/{versionId}")
public Result<List<DataEntryDTO>> getTree(
    @PathVariable Long versionId,
    @RequestParam(required = false) Long parentId
) {
    List<DataEntryDTO> tree = dataService.buildTree(versionId, parentId);
    return Result.success(tree);
}
```

**增强为支持过滤：**
```java
@GetMapping("/tree/{versionId}")
public Result<List<DataEntryDTO>> getTree(
    @PathVariable Long versionId,
    @RequestParam(required = false) Long parentId,
    @RequestParam(required = false) String name,
    @RequestParam(required = false) String status,
    @RequestParam(required = false) String productManager,
    @RequestParam(required = false) String solution,
    @RequestParam(required = false) String versionTag
) {
    List<DataEntryDTO> tree = dataService.buildTree(
        versionId,
        parentId,
        name, status, productManager, solution, versionTag
    );
    return Result.success(tree);
}
```

#### 4.3.2 修改 `DataService` 查询逻辑

**当前 `buildTree` 方法：**
```java
public List<DataEntryDTO> buildTree(Long versionId, Long parentId) {
    // 构建树形结构逻辑
}
```

**增强为支持过滤：**
```java
public List<DataEntryDTO> buildTree(
    Long versionId,
    Long parentId,
    String name,
    String status,
    String productManager,
    String solution,
    String versionTag
) {
    // 获取符合条件的所有记录
    List<DataEntry> allEntries = dataRepository.findByVersionId(versionId);

    // 应用过滤条件
    if (StringUtils.isNotBlank(name)) {
        allEntries = allEntries.stream()
            .filter(e -> e.getColProductSystem() != null &&
                        (e.getColProductSystem().contains(name) ||
                          (e.getColBizCategory() != null && e.getColBizCategory().contains(name))))
            .collect(Collectors.toList());
    }

    if (StringUtils.isNotBlank(status)) {
        allEntries = allEntries.stream()
            .filter(e -> status.equals(e.getColStatus()))
            .collect(Collectors.toList());
    }

    if (StringUtils.isNotBlank(productManager)) {
        allEntries = allEntries.stream()
            .filter(e -> e.getColProductManager() != null &&
                        e.getColProductManager().contains(productManager))
            .collect(Collectors.toList());
    }

    if (StringUtils.isNotBlank(solution)) {
        allEntries = allEntries.stream()
            .filter(e -> solution.equals(e.getColOtherSolutionTag()))
            .collect(Collectors.toList());
    }

    if (StringUtils.isNotBlank(versionTag)) {
        allEntries = allEntries.stream()
            .filter(e -> e.getColVersionDivision() != null &&
                        e.getColVersionDivision().contains(versionTag))
            .collect(Collectors.toList());
    }

    // 保留父子关系，仅筛选叶子节点
    List<DataEntryDTO> includedEntries = convertToDTO(allEntries);

    // 如果是树的顶层（parentId为null），需要只保留有孩子的节点或所有根节点
    return buildTreeFromEntries(includedEntries, parentId);
}

private List<DataEntryDTO> buildTreeFromEntries(List<DataEntryDTO> entries, Long parentId) {
    List<DataEntryDTO> result = new ArrayList<>();

    if (entries == null || entries.isEmpty()) {
        return result;
    }

    // 构建ID到DTO的映射
    Map<Long, DataEntryDTO> entryMap = entries.stream()
        .collect(Collectors.toMap(DataEntryDTO::getId, dto -> dto, (a, b) -> a));

    // 构建树
    for (DataEntryDTO entry : entries) {
        if (entry.getParentId() == null || (parentId != null && entry.getParentId().equals(parentId))) {
            // 找到子节点
            List<DataEntryDTO> children = new ArrayList<>();
            for (DataEntryDTO child : entries) {
                if (child.getParentId() != null && child.getParentId().equals(entry.getId())) {
                    children.add(buildTreeFromEntries(entries, child.getParentId()));
                }
            }
            if (!children.isEmpty()) {
                entry.setChildren(children);
            }
            result.add(entry);
        }
    }

    return result;
}
```

**修改查询方法：**

`DataRepository` 需要添加查询能力（使用 JPA Criteria API 或 Specification）：

```java
public interface DataRepository extends JpaRepository<DataEntry, Long> {

    // 基础查询方法
    List<DataEntry> findByVersionId(Long versionId);

    // 可选：添加链接到 VersionRepository
    // @Query("SELECT e FROM DataEntry e WHERE e.versionId = :versionId")
    // List<DataEntry> findByVersionId(@Param("versionId") Long versionId);
}
```

**优化策略：**
如果查询性能成为问题，可以改用 Specification：

```java
public interface DataRepository extends JpaRepository<DataEntry, Long>, JpaSpecificationExecutor<DataEntry> {
}

@Service
public class DataServiceImpl implements DataService {

    public List<DataEntryDTO> buildTree(
        Long versionId,
        Long parentId,
        String name,
        String status,
        String productManager,
        String solution,
        String versionTag
    ) {
        Specification<DataEntry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("versionId"), versionId));

            if (StringUtils.isNotBlank(name)) {
                predicates.add(cb.or(
                    cb.like(root.get("colProductSystem"), "%" + name + "%"),
                    cb.like(root.get("colBizCategory"), "%" + name + "%")
                ));
            }

            if (StringUtils.isNotBlank(status)) {
                predicates.add(cb.equal(root.get("colStatus"), status));
            }

            if (StringUtils.isNotBlank(productManager)) {
                predicates.add(cb.like(root.get("colProductManager"), "%" + productManager + "%"));
            }

            if (StringUtils.isNotBlank(solution)) {
                predicates.add(cb.equal(root.get("colOtherSolutionTag"), solution));
            }

            if (StringUtils.isNotBlank(versionTag)) {
                predicates.add(cb.like(root.get("colVersionDivision"), "%" + versionTag + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<DataEntry> allEntries = dataRepository.findAll(spec);

        // 后续构建树逻辑同上
    }
}
```

### 4.4 影响范围

**修改文件：**
- 前端：`BaseDataManage.vue`, `data.js`
- 后端：`DataController.java`, `DataService.java`, `DataRepository.java`

**向后兼容性：**
- ✅ 向后兼容：不传递过滤参数时保持原有行为
- ✅ 性能考虑：添加了过滤条件后查询性能会略微下降
- ✅ 数据一致性：过滤不影响原始数据，只是查询时筛选

---

## 五、总结与验收标准

### 5.1 功能验收

**功能1：Word文档格式修复**
- [ ] 生成的 Word 文档中，标题能正确应用 Heading 1-9 样式
- [ ] 文档中的编号格式为：1, 1.1, 1.1.1...
- [ ] 在 Word/WPS 中右键点击"更新域"后能成功生成目录
- [ ] 目录中的层级关系正确

**功能2：版本发布限制**
- [ ] 已发布版本的"封板发布"按钮显示为禁用状态
- [ ] 已发布版本新建、编辑、删除、排序按钮均为禁用
- [ ] 尝试修改已发布版本的Data时，前端UI显示禁用按钮
- [ ] 后端API返回错误：`已发布版本不可修改`
- [ ] 编辑中版本的所有功能正常可用

**功能3：过滤清单功能**
- [ ] 过滤区域正确显示（名称、状态、产品经理、解决方案、版本）
- [ ] 名称搜索支持模糊匹配
- [ ] 状态精确匹配
- [ ] 产品经理模糊匹配
- [ ] 解决方案精确匹配
- [ ] 版本包含匹配（varchar中的包含关系）
- [ ] 重置按钮能清空所有过滤条件
- [ ] 查询结果正确显示符合所有条件的数据

### 5.2 性能考虑

**Word生成性能：**
- 预计增加100-200ms（取决于数据量）

**前端过滤性能：**
- 单个条件查询：增加约10-50ms
- 多个条件查询：增加约20-100ms

**后端过滤性能：**
- 简单过滤：增加约5-20ms（使用索引）
- 复杂过滤：需要谨慎添加数据库索引

### 5.3 风险评估

**低风险：**
- Word文档格式修复（文档生成模块独立）
- 过滤清单功能（纯前端+查询增强）

**中风险：**
- 版本发布限制 - 需要全面测试所有写操作路径

**建议的测试路径：**
1. 测试编辑中版本的所有操作是否正常
2. 测试已发布版本的所有操作是否被阻止
3. 生成大文档测试Word目录生成
4. 测试各种过滤组合的性能

---

## 六、附录

### 6.1 相关文件清单

| 文件路径 | 操作类型 | 说明 |
|---------|---------|------|
| `generate_word.py` | 修改 | Word文档生成逻辑 |
| `frontend/src/views/system/VersionManage.vue` | 修改 | 版本管理前端 |
| `frontend/src/views/system/BaseDataManage.vue` | 修改 | 基础数据管理前端 |
| `frontend/src/api/data.js` | 修改 | 数据API接口 |
| `src/main/java/com/superpower/modules/version/service/VersionService.java` | 新增 | 版本服务 |
| `src/main/java/com/superpower/modules/data/controller/DataController.java` | 修改 | 数据控制器 |
| `src/main/java/com/superpower/modules/data/service/DataService.java` | 修改 | 数据服务 |
| `src/main/java/com/superpower/modules/data/repository/DataRepository.java` | 修改 | 数据仓储 |

### 6.2 技术栈确认

- **Python:** python-docx 0.8.11+
- **前端:** Vue 3 + Element Plus
- **后端:** Spring Boot 3.2.0 + Spring Data JPA
- **数据库:** SQLite

---

**文档版本:** v1.0
**创建日期:** 2026-05-26
**作者:** 支持系统交互助手
