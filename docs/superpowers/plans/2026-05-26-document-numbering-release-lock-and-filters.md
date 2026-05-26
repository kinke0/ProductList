# Document Numbering, Release Lock, and Filters Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现功能说明文档按最终导出目录树统一编号、已发版版本前后端锁定、以及清单过滤条件与左树过滤统一到后端查询。

**Architecture:** 后端以 `DataEntryService` 和 `DataEntryRepository` 承担版本锁定与统一查询职责，以 `DocumentService` 重构功能说明文档的标题树与编号生成；前端集中改造 `DataListTab.vue` 的筛选表单、只读态和拖拽/勾选保护。后端测试使用 JUnit 5 + Mockito 单元测试覆盖文档编号与发版锁定，前端以构建和手工联调作为主要验证方式。

**Tech Stack:** Spring Boot 3, Spring Data JPA, Apache POI, Vue 3, Element Plus, SortableJS, JUnit 5, Mockito

---

## File Structure Map

### Backend
- Modify: `src/main/java/com/superpower/modules/data/controller/DataEntryController.java`
  - 扩展查询接口参数：`solution`、`versionDivision`、`bizCategory`、`bizDomain`
- Modify: `src/main/java/com/superpower/modules/data/service/DataEntryService.java`
  - 增加 released 版本写保护、扩展统一查询、封装版本状态校验
- Modify: `src/main/java/com/superpower/modules/data/repository/DataEntryRepository.java`
  - 扩展 JPQL 查询条件，替换旧 `tag` 语义
- Modify: `src/main/java/com/superpower/modules/document/service/DocumentService.java`
  - 重构功能说明 Word 导出树和标题编号逻辑，补根节点排序
- Create: `src/test/java/com/superpower/modules/data/service/DataEntryServiceTest.java`
  - 单元测试：released 锁定、查询参数透传、排序保护
- Create: `src/test/java/com/superpower/modules/document/service/DocumentServiceTest.java`
  - 单元测试：部分导出重新编号、同级递增、根节点排序

### Frontend
- Modify: `frontend/src/components/DataListTab.vue`
  - 筛选表单改造、只读态 UI 与方法保护、移除前端二次过滤
- Modify: `frontend/src/api/data.js`
  - 复用现有 query API，只扩展参数对象语义，无路径改动
- Modify: `frontend/src/views/dashboard/DataWorkbench.vue`
  - 保持 `is-editing` 传参，必要时补只读提示文案

### Verification
- Run: `mvn test`
- Run: `mvn package`
- Run: `npm run build`

---

### Task 1: 用测试锁定后端发版保护与查询契约

**Files:**
- Create: `src/test/java/com/superpower/modules/data/service/DataEntryServiceTest.java`
- Modify: `src/main/java/com/superpower/modules/data/service/DataEntryService.java`
- Modify: `src/main/java/com/superpower/modules/data/repository/DataEntryRepository.java`
- Modify: `src/main/java/com/superpower/modules/data/controller/DataEntryController.java`

- [ ] **Step 1: 写 released 版本禁止更新/删除/排序的失败测试**

