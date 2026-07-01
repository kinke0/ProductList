# 智能化标签功能实施计划

## 目标
在数据清单中增加"智能化"布尔标签（colIntelligent），包括编辑表单复选框、清单"AI"冒泡标签、查询过滤条件。

## 根因/背景
后端 DataEntry/DataEntryDTO/DataEntrySummaryDTO 已有 `colIntelligent` 字段，copyFields 和 fromEntity 已处理此字段。但数据库缺列、后端查询缺参数、前端缺 UI。

## 涉及文件
1. `db_changes/V1.0.12_add_col_intelligent.sql` — ALTER TABLE 加列
2. `src/main/java/com/superpower/modules/data/controller/DataEntryController.java` — query 接口增加 intelligent 参数
3. `src/main/java/com/superpower/modules/data/service/DataEntryService.java` — query 方法增加 intelligent 过滤逻辑
4. `frontend/src/components/DataListTab.vue` — 编辑表单、清单显示、查询过滤
5. `VERSION.md` — 变更说明

## 分步实施

### Step 1: 数据库 ALTER TABLE
创建 `db_changes/V1.0.12_add_col_intelligent.sql`：
```sql
ALTER TABLE data_entry ADD COLUMN col_intelligent TEXT DEFAULT '';
```
注意：SQLite 没有 BOOLEAN 类型，使用 TEXT 存储（"1"=勾选，""=未勾选），与现有字段类型一致。

### Step 2: 后端 query 接口增加 intelligent 参数
DataEntryController.java 第115行 query 方法增加：
```java
@RequestParam(required = false) String intelligent
```

DataEntryService.java query 方法增加 `String intelligent` 参数，在过滤逻辑中：
- intelligent="1" 时，过滤结果中只保留自身 colIntelligent="1" 的条目，或其子树中包含 colIntelligent="1" 的父条目

### Step 3: 前端编辑表单
- `editForm` 增加 `colIntelligent: ''` 字段
- `initialFormState` 增加 `colIntelligent: ''`
- 编辑弹窗中版本划分区域下方增加复选框"智能化"

### Step 4: 前端清单显示
- 名称列中，产品名后、备注徽标前，增加"AI"标签
- 样式：圆角矩形蓝紫色背景，白色小字"AI"
- 自身勾选时直接显示
- 冒泡逻辑：类似备注的 getRemarks，新增 getIntelligentChildren 函数
  - 父节点未展开 + 有智能化子节点 → 显示"AI"标签
  - 悬浮 tooltip → 显示智能化子节点的章节名称列表
  - 父节点展开后 → 各子节点独立显示，父自身不勾选则不显示

### Step 5: 前端查询过滤
- queryForm 增加 `intelligent: false` 字段
- 查询栏增加复选框"智能化"
- handleQuery 传递 intelligent 参数

### Step 6: VERSION.md 更新

### Step 7: 验证
