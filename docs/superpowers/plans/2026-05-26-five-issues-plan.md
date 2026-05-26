# 五个问题修复与功能增强实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复Word文档格式问题、添加版本发布限制、实现过滤清单功能三个核心需求

**Architecture:**
- 问题1：重构 generate_word.py，使用原生 Heading 样式替代手动编号
- 问题2：前端禁用按钮 + 后端权限检查实现版本发布限制
- 问题3：前端增加过滤UI + 后端扩展查询接口支持多条件过滤

**Tech Stack:**
- Python: python-docx, pandas
- Frontend: Vue 3, Element Plus
- Backend: Spring Boot 3.2.0, Spring Data JPA
- Database: SQLite

---

# 第一阶段：Word文档格式修复

## 模块：generate_word.py

### Task 1.1: 移除标题手动编号逻辑

**Files:**
- Modify: `generate_word.py:492-650`

- [ ] **Step 1: 移除旧的多级列表创建函数**

Delete or comment out:
```python
def create_multilevel_list(doc):
    """创建多级编号样式 - 此函数已被移除，使用原生Heading样式"""
    # 旧的实现代码（492-550行）
    pass

def add_multilevel_numbering_to_paragraph(paragraph, level):
    """为段落添加多级编号 - 此函数已被移除，使用原生Heading样式"""
    p = paragraph._element
    pPr = p.get_or_add_pPr()
    numPr = OxmlElement('w:numPr')
    ilvl = OxmlElement('w:ilvl')
    ilvl.set(qn('w:val'), str(level))
    numId = OxmlElement('w:numId')
    numId.set(qn('w:val'), str(num_id))
    numPr.append(ilvl)
    numPr.append(numId)
    pPr.append(numPr)
```

- [ ] **Step 2: 注释或移除原始调用**

Find and comment out line 552:
```python
# num_id = create_multilevel_list(doc)  # 已迁移到文本中，不再需要
```

- [ ] **Step 3: 验证旧函数已移除**

Run: `grep -n "add_multilevel_numbering_to_paragraph" generate_word.py`
Expected: No matches

- [ ] **Step 4: Commit**

```bash
git add generate_word.py
git commit -m "refactor: remove manual numbering from Word document generation"
```

### Task 1.2: 添加编号生成核心函数

**Files:**
- Modify: `generate_word.py:573-730`

- [ ] **Step 1: 添加编号生成工具函数**

Add after `extract_name` function:
```python
def extract_code(product):
    """提取产品编号（不含点）"""
    if isinstance(product, str):
        text = str(product).strip().replace('\n', '').replace('\r', '')
        match = re.match(r'^[\d\.]+', text)
        if match:
            return match.group(0)
    return None
```

- [ ] **Step 2: 添加节点编号映射生成函数**

Add new function:
```python
def build_numbering_map(nodes, children_map):
    """构建节点编号映射（1, 1.1, 1.1.1...）"""

    numbering_map = {}
    level_3_counters = {}  # {code: {'value': count}}

    # 初始化L3计数器
    for code in nodes:
        if len(code.split('.')) == 3:
            level_3_counters[code] = {'value': 0}

    # 处理所有符号连接的代码（L1-L3层级）
    for code in sorted(nodes.keys()):
        if code in level_3_counters:
            level_3_counters[code]['value'] += 1
            numbering_map[code] = str(level_3_counters[code]['value'])
        else:
            # 计算当前编号
            parts = code.split('.')
            if len(parts) == 1:
                # L1
                parent_num = ""
                suffix = len(nodes[code.split('.')[0]]) if code.split('.')[0] in nodes else 1
                numbering_map[code] = str(suffix)
            else:
                # L2或L3
                prefix_parts = parts[:-1]
                parent_code = '.'.join(prefix_parts)

                if parent_code in numbering_map:
                    parent_num = numbering_map[parent_code]
                else:
                    parent_num = "" if len(prefix_parts) == 1 else numbering_map.get(parent_code, "")

                # 获取子节点数量
                suffix = 0
                current_level_parent = '.'.join(prefix_parts)
                if len(prefix_parts) == 1:
                    # L1的直接子节点
                    suffix = [k for k in nodes if k.startswith(code + '.')].count(code + '.') + 1
                else:
                    # L2或L3的子节点
                    prefix = '.'.join(prefix_parts)
                    suffix = [k for k in nodes if k.startswith(prefix + '.')].count(prefix + '.') + len([c for c in children_map.get(parent_code, []) if c in numbering_map])

                numbering_map[code] = f"{parent_num}.{suffix}" if parent_num else str(suffix)

    # 递归处理连接的代码
    resolve_linked_nodes(nodes, children_map, numbering_map, level_3_counters)

    return numbering_map
```