```java
package com.superpower.modules.data.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.data.dto.DataEntryDTO;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import com.superpower.modules.version.entity.DataVersion;
import com.superpower.modules.version.repository.DataVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataEntryServiceTest {

    @Mock
    private DataEntryRepository entryRepository;

    @Mock
    private DataVersionRepository versionRepository;

    @InjectMocks
    private DataEntryService dataEntryService;

    private DataVersion releasedVersion;
    private DataEntry releasedEntry;

    @BeforeEach
    void setUp() {
        releasedVersion = new DataVersion();
        releasedVersion.setId(9L);
        releasedVersion.setVersionNo("1.0");
        releasedVersion.setStatus("released");

        releasedEntry = new DataEntry();
        releasedEntry.setId(100L);
        releasedEntry.setVersionId(9L);
        releasedEntry.setParentId(10L);
        releasedEntry.setLevel(3);
        releasedEntry.setSortOrder(0);
        releasedEntry.setColProductSystem("收费系统");
    }

    @Test
    void update_shouldRejectReleasedVersion() {
        DataEntryDTO dto = new DataEntryDTO();
        dto.setColProductSystem("修改后名称");

        when(entryRepository.findById(100L)).thenReturn(Optional.of(releasedEntry));
        when(versionRepository.findById(9L)).thenReturn(Optional.of(releasedVersion));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dataEntryService.update(100L, dto));

        assertEquals("已发版版本不允许修改清单", ex.getMessage());
        verify(entryRepository, never()).save(any());
    }

    @Test
    void delete_shouldRejectReleasedVersion() {
        when(entryRepository.findById(100L)).thenReturn(Optional.of(releasedEntry));
        when(versionRepository.findById(9L)).thenReturn(Optional.of(releasedVersion));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dataEntryService.delete(100L));

        assertEquals("已发版版本不允许修改清单", ex.getMessage());
        verify(entryRepository, never()).deleteById(any());
    }

    @Test
    void updateSort_shouldRejectReleasedVersion() {
        when(entryRepository.findById(100L)).thenReturn(Optional.of(releasedEntry));
        when(versionRepository.findById(9L)).thenReturn(Optional.of(releasedVersion));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dataEntryService.updateSort(List.of(Map.of("id", 100L, "sortOrder", 1))));

        assertEquals("已发版版本不允许修改清单", ex.getMessage());
        verify(entryRepository, never()).save(any());
    }
}
```

- [ ] **Step 2: 写 draft 版本允许创建与扩展查询参数透传的失败测试**

```java
@Test
void create_shouldAllowDraftVersion() {
    DataVersion draftVersion = new DataVersion();
    draftVersion.setId(3L);
    draftVersion.setStatus("draft");

    DataEntryDTO dto = new DataEntryDTO();
    dto.setVersionId(3L);
    dto.setLevel(3);
    dto.setColProductSystem("影像平台");

    when(versionRepository.findById(3L)).thenReturn(Optional.of(draftVersion));
    when(entryRepository.save(any(DataEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

    DataEntry created = dataEntryService.create(dto);

    assertEquals("影像平台", created.getColProductSystem());
    assertEquals(3L, created.getVersionId());
}

@Test
void query_shouldForwardExpandedFilters() {
    when(entryRepository.queryEntries(1L, "收费", "可交付", "张三", "HIS", "A-曜系列", "门诊", "收费域"))
            .thenReturn(List.of());

    dataEntryService.query(1L, "收费", "可交付", "张三", "HIS", "A-曜系列", "门诊", "收费域");

    verify(entryRepository).queryEntries(1L, "收费", "可交付", "张三", "HIS", "A-曜系列", "门诊", "收费域");
}
```

- [ ] **Step 3: 运行测试，确认当前实现失败**

Run: `mvn test -Dtest=DataEntryServiceTest`
Expected: FAIL，原因应包括：
- `DataEntryService` 构造器缺少 `DataVersionRepository`
- `query(...)` / `queryEntries(...)` 方法签名不匹配
- 尚未抛出 `已发版版本不允许修改清单`

- [ ] **Step 4: 扩展 repository 查询签名与 JPQL**

```java
@Query("SELECT e FROM DataEntry e WHERE e.versionId = :versionId " +
       "AND e.level >= 3 " +
       "AND (:name IS NULL OR e.colProductSystem LIKE %:name%) " +
       "AND (:status IS NULL OR e.colStatus = :status) " +
       "AND (:pm IS NULL OR e.colProductManager LIKE %:pm%) " +
       "AND (:solution IS NULL OR e.colOtherSolutionTag = :solution) " +
       "AND (:versionDivision IS NULL OR e.colVersionDivision LIKE %:versionDivision%) " +
       "AND (:bizCategory IS NULL OR e.colBizCategory = :bizCategory) " +
       "AND (:bizDomain IS NULL OR e.colBizDomain = :bizDomain) " +
       "ORDER BY e.level, e.sortOrder")
List<DataEntry> queryEntries(@Param("versionId") Long versionId,
                             @Param("name") String name,
                             @Param("status") String status,
                             @Param("pm") String productManager,
                             @Param("solution") String solution,
                             @Param("versionDivision") String versionDivision,
                             @Param("bizCategory") String bizCategory,
                             @Param("bizDomain") String bizDomain);
```

