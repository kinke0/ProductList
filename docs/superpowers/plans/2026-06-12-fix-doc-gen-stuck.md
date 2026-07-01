# 修复文档生成卡在 generating 状态（processed=0）

## 问题
服务器上文档生成（Excel + Word）永远卡在 generating 状态，processed=0，文件不存在。
本机正常，服务器每次都卡。Docker 部署，内存充足（31GB）。

## 根因分析
1. `DocumentController` 使用 `newSingleThreadExecutor`，只有一个工作线程
2. 如果之前任务阻塞，后续任务排队永远等待
3. `HikariCP maximum-pool-size: 1` 未设置 `connection-timeout`，可能导致连接获取无限等待
4. 整个生成流程缺少日志，无法定位卡点
5. Excel 进度报告在 `wb.write()` 之前就报 100%

## 涉及文件
1. `src/.../document/controller/DocumentController.java` — 线程池扩容 + 日志
2. `src/.../document/service/DocumentService.java` — 关键日志 + 进度修复 + 重试增强
3. `src/main/resources/application.yml` — HikariCP 连接超时
4. `frontend/src/views/dashboard/DataWorkbench.vue` — 超时延长

## 实施步骤

### 步骤1：DocumentController.java
- `newSingleThreadExecutor` → `newFixedThreadPool(3)`
- 添加任务提交/完成/异常日志

### 步骤2：DocumentService.java
- `generateAndSaveDocument` 每个关键节点添加 log.info
- `updateGenRecordProgress` catch 块：`// ignored` → `log.warn`
- `updateGenRecordSuccess` 重试次数 3→10，间隔 200ms→1000ms
- `generateExcel` 行1330 的进度100%移到 `wb.write()` 之后

### 步骤3：application.yml
- 添加 `connection-timeout: 60000`
- 添加 `leak-detection-threshold: 30000`

### 步骤4：DataWorkbench.vue
- 100%后超时 30s → 120s