- [ ] **Step 3: 添加递归解析函数**

Add new function:
```python
def resolve_linked_nodes(nodes, children_map, numbering_map, level_3_counters):
    """递归解析符号连接的代码，更新编号映射"""

    already_processed = set()

    for code in sorted(nodes.keys()):
        if code in already_processed:
            continue
        already_processed.add(code)

        if len(code.split('.')) < 3:
            # 处理子节点
            if code in children_map:
                for child_code in children_map[code]:
                    resolve_linked_nodes(nodes, children_map, numbering_map, level_3_counters)
                    already_processed.add(child_code)
```

- [ ] **Step 4: 验证函数正确性**

Run: `grep -n "build_numbering_map\|resolve_linked_nodes" generate_word.py`
Expected: Shows line numbers of new functions

- [ ] **Step 5: Commit**

```bash
git add generate_word.py
git commit -m "feat(word): add numbering generation function for Word headings"
```

### Task 1.3: 重构 add_heading_with_style 函数

**Files:**
- Modify: `generate_word.py:68-97`

- [ ] **Step 1: 重命名函数为 add_heading_with_number**

Replace function signature:
```python
def add_heading_with_number(doc, text, level, number):
```

- [ ] **Step 2: 实现新逻辑**

Replace function body:
```python
    """使用原生Heading样式添加带编号的标题"""
    full_text = f"{number} {text}"

    para = doc.add_heading(full_text, level=level)

    for run in para.runs:
        run.font.name = '宋体'
        run.font.color.rgb = RGBColor(0, 0, 0)
        run.font.italic = False
        para.alignment = WD_ALIGN_PARAGRAPH.LEFT
        run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')

    return para
```

- [ ] **Step 3: 验证修改**

Run: `grep -A 10 "def add_heading_with_number" generate_word.py`
Expected: Should show new function implementation

- [ ] **Step 4: Commit**

```bash
git add generate_word.py
git commit -m "refactor(word): replace manual numbering with native Heading styles"
```

### Task 1.4: 修改 process_data 调用编号函数

**Files:**
- Modify: `generate_word.py:590-691`

- [ ] **Step 1: 添加编号映射生成调用**

Add at the start of `process_data` function:
```python
def process_data():
    unique_categories = df['业务分类'].dropna().unique()
    category_counter = 0

    # 构建编号映射
    nodes = {}
    children = {}
    level_3_codes = []

    for category in unique_categories:
        if isinstance(category, str):
            category_text = extract_text(category)

            category_para = add_heading_with_style(doc, category_text, 1, num_id)

            category_df = df[df['业务分类'] == category].copy()

            unique_domains = []
            seen_domains = set()
            for domain in category_df['业务域'].dropna():
                if domain not in seen_domains:
                    unique_domains.append(domain)
                    seen_domains.add(domain)

            domain_counter = 0

            for domain in unique_domains:
                if isinstance(domain, str):
                    domain_text = extract_text(domain)

                    domain_para = add_heading_with_style(doc, domain_text, 2, num_id)

                    domain_df = category_df[category_df['业务域'] == domain].copy()

                    for _, row in domain_df.iterrows():
                        product = row['产品/系统']
                        parent_record = row['父记录']
                        code = extract_code(product)

                        if code:
                            parent_code = None
                            if isinstance(parent_record, str) and parent_record.strip():
                                parent_code = extract_code(parent_record)

                            nodes[code] = {
                                'name': extract_name(product),
                                'parent_code': parent_code,
                                'description': row['功能说明']
                            }

    # 构建映射
    for code in nodes:
        parent_code = nodes[code]['parent_code']
        if parent_code and parent_code in nodes:
            if parent_code not in children:
                children[parent_code] = []
            children[parent_code].append(code)

    level_3_codes = [code for code in nodes if len(code.split('.')) == 3]
    level_3_codes.sort(key=lambda x: tuple(int(p) for p in x.split('.')))

    # 生成编号映射
    numbering_map = build_numbering_map(nodes, children)

    category_counter = 0
```