- [ ] **Step 5: 在 service 中引入版本仓库与统一锁定校验**

```java
private final DataEntryRepository entryRepository;
private final DataVersionRepository versionRepository;

public DataEntryService(DataEntryRepository entryRepository,
                        DataVersionRepository versionRepository) {
    this.entryRepository = entryRepository;
    this.versionRepository = versionRepository;
}

public List<DataEntry> query(Long versionId,
                             String name,
                             String status,
                             String productManager,
                             String solution,
                             String versionDivision,
                             String bizCategory,
                             String bizDomain) {
    return entryRepository.queryEntries(versionId, name, status, productManager,
            solution, versionDivision, bizCategory, bizDomain);
}

private void ensureVersionEditable(Long versionId) {
    DataVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new BusinessException("版本不存在"));
    if (!"draft".equals(version.getStatus())) {
        throw new BusinessException("已发版版本不允许修改清单");
    }
}
```

- [ ] **Step 6: 在 create / update / delete / sort / level-up / level-down 中接入校验**

```java
@Transactional
public DataEntry create(DataEntryDTO dto) {
    ensureVersionEditable(dto.getVersionId());
    DataEntry entry = new DataEntry();
    copyFields(entry, dto);
    entry.setVersionId(dto.getVersionId());
    entry.setParentId(dto.getParentId());
    entry.setLevel(dto.getLevel());
    entry.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
    entry.setIsLeaf(true);
    // 原有父节点 isLeaf 修正逻辑保留
    return entryRepository.save(entry);
}

@Transactional
public DataEntry update(Long id, DataEntryDTO dto) {
    DataEntry entry = getById(id);
    ensureVersionEditable(entry.getVersionId());
    copyFields(entry, dto);
    return entryRepository.save(entry);
}

@Transactional
public void delete(Long id) {
    DataEntry entry = getById(id);
    ensureVersionEditable(entry.getVersionId());
    List<DataEntry> children = entryRepository.findByVersionIdAndParentId(entry.getVersionId(), id);
    if (!children.isEmpty()) {
        throw new BusinessException("该节点下有子节点，无法删除");
    }
    entryRepository.deleteById(id);
}
```

```java
@Transactional
public void updateSort(List<Map<String, Object>> sortList) {
    for (Map<String, Object> item : sortList) {
        Long id = Long.valueOf(item.get("id").toString());
        DataEntry entry = getById(id);
        ensureVersionEditable(entry.getVersionId());
        Integer sortOrder = Integer.valueOf(item.get("sortOrder").toString());
        entry.setSortOrder(sortOrder);
        entryRepository.save(entry);
    }
}
```

- [ ] **Step 7: 扩展 controller 查询参数并更新 service 调用**

```java
@GetMapping("/query/{versionId}")
public Result<List<DataEntry>> query(
        @PathVariable Long versionId,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String productManager,
        @RequestParam(required = false) String solution,
        @RequestParam(required = false) String versionDivision,
        @RequestParam(required = false) String bizCategory,
        @RequestParam(required = false) String bizDomain) {
    return Result.success(dataEntryService.query(versionId, name, status, productManager,
            solution, versionDivision, bizCategory, bizDomain));
}
```

- [ ] **Step 8: 重新运行测试，确认通过**

Run: `mvn test -Dtest=DataEntryServiceTest`
Expected: PASS

---

