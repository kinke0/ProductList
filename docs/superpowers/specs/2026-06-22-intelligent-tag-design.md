# 智能化标签功能设计规格

## 功能概述
在数据清单中新增"智能化"布尔标记字段，用于标识条目是否具备智能化能力。编辑表单增加复选框，清单中显示"AI"圆角矩形标签（冒泡式），查询栏增加过滤条件。

## 业务规则
- 每个条目可勾选"智能化"（布尔值，默认不勾选）
- 勾选后，该条目在清单名称列显示圆角矩形"AI"标签
- 冒泡逻辑：父节点未展开时，如果子节点中有勾选智能化的，父节点显示"AI"标签；鼠标悬浮时显示具体被勾选智能化的章节名称列表
- 展开后，各子节点单独显示各自的"AI"标签
- 查询过滤：勾选查询栏"智能化"复选框后，只显示勾选了智能化的条目或包含智能化子节点的父条目

## 数据模型
- `data_entry` 表新增 `col_intelligent` 列：INTEGER 类型，默认值 0（0=未勾选，1=勾选）
- `DataEntry.java` 实体新增 `colIntelligent` Boolean 字段
- `DataEntryDTO.java` 新增 `colIntelligent` 字段
- `DataEntrySummaryDTO.java` 新增 `colIntelligent` 字段

## 前端设计

### 编辑表单
- 位置：版本划分区域（曜/远/驰/非标配）下方，新增一行"智能化"复选框
- 字段：`editForm.colIntelligent`
- 交互：勾选/取消勾选，保存时写入数据库

### 清单显示
- 标签样式：圆角矩形，蓝紫色/渐变背景，中间写"AI"文字，字体小号白色
- 显示位置：名称列中，紧跟产品名称文字之后，备注徽标之前
- 自身勾选：直接显示"AI"标签
- 冒泡显示：
  - 父节点未展开且有智能化子节点 → 显示"AI"标签（不带计数）
  - 悬浮 tooltip → 显示所有被勾选智能化的子节点章节名称列表
  - 父节点展开后 → 各子节点独立显示各自的标签，父节点自身未勾选则不显示

### 查询过滤
- 查询栏增加一个复选框"智能化"
- `queryForm` 增加 `intelligent` 字段（Boolean）
- 勾选后调用 queryEntries 时传递 `intelligent=true` 参数
- 后端返回结果：包含自身勾选智能化的条目，或包含智能化子节点的父条目（冒泡匹配）

## 后端设计

### 查询接口
- `DataEntryController.query` 增加 `@RequestParam(required = false) Boolean intelligent` 参数
- `DataEntryService.query` 增加 `Boolean intelligent` 参数
- 过滤逻辑：
  - intelligent=true 时，返回自身 colIntelligent=true 的条目
  - 同时返回包含智能化子节点的父条目（通过递归查询子树判断）

### CRUD 接口
- 创建/更新时 colIntelligent 字段随 editForm 保存
- 无需新增独立接口

## 涉及文件清单
1. `db_changes/V1.0.12_add_col_intelligent.sql` — ALTER TABLE
2. `src/main/java/com/superpower/modules/data/entity/DataEntry.java` — 实体字段
3. `src/main/java/com/superpower/modules/data/dto/DataEntryDTO.java` — DTO字段
4. `src/main/java/com/superpower/modules/data/dto/DataEntrySummaryDTO.java` — SummaryDTO字段
5. `src/main/java/com/superpower/modules/data/service/DataEntryService.java` — query 方法增加参数
6. `src/main/java/com/superpower/modules/data/controller/DataEntryController.java` — query 接口增加参数
7. `frontend/src/components/DataListTab.vue` — 编辑表单、清单显示、查询过滤
8. `VERSION.md` — 变更说明