- [ ] **Step 2: 修改所有 add_heading_with_style 调用**

Replace all `add_heading_with_style(doc, text, level, num_id)` calls with:
```python
# L1
category_text = extract_text(category)
numbering = numbering_map.get(code, str(category_counter + 1))
category_para = add_heading_with_number(doc, category_text, 1, numbering)

# L2
domain_text = extract_text(domain)
numbering = numbering_map.get(code, f"{numbering}.{len(level_3_codes) + 1}")
domain_para = add_heading_with_number(doc, domain_text, 2, numbering)

# L3+
name = nodes[code]['name']
numbering = numbering_map.get(code, str(level_3_counter['value']))
product_para = add_heading_with_number(doc, name, actual_level, numbering)
```

- [ ] **Step 3: 删除旧的 level_new_numbers 和处理逻辑**

Delete lines 646-689 (旧的 level_new_numbers, node_children_counters, process_node_and_children 等)

- [ ] **Step 4: 验证修改**

Run: `grep -n "add_heading_with_number" generate_word.py`
Expected: Multiple calls found

- [ ] **Step 5: Commit**

```bash
git add generate_word.py
git commit -m "feat(word): integrate numbering map into document generation logic"
```

### Task 1.5: 测试Word文档生成

**Files:**
- Test: `generate_word.py`

- [ ] **Step 1: 准备测试数据**

Create test Excel file at `test_data.xlsx` with required columns

- [ ] **Step 2: 运行文档生成**

```bash
python generate_word.py test_data.xlsx output_test.docx
```

- [ ] **Step 3: 验证Word文档**

Open `output_test.docx` in Word/WPS:
- [ ] 标题1: "1 XXXX" 正确显示
- [ ] 标题2: "1.1 XXXX" 正确显示
- [ ] 标题3: "1.1.1 XXXX" 正确显示
- [ ] 右键点击 → 更新域 → 成功生成目录
- [ ] 目录层级正确

- [ ] **Step 4: Commit**

```bash
git add generate_word.py
git commit -m "test(word): verify Word document headings and TOC generation"
```

---

# 第二阶段：版本发布限制

## 模块1：前端

### Task 2.1: 修改 VersionManage.vue 状态管理

**Files:**
- Modify: `frontend/src/views/system/VersionManage.vue`

- [ ] **Step 1: 添加 currentVersion 变量**

Add after line 42:
```javascript
const currentVersion = ref(null)
```

- [ ] **Step 2: 修改 loadVersions 函数**

Replace async function loadVersions():
```javascript
async function loadVersions() {
  const res = await getVersions()
  versions.value = res.data || []

  if (versions.value.length > 0) {
    currentVersion.value = versions.value.find(v => v.status === 'draft') ||
                          versions.value[versions.value.length - 1]
  }
}
```

- [ ] **Step 3: 修改 handleRelease 方法**（第55-64行）

Add line 62 (获取当前版本对象):
```javascript
const versionToRelease = versions.value.find(v => v.id === id)
if (!versionToRelease) return
```

Add line 70 (发布后更新状态):
```javascript
versionToRelease.status = 'released'
versionToRelease.releasedAt = new Date().toISOString()
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/system/VersionManage.vue
git commit -m "feat(frontend): add version status tracking for unified access control"
```

### Task 2.2: 修改 VersionManage.vue UI

**Files:**
- Modify: `frontend/src/views/system/VersionManage.vue`

- [ ] **Step 1: 添加版本状态提示**

Add after page-header (before table):
```vue
<div class="status-bar">
  <span>当前版本: {{ currentVersion?.versionNo }}</span>
  <el-tag v-if="currentVersion?.status === 'draft'" type="warning" size="small">
    编辑中
  </el-tag>
  <el-tag v-else type="success" size="small">
    已发布
  </el-tag>
</div>
```

- [ ] **Step 2: 禁用"创建版本"按钮**

Modify line 5:
```vue
<el-button type="primary" size="small" @click="handleCreateVersion" :disabled="currentVersion?.status === 'released'">
  创建新版本
</el-button>
```