### Task 2: 用测试驱动文档编号改造

**Files:**
- Create: `src/test/java/com/superpower/modules/document/service/DocumentServiceTest.java`
- Modify: `src/main/java/com/superpower/modules/document/service/DocumentService.java`

- [ ] **Step 1: 写功能说明文档编号与排序的失败测试**

```java
package com.superpower.modules.document.service;

import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import com.superpower.modules.document.repository.DocGenRecordRepository;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class DocumentServiceTest {

    @Test
    void generateFeatureWord_shouldRestartNumberingForPartialExportAndSortRoots() throws Exception {
        DataEntryRepository entryRepository = Mockito.mock(DataEntryRepository.class);
        DocGenRecordRepository recordRepository = Mockito.mock(DocGenRecordRepository.class);
        DocumentService service = new DocumentService(entryRepository, recordRepository);

        DataEntry category = entry(1L, null, 1, 0, null, null, "临床", null);
        DataEntry domain = entry(2L, 1L, 2, 0, null, null, "临床", "门诊");
        DataEntry rootB = entry(20L, 2L, 3, 1, "B产品", "根节点B说明", "临床", "门诊");
        DataEntry rootA = entry(10L, 2L, 3, 0, "A产品", "根节点A说明", "临床", "门诊");
        DataEntry childA = entry(11L, 10L, 4, 0, "A模块", "A模块说明", "临床", "门诊");

        when(entryRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(rootB, rootA));

        byte[] bytes = service.generateDocument("feature", "word", List.of(10L, 20L));

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            String text = doc.getParagraphs().stream()
                    .map(p -> p.getText() == null ? "" : p.getText().trim())
                    .filter(s -> !s.isBlank())
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");

            assertTrue(text.contains("1 临床"));
            assertTrue(text.contains("1.1 门诊"));
            assertTrue(text.contains("1.1.1 A产品"));
            assertTrue(text.contains("1.1.2 B产品"));
        }
    }

    private static DataEntry entry(Long id, Long parentId, int level, int sortOrder,
                                   String name, String featureDesc, String category, String domain) {
        DataEntry entry = new DataEntry();
        entry.setId(id);
        entry.setParentId(parentId);
        entry.setVersionId(1L);
        entry.setLevel(level);
        entry.setSortOrder(sortOrder);
        entry.setColProductSystem(name);
        entry.setColFeatureDesc(featureDesc);
        entry.setColBizCategory(category);
        entry.setColBizDomain(domain);
        return entry;
    }
}
```

- [ ] **Step 2: 运行测试，确认当前实现失败**

Run: `mvn test -Dtest=DocumentServiceTest`
Expected: FAIL，原因应包括：
- 当前 `generateDocument(...)` 对部分导出仅取 `findAllById(...)`，不会自行构树补子节点到测试预期
- 当前输出标题文本没有稳定的“编号 + 空格 + 标题”格式
- 根节点排序可能不稳定

- [ ] **Step 3: 在 DocumentService 中新增导出树节点模型与构树方法**

```java
private static class ExportNode {
    private final String title;
    private final DataEntry entry;
    private final List<ExportNode> children = new ArrayList<>();

    private ExportNode(String title, DataEntry entry) {
        this.title = title;
        this.entry = entry;
    }
}

private List<ExportNode> buildFeatureExportTree(List<DataEntry> entries) {
    Map<String, List<DataEntry>> byCategory = entries.stream()
            .collect(Collectors.groupingBy(e -> defaultString(e.getColBizCategory())));
    List<ExportNode> roots = new ArrayList<>();

    byCategory.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(categoryEntry -> {
                ExportNode categoryNode = new ExportNode(categoryEntry.getKey(), null);
                roots.add(categoryNode);

                Map<String, List<DataEntry>> byDomain = categoryEntry.getValue().stream()
                        .collect(Collectors.groupingBy(e -> defaultString(e.getColBizDomain())));

                byDomain.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(domainEntry -> {
                            ExportNode domainNode = new ExportNode(domainEntry.getKey(), null);
                            categoryNode.children.add(domainNode);
                            domainNode.children.addAll(buildEntryTree(domainEntry.getValue()));
                        });
            });
    return roots;
}
```

