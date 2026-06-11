# 拖拽升降级/排序修复 + 右键菜单上移下移 L3 支持

## 目标
1. 修复拖拽降级跳级问题（nest 模式改用 moveToSibling 而非 moveToParent）
2. 修复拖拽到同域最上方/最下方指示线消失问题（fallback 双向搜索）
3. 修复 updateDragIndicator 中 baseIndent 未定义变量报错
4. 修复上移/下移在 L3 级（parentId=null）不生效问题
5. 文字修改：粘贴到下方 → 粘贴到同级

## 涉及文件

| 文件 | 改动点 |
|------|--------|
| `frontend/src/components/DataListTab.vue` | applyNestMove 改调用 moveToSibling；fallback 双向搜索；baseIndent→srcIndent 修复；文字修改 |
| `src/.../data/repository/DataEntryRepository.java` | 新增 findRootEntries（parentId IS NULL） |
| `src/.../data/service/DataEntryService.java` | moveUp/moveDown 支持 parentId=null |

## 分步实施

### 步骤 1：前端 DataListTab.vue — applyNestMove 改为 moveToSibling
- **位置**: applyNestMove 函数中 `moveToParent` 调用
- **改动**: `await moveToParent(sourceRow.id, targetId)` → `await moveToSibling(sourceRow.id, targetId)`
- **原因**: nest 模式（偏右）语义应为"变为目标的同级下一个兄弟"，而非"变为目标的子节点"。moveToParent 会设置 `newLevel = target.level + 1`，导致跨级跳变（如 L3→L5）

### 步骤 2：前端 DataListTab.vue — fallback 双向搜索
- **位置**: dragMoveHandler 中 else 分支（fallback 搜索逻辑）
- **改动**: 将单向递减搜索改为双向搜索
  - 先向下搜索同域有效行（处理拖到域分隔行/顶部的场景），`detectedSortEnd = false`
  - 向下没找到再向上搜索（处理域底部场景），`detectedSortEnd = true`
  - 向下搜索遇到不同域的分隔行即停止
- **原因**: 当拖拽到同域最上方的分隔行区域时，fallback 只向上搜索找不到同域有效行，targetIdx 回退到 source 自身，指示线消失

### 步骤 3：前端 DataListTab.vue — baseIndent 变量修复
- **位置**: updateDragIndicator 函数中第 1012 行
- **改动**: `baseIndent` → `srcIndent`
- **原因**: baseIndent 未定义，运行时会产生 ReferenceError

### 步骤 4：前端 DataListTab.vue — 文字修改
- **位置**: 右键菜单中"粘贴到下方"
- **改动**: `粘贴到下方` → `粘贴到同级`

### 步骤 5：后端 DataEntryRepository — 新增 findRootEntries
- **改动**: 新增方法
  ```java
  @Query("SELECT e FROM DataEntry e WHERE e.versionId = :versionId AND e.parentId IS NULL ORDER BY e.sortOrder")
  List<DataEntry> findRootEntries(@Param("versionId") Long versionId);
  ```

### 步骤 6：后端 DataEntryService — moveUp/moveDown 支持 L3
- **改动**: moveUp 和 moveDown 方法中获取兄弟列表的逻辑增加 null 判断
  ```java
  List<DataEntry> siblings = (parentId != null)
      ? entryRepository.findByVersionIdAndParentIdOrderBySortOrder(versionId, parentId)
      : entryRepository.findRootEntries(versionId);
  ```
- **原因**: L3 级节点 parentId 为 null，JPA `findByVersionIdAndParentId(versionId, null)` 生成 `parent_id = null`（SQL 中永远为 false），查不到任何记录

## 验证步骤
1. `npm run build` 前端构建无错误
2. `mvn compile` 后端编译无错误
3. 重启前后端服务
4. curl 验证 8080 和 5173 端口返回 200
5. curl 调用 `/api/versions` 检查无 500 错误
6. 功能测试：
   - L3 级节点上移/下移是否生效
   - 拖拽偏右降级是否变为同级兄弟（而非跳级到子节点）
   - 拖拽到域分隔行区域指示线是否正常显示
   - 右键粘贴菜单显示"粘贴到同级"