- [ ] **Step 3: 修改操作列显示**

Replace template section (lines 25-33):
```vue
<el-table-column prop="status" label="状态" width="80">
  <template #default="{ row }">
    <el-tag v-if="row.status === 'draft'" type="warning">编辑中</el-tag>
    <el-tag v-else type="success">已发布</el-tag>
  </template>
</el-table-column>
```

Add operation column:
```vue
<el-table-column label="操作" width="120">
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
```

- [ ] **Step 4: 添加样式**

Add after existing styles (line 70):
```css
.status-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 8px 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  font-size: 14px;
}
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/system/VersionManage.vue
git commit -m "feat(frontend): disabled buttons for released versions"
```

### Task 2.3: 修改 BaseDataManage.vue 版本状态

**Files:**
- Modify: `frontend/src/views/system/BaseDataManage.vue`

- [ ] **Step 1: 添加 versionStatus 变量**

Add after line 114:
```javascript
const versionStatus = ref('draft')
```

- [ ] **Step 2: 修改 loadVersion 函数**

Replace lines 116-120:
```javascript
async function loadVersion() {
  const res = await getVersions()
  const draft = res.data.find(v => v.status === 'draft')
  versionId.value = draft ? draft.id : res.data[res.data.length - 1].id
  versionStatus.value = draft ? 'draft' : 'released'
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/system/BaseDataManage.vue
git commit -m "feat(frontend): track version status in BaseDataManage"
```

## 模块2：后端

### Task 2.4: 添加 VersionService 检查方法

**Files:**
- Create: `src/main/java/com/superpower/modules/version/service/VersionService.java`
- Modify: `src/main/java/com/superpower/modules/data/controller/DataController.java`

- [ ] **Step 1: 创建 VersionService**

Create new file:
```java
package com.superpower.modules.version.service;

import com.superpower.modules.version.entity.DataVersion;
import com.superpower.modules.version.repository.DataVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VersionService {

    private final DataVersionRepository versionRepository;

    public String getAccessStatus(Long versionId) {
        DataVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new RuntimeException("版本不存在"));
        return version.getStatus();
    }

    public boolean isEditable(Long versionId) {
        return "draft".equals(getAccessStatus(versionId));
    }
}
```

- [ ] **Step 2: 在 DataController 中注入 VersionService**

Modify constructor:
```java
private final DataService dataService;
private final VersionService versionService;

public DataController(DatabaseService databaseService, VersionService versionService) {
    this.dataService = databaseService;
    this.versionService = versionService;
}
```

- [ ] **Step 3: 添加权限检查方法**

Add private method before createEntry:
```java
private void checkVersionEditPermission(Long versionId) {
    if (!versionService.isEditable(versionId)) {
        throw new RuntimeException("已发布版本不可修改");
    }
}
```

- [ ] **Step 4: 添加缺失的 import**

Add to imports:
```java
import com.superpower.modules.version.service.VersionService;
import org.springframework.security.core.Authentication;
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/superpower/modules/version/service/VersionService.java
git add src/main/java/com/superpower/modules/data/controller/DataController.java
git commit -m "feat(backend): add version access control service"
```

### Task 2.5: 为所有写操作添加权限检查

**Files:**
- Modify: `src/main/java/com/superpower/modules/data/controller/DataController.java`

- [ ] **Step 1: 在 createEntry 添加检查**

Add at start of createEntry:
```java
checkVersionEditPermission(dto.getVersionId());
```

- [ ] **Step 2: 在 updateEntry 添加检查**

Add after getting entry:
```java
if (entry == null) {
    return Result.error("记录不存在");
}

checkVersionEditPermission(entry.getVersionId());
```

- [ ] **Step 3: 在 deleteEntry 添加检查**

Add after getting entry:
```java
if (entry == null) {
    return Result.error("记录不存在");
}

checkVersionEditPermission(entry.getVersionId());
```

- [ ] **Step 4: 在 updateSort 添加检查**

Add at start of updateSort:
```java
checkVersionEditPermission(sortList.getVersionId());
```

- [ ] **Step 5: 在 levelUp 添加检查**

