# V1.0.12 图片重命名事务一致性缺陷修复

## 目标

消除"图片重命名后物理文件已改名，但数据库 url 未更新"的不一致窗口，并增强服务器网络延迟场景下的健壮性。

## 根因

`ImageResourceService.update()` 标注 `@Transactional`，方法内 `Files.move`（不可回滚）早于事务提交执行。一旦后续 DB 写入因 SQLite 锁等待超时（`SQLITE_BUSY`，弱网下高发）或引用同步异常而回滚，物理改名无法撤销，产生不一致。

服务器网络延迟通过三个机制放大该 Bug：
1. Phase 3 引用同步慢 → 事务持锁久 → `busy_timeout=30s` 易耗尽
2. 前端 60s 超时 → 用户重试 → 并发竞争加剧锁冲突
3. HTTP 超时后后端事务不终止 → 孤儿事务持续持锁 → 雪崩

## 修复方案（方案 A：先改库后改文件）

将文件系统操作（`Files.move`）和引用同步（`syncImageNameInReferences`）从 `@Transactional` 事务体内移至 `TransactionSynchronization.afterCommit` 回调，确保只有数据库事务确认提交后才执行物理文件操作。

## 涉及文件

| 文件 | 改动 |
|------|------|
| `src/main/java/com/superpower/modules/image/service/ImageResourceService.java` | 重构 `update()`；新增 `recomputeImageLocation()`；修复 `moveImageFile` 吞异常 |
| `src/main/java/com/superpower/modules/data/service/DataEntryService.java` | 修复 `syncEntryImageClassifications` 与 `moveImageFile` |
| `VERSION.md` | V1.0.12 变更说明追加 |

## 分步实施

### 改动 1：ImageResourceService.update() 重构
- 事务内：纯计算 → 改实体字段（stored_name/path/url）→ `saveAndFlush`
- `afterCommit`：执行 `Files.move(originalPath, finalPath)` + 引用同步
- 合并改名/目录变更的文件操作为一次 move（原代码分两次）
- 记录 `originalPath`（改实体前）用于 afterCommit 移动源

### 改动 2：新增 recomputeImageLocation()
从 `moveImageFile` 抽取计算部分：根据 category/domain/product/storedName 计算 path/url 并 set 到实体（不移动文件）

### 改动 3：修复 ImageResourceService.moveImageFile 吞异常
`log.warn` → `log.error`（含完整上下文 imageId/oldPath/newPath）

### 改动 4：DataEntryService.syncEntryImageClassifications
将循环中的 `moveImageFile(img)` 拆为：事务内 `recomputeImageLocation` + save；afterCommit 批量移动文件

### 改动 5：DataEntryService.moveImageFile 吞异常修复
同改动 3

## 验证
1. `mvn compile -Dmaven.test.skip=true` 编译通过
2. 重启后端
3. 测试图片改名：DB 与文件名一致 + 引用同步正常
4. `/api/app-version` 返回 V1.0.12 beta
5. 后端日志无 500
