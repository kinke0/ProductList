# 实施计划：清单右键菜单（复制/剪切/粘贴）

## 目标

在清单的拖拽手柄上增加右键菜单，支持：**复制、剪切、粘贴到下方（同级）、粘贴到下级（子级）**。支持多选。采用剪贴板模式：先复制/剪切选中节点，再右键目标节点选择粘贴方式。支持跨业务域移动，自动同步 L1/L2 分类信息。

## 需求确认

- 跨域移动：允许，插入后同步更新 L1/L2 分类信息
- 多选：所有已勾选的节点（含其子树）
- 复制深度：深拷贝（含子节点）
- 粘贴目标：先复制/剪切 → 再右键目标节点选粘贴

## 涉及文件

| 文件 | 改动类型 | 说明 |
|------|---------|------|
| `DataEntryService.java` | 修改 | 新增 `copyEntriesToTarget` / `moveEntriesToTarget` 方法 |
| `DataEntryController.java` | 修改 | 新增 `POST /api/data/copy` 和 `PUT /api/data/move` 接口 |
| `frontend/src/api/data.js` | 修改 | 新增 `copyEntries` / `moveEntries` API |
| `frontend/src/components/DataListTab.vue` | 修改 | 剪贴板状态、右键菜单 UI、事件处理 |

## 分步实施

### 第 1 步：后端 Service — 复制方法

`DataEntryService.copyEntriesToTarget(List<Long> sourceIds, Long targetId, String mode)`

1. 校验目标节点 level >= 3
2. 获取目标节点的分类信息（category/domain/domainId/categoryId）
3. 对每个源节点：
   - 校验同一版本、非自身子节点
   - `mode=child`：新 parentId=targetId, level=target.level+1
   - `mode=sibling`：新 parentId=target.parentId, level=target.level
   - `cloneWithoutId()` 克隆根节点，设置新 parentId/level/sortOrder
   - 递归克隆子节点（`cloneDescendants` 辅助方法）
   - 跨域时同步 categoryId/domainId/colBizCategory/colBizDomain
   - `mode=sibling` 时 shiftSiblingsAfter 重排 sortOrder
   - 更新 target 的 isLeaf=false（child 模式）
   - syncEntryImageClassifications

### 第 2 步：后端 Service — 移动方法

`DataEntryService.moveEntriesToTarget(List<Long> sourceIds, Long targetId, String mode)`

1. 校验所有源节点不是目标节点的祖先
2. 对每个源节点：
   - 修改 parentId/level/sortOrder
   - 跨域时同步分类信息（含子节点）
   - 处理 levelDelta 调整子节点层级
   - 更新旧父节点 isLeaf
   - 更新新父节点 isLeaf
   - syncEntryImageClassifications

### 第 3 步：后端 Controller

- `POST /api/data/copy` — 请求体 `{ sourceIds, targetId, mode }`
- `PUT /api/data/move` — 请求体 `{ sourceIds, targetId, mode }`
- 含操作日志记录

### 第 4 步：前端 API

`data.js` 新增：
- `copyEntries(sourceIds, targetId, mode)`
- `moveEntries(sourceIds, targetId, mode)`

### 第 5 步：前端剪贴板状态

```js
const clipboard = reactive({
  mode: null,        // 'copy' | 'cut' | null
  entryIds: [],      // 源节点ID列表
})
```

### 第 6 步：前端右键菜单 UI

拖拽手柄添加 `@contextmenu.prevent="showContextMenu($event, row)"`

右键菜单项：
- **复制** — 将 selectedIds 存入剪贴板，mode='copy'
- **剪切** — 将 selectedIds 存入剪贴板，mode='cut'
- --- 分隔线 ---
- **粘贴到下方** — 剪贴板有内容时可用，mode='sibling'
- **粘贴到下级** — 剪贴板有内容时可用，mode='child'

### 第 7 步：前端粘贴逻辑

```js
async function pasteEntries(targetRow, mode) {
  const api = clipboard.mode === 'copy' ? copyEntries : moveEntries
  await api(clipboard.entryIds, targetRow.id, mode)
  if (clipboard.mode === 'cut') { clipboard.mode = null; clipboard.entryIds = [] }
  handleQuery(true)
}
```

### 第 8 步：菜单关闭

- 点击菜单外任意位置关闭
- 按 Esc 关闭
- 执行操作后自动关闭

## 关键约束

1. 源节点和目标节点必须同一 versionId
2. 源节点不能是目标节点的祖先
3. 目标 level >= 3，child 模式时目标 level + 1 为新层级
4. 跨域时同步 categoryId/domainId/colBizCategory/colBizDomain（含子节点）
5. 复制的新节点审批状态重置为"待提交"

## 验证

1. `mvn compile` 后端编译通过
2. `npm run build` 前端构建通过
3. 功能测试：单选复制/多选剪切/跨域操作/菜单交互