- [ ] **Step 4: 实现条目树排序与递归编号写入**

```java
private List<ExportNode> buildEntryTree(List<DataEntry> entries) {
    Map<Long, ExportNode> nodeMap = new LinkedHashMap<>();
    for (DataEntry entry : entries) {
        nodeMap.put(entry.getId(), new ExportNode(defaultString(entry.getColProductSystem()), entry));
    }

    List<ExportNode> roots = new ArrayList<>();
    entries.stream()
            .sorted(Comparator.comparing(DataEntry::getSortOrder, Comparator.nullsFirst(Integer::compareTo)))
            .forEach(entry -> {
                ExportNode node = nodeMap.get(entry.getId());
                if (entry.getParentId() != null && nodeMap.containsKey(entry.getParentId())) {
                    nodeMap.get(entry.getParentId()).children.add(node);
                } else {
                    roots.add(node);
                }
            });
    return roots;
}

private void writeExportNodes(XWPFDocument doc, List<ExportNode> nodes, List<Integer> path) {
    for (int i = 0; i < nodes.size(); i++) {
        ExportNode node = nodes.get(i);
        List<Integer> currentPath = new ArrayList<>(path);
        currentPath.add(i + 1);
        int level = currentPath.size();
        String prefix = currentPath.stream().map(String::valueOf).collect(Collectors.joining("."));
        addNumberedHeading(doc, prefix + " " + node.title, Math.min(level, 9));
        if (node.entry != null && node.entry.getColFeatureDesc() != null) {
            processDescriptionWithImages(doc, node.entry.getColFeatureDesc());
        }
        writeExportNodes(doc, node.children, currentPath);
    }
}
```

- [ ] **Step 5: 将功能说明导出主流程切换到“先构树，再写节点”**

```java
private byte[] generateFeatureWord(XWPFDocument doc, List<DataEntry> entries) throws Exception {
    List<DataEntry> filtered = entries.stream()
            .filter(e -> e.getLevel() != null && e.getLevel() >= 3)
            .toList();

    List<ExportNode> exportTree = buildFeatureExportTree(filtered);
    writeExportNodes(doc, exportTree, List.of());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    doc.write(out);
    return out.toByteArray();
}
```

- [ ] **Step 6: 简化标题写入函数，改为显式文本编号而不是依赖双轨编号**

```java
private void addNumberedHeading(XWPFDocument doc, String text, int level) {
    XWPFParagraph paragraph = doc.createParagraph();
    paragraph.setStyle("Heading" + Math.min(level, 9));

    XWPFRun run = paragraph.createRun();
    run.setText(text);
    run.setBold(true);
    run.setFontFamily("宋体");
    run.setFontSize(Math.max(12, 18 - level));
}
```

- [ ] **Step 7: 重新运行文档测试，确认通过**

Run: `mvn test -Dtest=DocumentServiceTest`
Expected: PASS

---

### Task 3: 改造前端筛选表单并统一查询参数

**Files:**
- Modify: `frontend/src/components/DataListTab.vue`
- Modify: `frontend/src/api/data.js`

- [ ] **Step 1: 先写出新的查询表单数据结构与重置逻辑**

```js
const queryForm = reactive({
  name: '',
  status: '',
  productManager: '',
  solution: '',
  versionDivision: ''
})

function resetQuery() {
  queryForm.name = ''
  queryForm.status = ''
  queryForm.productManager = ''
  queryForm.solution = ''
  queryForm.versionDivision = ''
  handleQuery()
}
```

- [ ] **Step 2: 把模板中的“方案标记输入框”替换为“解决方案下拉框”，并新增版本下拉框**

