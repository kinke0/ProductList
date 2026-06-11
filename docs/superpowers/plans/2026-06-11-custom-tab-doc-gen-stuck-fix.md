# 自定义Tab Excel生成卡在100%修复

## 目标
修复自定义Tab点击"生成文档"后直接点生成，进度到100%但卡住不完成的问题。

## 问题根因
1. `onGenerateDoc()` 没有重置 `docFormat`，默认值是 `'word'`。用户打开生成对话框后直接点击生成，实际发送 `format: 'word'` 而非 `format: 'excel'`
2. Word 生成路径在自定义Tab大量entries+图片处理场景下，`doc.write(out)` 可能失败，导致文件未生成但进度已到100%
3. `updateGenRecordSuccess` 和 `updateGenRecordProgress` 无重试/容错，SQLite锁竞争时静默失败
4. 前端 `pollProgress` 的 catch 静默吞错误，无超时保护

## 涉及文件
1. `frontend/src/views/dashboard/DataWorkbench.vue` — onGenerateDoc重置docFormat + 轮询增强
2. `src/main/java/com/superpower/modules/document/service/DocumentService.java` — 重试 + try-catch
3. `src/main/java/com/superpower/modules/document/controller/DocumentController.java` — 兜底确认

## 改动说明

### 1. DataWorkbench.vue — onGenerateDoc 重置所有表单状态
- 设置 `docFormat.value = 'excel'`（自定义Tab默认Excel）
- 设置 `docType.value = 'feature'`、`dataScope.value = 'all'`

### 2. DocumentService.java — updateGenRecordSuccess 增加重试
- 3次重试，每次间隔200ms
- 添加日志记录

### 3. DocumentService.java — updateGenRecordProgress 增加 try-catch
- 失败不影响主流程

### 4. DataWorkbench.vue — pollProgress 增强
- catch 中添加 console.error
- 进度100%后30秒未变completed → 强制停止并刷新
- 总超时5分钟

### 5. DocumentController.java — 兜底确认
- generateAndSaveDocument成功返回后再确认状态为completed

## 验证
- `mvn compile` 编译通过
- `npm run build` 构建通过
- curl 测试 Excel/Word 生成均正常完成