Add after getting entry:
```java
if (entry == null) {
    return Result.error("记录不存在");
}

checkVersionEditPermission(entry.getVersionId());
```

- [ ] **Step 6: 在 levelDown 添加检查**

Add after getting entry:
```java
if (entry == null) {
    return Result.error("记录不存在");
}

checkVersionEditPermission(entry.getVersionId());
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/superpower/modules/data/controller/DataController.java
git commit -m "feat(backend): add version permission check to all write operations"
```

### Task 2.6: 测试版本发布限制

**Files:**
- Test: Frontend UI + Backend API

- [ ] **Step 1: 测试编辑中版本**

1. 运行后端: `mvn spring-boot:run`
2. 打开前端并登录
3. 选择编辑中的版本
4. 尝试新增/编辑/删除/排序
   Expected: All buttons enabled, operations successful

- [ ] **Step 2: 测试已发布版本**

1. 创建新版本
2. 发布版本
3. 版本状态变为"已发布"
4. 尝试新增/编辑/删除/排序
   Expected: All buttons disabled
5. 使用 Postman/curl 调用 API
   Expected: `{"code":500,"message":"已发布版本不可修改"}`

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "test(version): verify access control implementation"
```

---

# 第三阶段：过滤清单功能

## 模块1：前端

### Task 3.1: 添加过滤数据结构

**Files:**
- Modify: `frontend/src/views/system/BaseDataManage.vue`

- [ ] **Step 1: 添加过滤条件和选项**

Add after line 110:
```javascript
const filters = ref({
  name: '',
  status: '',
  productManager: '',
  solution: '',
  versionTag: ''
})

const statusOptions = ref([
  { label: '已开发', value: '已开发' },
  { label: '开发中', value: '开发中' },
  { label: '已规划', value: '已规划' }
])

const versionOptions = ref([])

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
```

- [ ] **Step 2: 添加过滤方法**

Add after line 236:
```javascript
async function handleFilter() {
  if (!versionId.value) return
  await loadL1()
}

function resetFilters() {
  filters.value = {
    name: '',
    status: '',
    productManager: '',
    solution: '',
    versionTag: ''
  }
  handleFilter()
}

// 在 onMounted 中加载版本选项
onMounted(async () => {
  await loadVersion()
  await loadVersionOptions()
  await loadL1()
})
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/system/BaseDataManage.vue
git commit -m "feat(frontend): add filter data structures and methods"
```

### Task 3.2: 添加过滤UI组件

**Files:**
- Modify: `frontend/src/views/system/BaseDataManage.vue`

- [ ] **Step 1: 添加版本状态提示**

Add after line 93:
```vue
<div class="version-status">
  <el-alert
    :title="versionStatus === 'draft' ? '当前版本：编辑中' : '当前版本：已发布（不可修改）'"
    :type="versionStatus === 'draft' ? 'warning' : 'error'"
    :closable="false"
    show-icon
  />
</div>
```

- [ ] **Step 2: 添加过滤区域**

Add after version status:
```vue
<div class="filter-section" v-if="versionStatus === 'draft'">
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

- [ ] **Step 3: 复制原有 dual-tables 结构并添加 enabedDisabled 属性**

Modify `<div class="dual-tables">`:
```vue
<div class="dual-tables">
  <div class="table-wrapper">
    <div class="table-header">
      <strong>业务分类 (L1)</strong>
      <el-button
        size="small"
        type="primary"
        @click="openL1Dialog()"
        :disabled="versionStatus === 'released'"
      >
        新增
      </el-button>
    </div>
    <el-table
      :data="l1List"
      border
      stripe
      size="small"
      highlight-current-row
      @current-change="onL1Select"
      style="cursor: pointer;"
    >
      <el-table-column label="名称" min-width="160">
        <template #default="{ row }">
          {{ row.colBizCategory || row.label }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openL1Dialog(row)" :disabled="versionStatus === 'released'">编辑</el-button>
          <el-button
            size="small"
            type="danger"
            link
            @click="deleteL1(row)"
            :disabled="versionStatus === 'released'"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <div class="table-wrapper">
    <div class="table-header">
      <strong>业务域 (L2)</strong>
      <div>
        <span v-if="selectedL1" style="font-size:12px;color:#999;margin-right:8px;">
          当前: {{ selectedL1.colBizCategory }}
        </span>
        <el-button
          size="small"
          type="primary"
          :disabled="!selectedL1 || versionStatus === 'released'"
          @click="openL2Dialog()"
        >
          新增
        </el-button>
      </div>
    </div>
    <el-table
      v-if="selectedL1"
      :data="l2List"
      border
      stripe
      size="small"
    >
      <el-table-column label="名称" min-width="160">
        <template #default="{ row }">
          {{ row.colBizDomain || row.label }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button
            size="small"
            type="primary"
            link
            @click="openL2Dialog(row)"
            :disabled="versionStatus === 'released'"
          >
            编辑
          </el-button>
          <el-button
            size="small"
            type="danger"
            link
            @click="deleteL2(row)"
            :disabled="versionStatus === 'released'"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div v-else class="placeholder">
      请先在左侧选择一个业务分类
    </div>
  </div>
</div>
```