```vue
<el-form-item label="解决方案">
  <el-select v-model="queryForm.solution" placeholder="全部" clearable style="width: 140px">
    <el-option v-for="s in solutions" :key="s" :label="s" :value="s" />
  </el-select>
</el-form-item>
<el-form-item label="版本">
  <el-select v-model="queryForm.versionDivision" placeholder="全部" clearable style="width: 140px">
    <el-option label="A-曜系列" value="A-曜系列" />
    <el-option label="B-远系列" value="B-远系列" />
    <el-option label="C-驰系列" value="C-驰系列" />
  </el-select>
</el-form-item>
```

- [ ] **Step 3: 把 handleQuery 改成把筛选与左树条件一起发给后端**

```js
async function handleQuery() {
  const res = await queryEntries(props.versionId, {
    name: queryForm.name || undefined,
    status: queryForm.status || undefined,
    productManager: queryForm.productManager || undefined,
    solution: queryForm.solution || undefined,
    versionDivision: queryForm.versionDivision || undefined,
    bizCategory: props.selectedNode?.id !== 'all' ? (props.selectedNode?.categoryLabel || undefined) : undefined,
    bizDomain: props.selectedNode?.id !== 'all' ? (props.selectedNode?.domainLabel || undefined) : undefined
  })

  const entries = res.data || []
  tableData.value = buildTree(entries)
  nextTick(initSortable)
}
```

- [ ] **Step 4: 删除前端二次过滤与旧 tag 字段依赖**

```js
// 删除以下逻辑：
// entries = entries.filter(e => e.level && e.level >= 3)
// if (props.selectedNode && props.selectedNode.id !== 'all') { ... }
```

- [ ] **Step 5: 执行前端构建检查**

Run: `npm run build`
Expected: PASS

---

### Task 4: 给前端页面增加只读态与交互保护

**Files:**
- Modify: `frontend/src/components/DataListTab.vue`
- Modify: `frontend/src/views/dashboard/DataWorkbench.vue`

- [ ] **Step 1: 定义统一只读态并在模板上禁用按钮/勾选/保存**

```js
const isReadonly = computed(() => !props.isEditing)
```

```vue
<el-button
  v-if="props.selectedNode?.level === 2"
  type="primary"
  size="small"
  :disabled="isReadonly"
  @click="openNewDialog"
>新建</el-button>

<el-checkbox
  :model-value="hasVer(row, 'A-曜系列')"
  :disabled="isReadonly"
  @change="toggleVer(row, 'A-曜系列')"
  size="small"
>曜</el-checkbox>

<el-button type="primary" :disabled="isReadonly" @click="saveEdit">保存</el-button>
```

- [ ] **Step 2: 为行内操作增加只读样式与方法级保护**

```vue
<span class="op-btn op-edit" :class="{ disabled: isReadonly }" @click="editRow(row)">编辑</span>
<span class="op-btn op-add" :class="{ disabled: isReadonly }" @click="addChildRow(row)">添加</span>
<span class="op-btn op-del" :class="{ disabled: isReadonly }" @click="deleteRow(row)">删除</span>
```

```js
function guardReadonly() {
  if (!props.isEditing) {
    ElMessage.warning('已发版版本不可修改')
    return true
  }
  return false
}

function editRow(row) {
  if (guardReadonly()) return
  isNew.value = false
  editingId.value = row.id
  parentRow.value = null
  Object.assign(editForm, row)
  syncVersionFromForm()
  showEditDialog.value = true
}
```

- [ ] **Step 3: 在 toggleVer / saveEdit / deleteRow / openNewDialog / addChildRow 中统一调用 guardReadonly**

