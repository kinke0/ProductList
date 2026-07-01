# 批量修改版本划分实施计划

**日期：** 2026-06-09
**版本：** V1.0.5 beta

## 目标

在数据清单的批量操作下拉菜单中新增"版本划分"选项，弹窗中可勾选 A-曜系列 / B-远系列 / C-驰系列 / 非标配系统，每个系列勾选后还可设置最小集（是/否），确认后批量更新选中条目的版本划分和最小集属性。

## 涉及字段

| 字段 | 说明 |
|---|---|
| `colVersionDivision` | 版本划分，空格分隔多值（如 "A-曜系列 B-远系列"） |
| `colYao` | 曜最小集，"是"/"否" |
| `colYuan` | 远最小集，"是"/"否" |
| `colChi` | 驰最小集，"是"/"否" |

## 涉及文件

- `frontend/src/components/DataListTab.vue` — 唯一需修改的文件

## 实施步骤

### Step 1：下拉菜单新增选项
在 `el-dropdown-menu` 中（约第85行），"修改业务分类/业务域"之后新增 `<el-dropdown-item command="version">版本划分</el-dropdown-item>`

### Step 2：新增弹窗状态变量（约第520行）
- `showBatchVersionDialog`
- `batchVerYao / batchVerYuan / batchVerChi / batchVerNonStd`
- `batchMinYao / batchMinYuan / batchMinChi`

### Step 3：新增弹窗模板（约第448行后）
复选框选择系列，勾选后条件显示最小集开关，非标配与系列互斥。

### Step 4：onBatchCommand 新增 version 分支
重置状态变量，打开弹窗。

### Step 5：互斥逻辑 onBatchVerChange
- 选非标配 → 清除系列和最小集
- 选系列 → 清除非标配
- 取消系列 → 清除对应最小集

### Step 6：确认函数 confirmBatchVersion
- 校验至少选一个
- 组装 colVersionDivision / colYao / colYuan / colChi
- 循环调用 updateEntry 批量更新

### 无后端改动
采用前端循环调用单条更新模式。