- [ ] **Step 4: 添加样式**

Add after existing styles:
```css
.el-alert {
  margin-bottom: 12px;
}

.filter-section {
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 12px;
}
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/system/BaseDataManage.vue
git commit -m "feat(frontend): add filter section and UI components"
```

### Task 3.3: 修改 L1/L2 加载应用过滤

**Files:**
- Modify: `frontend/src/views/system/BaseDataManage.vue`

- [ ] **Step 1: 修改 loadL1 函数**

Replace lines 122-125:
```javascript
async function loadL1() {
  const params = {}

  if (filters.value.name) params.name = filters.value.name
  if (filters.value.status) params.status = filters.value.status
  if (filters.value.productManager) params.productManager = filters.value.productManager
  if (filters.value.solution) params.solution = filters.value.solution
  if (filters.value.versionTag) params.versionTag = filters.value.versionTag

  const res = await getTree(versionId.value, params)
  l1List.value = res.data || []
}
```

- [ ] **Step 2: 修改 loadL2 函数**

Replace lines 127-130:
```javascript
async function loadL2(l1Id) {
  const params = {}

  if (filters.value.name) params.name = filters.value.name
  if (filters.value.status) params.status = filters.value.status
  if (filters.value.productManager) params.productManager = filters.value.productManager
  if (filters.value.solution) params.solution = filters.value.solution
  if (filters.value.versionTag) params.versionTag = filters.value.versionTag

  const res = await getChildren(versionId.value, l1Id, params)
  l2List.value = res.data || []
}
```

- [ ] **Step 3: 修改 saveL1 添加版本状态检查**

Add after line 152:
```javascript
if (versionStatus.value === 'released') {
  ElMessage.warning('已发布版本不可修改')
  return
}
```

- [ ] **Step 4: 修改 deleteL1 添加版本状态检查**

Add after line 174:
```javascript
if (versionStatus.value === 'released') {
  ElMessage.warning('已发布版本不可修改')
  return
}
```

- [ ] **Step 5: 修改 saveL2 添加版本状态检查**

Add after line 210:
```javascript
if (versionStatus.value === 'released') {
  ElMessage.warning('已发布版本不可修改')
  return
}
```

- [ ] **Step 6: 修改 deleteL2 添加版本状态检查**

Add after line 231:
```javascript
if (versionStatus.value === 'released') {
  ElMessage.warning('已发布版本不可修改')
  return
}
```

- [ ] **Step 7: Commit**

```bash
git add frontend/src/views/system/BaseDataManage.vue
git commit -m "feat(frontend): apply filters and status checks to data operations"
```

## 模块2：后端

### Task 3.4: 扩展 DataController 查询接口

**Files:**
- Modify: `src/main/java/com/superpower/modules/data/controller/DataController.java`

- [ ] **Step 1: 扩展 getTree 方法签名**