```js
async function toggleVer(row, ver) {
  if (guardReadonly()) return
  const parts = (row.colVersionDivision || '').split(' ').filter(Boolean)
  const idx = parts.indexOf(ver)
  if (idx >= 0) parts.splice(idx, 1)
  else parts.push(ver)
  row.colVersionDivision = parts.join(' ')
  await updateEntry(row.id, { colVersionDivision: row.colVersionDivision })
  handleQuery()
}

async function saveEdit() {
  if (guardReadonly()) return
  syncVersionToForm()
  // 保留现有创建/更新逻辑
}
```

- [ ] **Step 4: 在 initSortable 中关闭 released 版本拖拽初始化**

```js
function initSortable() {
  sortableInstances.forEach(s => s.destroy())
  sortableInstances = []
  if (!props.isEditing) return

  nextTick(() => {
    const tbody = document.querySelector('.el-table__body-wrapper tbody')
    if (!tbody) return
    const inst = Sortable.create(tbody, {
      handle: '.drag-col',
      animation: 150,
      onMove(evt) {
        const dragLevel = getRowLevel(evt.dragged)
        const relatedLevel = getRowLevel(evt.related)
        if (dragLevel !== relatedLevel) return false
        if (dragLevel === 3) return true
        return getRowParent(evt.dragged) === getRowParent(evt.related)
      },
      onEnd: async () => {
        if (guardReadonly()) return
        // 保留原有 payload 计算逻辑
      }
    })
    sortableInstances.push(inst)
  })
}
```

- [ ] **Step 5: 在工作台页增加只读提示文案**

```vue
<div class="version-info">
  <span class="version-badge">v{{ selectedVersion.versionNo }}</span>
  <span class="version-date" v-if="selectedVersion.releasedAt">发布: {{ formatDate(selectedVersion.releasedAt) }}</span>
  <el-tag v-if="selectedVersion.status === 'draft'" type="warning" size="small">编辑中</el-tag>
  <el-tag v-else type="success" size="small">已发布</el-tag>
  <span v-if="selectedVersion.status !== 'draft'" class="readonly-tip">已发版版本不可修改</span>
</div>
```

- [ ] **Step 6: 再次执行前端构建检查**

Run: `npm run build`
Expected: PASS

---

### Task 5: 做整体验证并记录人工验收路径

**Files:**
- Modify: `docs/superpowers/plans/2026-05-26-document-numbering-release-lock-and-filters.md`

- [ ] **Step 1: 运行后端全部测试**

Run: `mvn test`
Expected: PASS

- [ ] **Step 2: 运行后端打包验证**

Run: `mvn package`
Expected: BUILD SUCCESS

- [ ] **Step 3: 运行前端构建验证**

Run: `npm run build`
Expected: vite build 成功，无类型/语法错误

- [ ] **Step 4: 按以下人工验收脚本联调**

```text
1. 登录系统，选择 draft 版本。
2. 在“数据清单”中验证：名称/状态/产品经理/解决方案/版本 五个筛选项均可使用。
3. 选择某业务分类和业务域，确认查询结果已由后端直接返回，无前端二次过滤痕迹。
4. 在 draft 版本中验证：新建、编辑、删除、拖拽排序、版本划分勾选都可成功。
5. 发布一个版本或切换到 released 版本，确认：新建按钮禁用、编辑/添加/删除被阻止、版本划分勾选不可改、拖拽不可用。
6. 直接用浏览器开发者工具或接口工具调用写接口，确认后端返回“已发版版本不允许修改清单”。
7. 在文档生成中执行“整个版本”导出，确认功能说明文档标题编号为 1 / 1.1 / 1.1.1 结构。
8. 在文档生成中执行“仅勾选产品”导出，确认导出结果中的编号重新从 1 开始。
9. 检查同级多个根节点是否按 sortOrder 输出，且编号递增稳定。
```

- [ ] **Step 5: 若验证通过，记录最终完成标准**

```text
完成标准：
- 后端测试全部通过
- 前端构建通过
- released 版本前后端都无法修改清单
- 查询条件与左树过滤统一到后端
- 功能说明文档编号仅由最终导出目录树决定
- 部分导出时编号重新从 1 开始
```
