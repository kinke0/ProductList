# 实施计划：文档生成进度卡 0% 修复

## 目标
解决文档生成（尤其是 Excel）进度卡在 0% 不动的问题。

## 根因
1. `createGenRecord()` 未预设 `totalEntries`/`processedEntries`，初始为 null，前端 `null || 0` 始终为 0
2. `generateExcel()` 方法完全没有进度更新调用，`processedEntries` 永远为 0
3. `new Thread()` 裸线程无并发控制，多次点击创建多个线程竞争 SQLite 写锁
4. Excel 的 `totalSize` 用的是过滤前的数量，进度百分比不准

## 涉及文件

| 文件 | 改动 |
|------|------|
| `DocumentService.java` | `createGenRecord` 预设进度；`generateExcel` 添加进度更新；过滤后重算 totalSize |
| `DocumentController.java` | `new Thread()` 改为注入 `TaskExecutor` |
| `DataWorkbench.vue` | 进度显示处理 null 的逻辑优化 |

## 分步实施

### 第 1 步：`createGenRecord` 预设进度字段
`totalEntries=0`、`processedEntries=0`

### 第 2 步：`generateAndSaveDocument` 入口立即标记开始
查询 entries 前先更新 status 和初始进度

### 第 3 步：`generateExcel` 添加进度更新
过滤 entries 后重算 totalSize，行写入循环中调用 `updateGenRecordProgress`

### 第 4 步：`new Thread()` 改为 `TaskExecutor`
注入 Spring TaskExecutor，限制并发为 1，避免 SQLite 锁竞争

## 验证
1. `mvn compile` 后端编译通过
2. `npm run build` 前端构建通过