Modify lines 29-34:
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
        versionId, parentId, name, status, productManager, solution, versionTag
    );
    return Result.success(tree);
}
```

- [ ] **Step 2: 扩展 getChildren 方法签名**

Modify lines 36-41:
```java
@GetMapping("/children/{versionId}/{parentId}")
public Result<List<DataEntryDTO>> getChildren(
    @PathVariable Long versionId,
    @PathVariable Long parentId,
    @RequestParam(required = false) String name,
    @RequestParam(required = false) String status,
    @RequestParam(required = false) String productManager,
    @RequestParam(required = false) String solution,
    @RequestParam(required = false) String versionTag
) {
    List<DataEntryDTO> children = dataService.buildChildren(
        versionId, parentId, name, status, productManager, solution, versionTag
    );
    return Result.success(children);
}
```

- [ ] **Step 3: 添加缺失的 import**

Add to imports:
```java
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import org.apache.commons.lang3.StringUtils;
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/superpower/modules/data/controller/DataController.java
git commit -m "feat(backend): extend query APIs to support filters"
```

### Task 3.5: 修改 DataService 实现过滤

**Files:**
- Modify: `src/main/java/com/superpower/modules/data/service/DataService.java`

- [ ] **Step 1: 更新 buildTree 方法**

Modify lines 40-50:
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
    List<DataEntry> allEntries = dataRepository.findByVersionId(versionId);

    // 应用过滤条件
    if (StringUtils.isNotBlank(name)) {
        allEntries = applyNameFilter(allEntries, name);
    }
    if (StringUtils.isNotBlank(status)) {
        allEntries = applyStatusFilter(allEntries, status);
    }
    if (StringUtils.isNotBlank(productManager)) {
        allEntries = applyProductManagerFilter(allEntries, productManager);
    }
    if (StringUtils.isNotBlank(solution)) {
        allEntries = applySolutionFilter(allEntries, solution);
    }
    if (StringUtils.isNotBlank(versionTag)) {
        allEntries = applyVersionTagFilter(allEntries, versionTag);
    }

    List<DataEntryDTO> includedEntries = convertToDTO(allEntries);
    return buildTreeFromEntries(includedEntries, parentId);
}
```

- [ ] **Step 2: 更新 buildChildren 方法**

Modify lines 51-61:
```java
public List<DataEntryDTO> buildChildren(
    Long versionId,
    Long parentId,
    String name,
    String status,
    String productManager,
    String solution,
    String versionTag
) {
    List<DataEntry> allEntries = dataRepository.findByVersionId(versionId);

    // 应用过滤条件
    if (StringUtils.isNotBlank(name)) {
        allEntries = applyNameFilter(allEntries, name);
    }
    if (StringUtils.isNotBlank(status)) {
        allEntries = applyStatusFilter(allEntries, status);
    }
    if (StringUtils.isNotBlank(productManager)) {
        allEntries = applyProductManagerFilter(allEntries, productManager);
    }
    if (StringUtils.isNotBlank(solution)) {
        allEntries = applySolutionFilter(allEntries, solution);
    }
    if (StringUtils.isNotBlank(versionTag)) {
        allEntries = applyVersionTagFilter(allEntries, versionTag);
    }

    List<DataEntryDTO> includedEntries = convertToDTO(allEntries);
    return buildChildrenFromEntries(includedEntries, parentId);
}
```

- [ ] **Step 3: 添加过滤方法实现**

Add four new private methods (after convertToDTO):
```java
private List<DataEntry> applyNameFilter(List<DataEntry> entries, String keyword) {
    if (StringUtils.isBlank(keyword)) return entries;

    return entries.stream()
        .filter(e -> isMatchName(e, keyword))
        .collect(Collectors.toList());
}

private boolean isMatchName(DataEntry entry, String keyword) {
    if (entry.getColProductSystem() != null && entry.getColProductSystem().contains(keyword)) {
        return true;
    }
    if (entry.getColBizCategory() != null && entry.getColBizCategory().contains(keyword)) {
        return true;
    }
    if (entry.getColBizDomain() != null && entry.getColBizDomain().contains(keyword)) {
        return true;
    }
    return false;
}

private List<DataEntry> applyStatusFilter(List<DataEntry> entries, String status) {
    if (StringUtils.isBlank(status)) return entries;
    return entries.stream()
        .filter(e -> status.equals(e.getColStatus()))
        .collect(Collectors.toList());
}

private List<DataEntry> applyProductManagerFilter(List<DataEntry> entries, String keyword) {
    if (StringUtils.isBlank(keyword)) return entries;
    return entries.stream()
        .filter(e -> isMatchProductManager(e, keyword))
        .collect(Collectors.toList());
}

private boolean isMatchProductManager(DataEntry entry, String keyword) {
    return entry.getColProductManager() != null &&
           entry.getColProductManager().contains(keyword);
}

private List<DataEntry> applySolutionFilter(List<DataEntry> entries, String solution) {
    if (StringUtils.isBlank(solution)) return entries;
    return entries.stream()
        .filter(e -> solution.equals(e.getColOtherSolutionTag()))
        .collect(Collectors.toList());
}

private List<DataEntry> applyVersionTagFilter(List<DataEntry> entries, String tag) {
    if (StringUtils.isBlank(tag)) return entries;
    return entries.stream()
        .filter(e -> isMatchVersionTag(e, tag))
        .collect(Collectors.toList());
}

private boolean isMatchVersionTag(DataEntry entry, String tag) {
    return entry.getColVersionDivision() != null &&
           entry.getColVersionDivision().contains(tag);
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/superpower/modules/data/service/DataService.java
git commit -m "feat(backend): implement multiple field filtering capabilities"
```

### Task 3.6: 测试过滤清单功能

**Files:**
- Test: Frontend UI + Backend API

- [ ] **Step 1: 测试前端过滤UI**

1. 运行后端: `mvn spring-boot:run`
2. 构建前端: `cd frontend && npm run build`，或使用开发服务器
3. 打开页面，确保版本状态为"编辑中"
4. 测试各种过滤条件：
   - 为空时：显示所有数据
   - 输入名称：模糊匹配
   - 选择状态：精确匹配
   - 输入产品经理：模糊匹配
   - 选择解决方案：精确匹配
   - 选择版本：包含匹配

- [ ] **Step 2: 测试重置按钮**

点击"重置"按钮，所有过滤条件清除，数据恢复完整

- [ ] **Step 3: 测试后端API**

使用 Postman/curl 调用：
```bash
curl -X GET "http://localhost:8080/api/data/tree/1?name=智慧医疗&status=已开发"
curl -X GET "http://localhost:8080/api/data/children/1/2?versionTag=V1.0"
```

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "test(filter): verify filtering functionality in UI and backend"
```

---

# 综合测试与验收

### Task 4.1: 端到端测试

**Files:**
- Test: 全系统

- [ ] **Step 1: Word文档生成测试**

1. 准备完整数据
2. 运行: `python generate_word.py input.xlsx output.docx`
3. 打开 output.docx
   - [ ] 标题格式正确
   - [ ] 编号正确
   - [ ] 目录生成成功
   - [ ] 图片插入正确

- [ ] **Step 2: 版本管理测试**

1. 创建版本A
2. 创建版本B
3. 发布版本A
4. 操作检查:
   - [ ] 版本A状态显示"已发布"
   - [ ] 版本A的"封板发布"按钮禁用
   - [ ] 版本B状态显示"编辑中"
   - [ ] 版本B所有按钮可用

- [ ] **Step 3: 数据修改测试**

1. 以编辑中版本运行
   - [ ] 所有数据维护功能正常
   - [ ] 过滤功能正常
   - [ ] 新增/编辑/删除成功
2. 发布版本
3. 以已发布版本运行
   - [ ] 所有按钮禁用
   - [ ] 数据查询正常
   - [ ] 文档生成正常
   - [ ] API拒绝写操作

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "test(e2e): comprehensive end-to-end verification"
```

### Task 4.2: 性能检查

**Files:**
- Test: 性能监控

- [ ] **Step 1: Word生成性能**

生成1000条数据的文档，记录时间

- [ ] **Step 2: 过滤查询性能**

分别测试：
  - 单条件过滤: 100ms以下
  - 多条件过滤: 200ms以下
  - 无过滤: 50ms以下

- [ ] **Step 3: 文档总结**

记录性能数据和任何发现的问题

---

# 实施总结

**计划文件位置：** `docs/superpowers/plans/2026-05-26-five-issues-plan.md`

**执行选项：**

1. **Subagent-Driven（推荐）** - 我将使用 subagent-driven-development 技能，为每个任务派遣独立的子代理批量执行，任务之间进行两轮审查，实现快速迭代

2. **Inline Execution** - 使用 executing-plans 技能在当前会话中按顺序执行任务，批量执行并设置审查检查点

**选择哪种执行方式？**
