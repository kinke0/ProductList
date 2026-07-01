# Playwright E2E 业务流程测试设计 - 第1批：产品清单

## 目标

为产品清单模块补充完整的业务流程功能测试，覆盖CRUD、右键菜单、批量操作、版本划分、审批、文档生成等全部核心功能操作。当前仅57个基础可见性测试，本批次新增约41个深度交互测试场景。

## 测试范围

### 产品清单模块全量业务操作

| 组别 | 场景数 | 测试内容 |
|------|--------|---------|
| CRUD闭环 | 6 | 新建条目、编辑条目保存、添加子条目、删除条目确认、查看详情、分隔行添加产品 |
| 右键菜单 | 8 | 复制、剪切、粘贴到同级、粘贴到下级、升级、降级、上移、下移 |
| 批量操作 | 6 | 批量修改状态、批量修改解决方案(替换/追加)、批量指定产品经理、批量修改业务分类域、批量版本划分、批量删除 |
| 版本划分 | 5 | 曜勾选/取消、远勾选/取消、驰勾选/取消、非标配勾选、最小集标记 |
| 搜索筛选 | 5 | 按名称搜索、按状态筛选、按解决方案筛选、按产品经理筛选、智能化过滤 |
| 侧边栏 | 3 | 树节点筛选、展开/折叠子节点、收缩/展开侧边栏 |
| 编码重排序 | 2 | 打开编码重排序对话框、执行重排序 |
| 审批流程 | 3 | 提交审批、通过审批、驳回审批 |
| 文档生成 | 2 | 生成Word、生成Excel |
| 版本切换 | 2 | 切换版本对话框、确认切换版本 |
| 智能化标签 | 2 | 编辑表单勾选智能化、AI徽标显示验证 |
| 预览 | 1 | 点击预览按钮 |
| 插入待生成清单 | 1 | 点击插入按钮 |

## 核心技术挑战

### 1. 虚拟滚动列表 (RecycleScroller)
DataListTab 使用 `vue-virtual-scroller` 的 RecycleScroller，只有可见行被渲染。
- **策略**: 使用 `page.mouse.wheel(0, delta)` 滚动定位目标行
- **定位行**: 通过 `.row-id-{id}` 类名精确定位（如 `.row-id-42`）
- **获取行ID**: 先从API获取数据确定ID，再定位行

### 2. 自定义右键菜单 (Teleport + 绝对定位)
右键菜单通过 `<Teleport to="body">` 渲染到body层，绝对定位在鼠标位置。
- **触发**: `rowLocator.click({ button: 'right' })`
- **定位菜单项**: `page.locator('.ctx-menu .ctx-menu-item').filter({ hasText: '复制' })`

### 3. 自定义拖拽 (mousedown/mousemove/mouseup)
拖拽使用原生鼠标事件，不是HTML5 drag-and-drop API。
- **触发拖拽**: 找到 `.drag-handle`，使用 `page.mouse.move()` + `page.mouse.down()` + `page.mouse.move(targetX, targetY)` + `page.mouse.up()`
- **验证拖拽结果**: 比较拖拽前后行顺序

### 4. 版本划分3态复选框
`toggleVer` 实现了3态循环：点击1次=勾选系列，点击2次=标记最小集(绿色)，点击3次=取消。
- **定位**: `rowLocator.locator('.version-inline').getByRole('checkbox', { name: '曜' })`
- **验证**: 检查 `.ver-min-badge` CSS类是否出现

### 5. 批量操作下拉菜单
批量操作通过 `el-dropdown` 触发，菜单项通过 `command` 属性区分。
- **触发**: `page.getByRole('button', { name: '其他批量操作' }).click()` → 等待dropdown出现 → `page.getByRole('menuitem', { name: '状态修改' }).click()`

## Page Object 扩展设计

### DataListPage 新增定位器与方法

```typescript
// === 定位器 ===
// 工具栏按钮
readonly newButton: Locator;              // 新建按钮 (level=2时可见)
readonly insertButton: Locator;           // 插入待生成清单按钮
readonly batchSubmitButton: Locator;      // 批量提交按钮
readonly batchApproveButton: Locator;     // 批量通过按钮
readonly batchDropdown: Locator;          // 其他批量操作下拉
readonly renumberButton: Locator;         // 编码重排序按钮
readonly generateWordButton: Locator;     // 生成Word文档按钮
readonly generateExcelButton: Locator;    // 生成Excel按钮

// 虚拟表格行定位
readonly virtualTable: Locator;           // .virtual-table
readonly firstDataRow: Locator;           // 第一条非分隔行
readonly rowById(id: number): Locator;    // .row-id-{id}

// 操作按钮 (在行内)
readonly editBtn(row): Locator;           // .op-btn.op-edit "编辑"
readonly addChildBtn(row): Locator;       // .op-btn.op-add "添加"
readonly deleteBtn(row): Locator;         // .op-btn.op-del "删除"
readonly viewBtn(row): Locator;           // .op-btn.op-edit "查看"
readonly previewBtn(row): Locator;        // .op-btn.op-add "预览"

// 版本划分复选框 (在行内)
readonly verYaoCheckbox(row): Locator;    // 曜 checkbox
readonly verYuanCheckbox(row): Locator;   // 远 checkbox
readonly verChiCheckbox(row): Locator;    // 驰 checkbox
readonly verNonStdCheckbox(row): Locator; // 非标配 checkbox

// 编辑对话框
readonly editDialog: Locator;             // 编辑对话框
readonly productNameInput: Locator;       // 产品名称输入框
readonly businessCategorySelect: Locator; // 业务分类下拉
readonly businessDomainSelect: Locator;   // 业务域下拉
readonly intelligentCheckbox: Locator;    // 智能化复选框
readonly saveButton: Locator;             // 保存按钮
readonly cancelButton: Locator;           // 取消按钮

// 右键菜单
readonly contextMenu: Locator;            // .ctx-menu
readonly ctxCopyItem: Locator;            // 复制菜单项
readonly ctxCutItem: Locator;             // 剪切菜单项
readonly ctxPasteSibling: Locator;        // 粘贴到同级
readonly ctxPasteChild: Locator;          // 粘贴到下级
readonly ctxLevelUp: Locator;             // 升级
readonly ctxLevelDown: Locator;           // 降级
readonly ctxMoveUp: Locator;              // 上移
readonly ctxMoveDown: Locator;            // 下移

// 批量对话框
readonly batchStatusDialog: Locator;      // 批量修改状态对话框
readonly batchSolutionDialog: Locator;    // 批量修改解决方案对话框
readonly batchManagerDialog: Locator;     // 批量指定产品经理对话框
readonly batchCategoryDialog: Locator;    // 批量修改分类域对话框
readonly batchVersionDialog: Locator;     // 批量版本划分对话框

// === 方法 ===
async selectRow(rowId: number): Promise<void>;
async selectMultipleRows(rowIds: number[]): Promise<void>;
async scrollToRow(rowId: number): Promise<void>;
async openEditDialog(rowId: number): Promise<void>;
async openNewDialog(): Promise<void>;
async fillEditForm(data: EditFormData): Promise<void>;
async saveEdit(): Promise<void>;
async deleteRow(rowId: number): Promise<void>;
async rightClickRow(rowId: number): Promise<void>;
async clickContextMenuItem(name: string): Promise<void>;
async dragRowTo(fromId: number, toId: number): Promise<void>;
async toggleVersionDivision(rowId: number, series: string): Promise<void>;
async openBatchDialog(command: string): Promise<void>;
async executeBatchOperation(command: string, data: any): Promise<void>;
```

## 测试文件组织

### 新增文件
- `e2e/pages/data-list-functional.page.ts` — 业务交互Page Object（扩展现有DataListPage）
- `e2e/specs/product/data-list-crud.spec.ts` — CRUD闭环测试 (6个)
- `e2e/specs/product/data-list-context-menu.spec.ts` — 右键菜单测试 (8个)
- `e2e/specs/product/data-list-batch.spec.ts` — 批量操作测试 (6个)
- `e2e/specs/product/data-list-version-division.spec.ts` — 版本划分测试 (5个)
- `e2e/specs/product/data-list-search.spec.ts` — 搜索筛选测试 (5个，替换现有基础测试)
- `e2e/specs/product/data-list-advanced.spec.ts` — 编码重排序+审批+文档生成+智能化+预览+插入+版本切换 (11个)

### 复用现有文件
- `e2e/pages/data-list.page.ts` — 基础定位器保留，新增业务定位器和方法
- `e2e/specs/product/data-list.spec.ts` — 保留现有12个基础测试（可见性、导航等），不冲突

## 数据管理策略

### 测试数据原则
- **创建测试**: 在草稿版本下创建新条目 → 验证创建成功 → 测试结束后删除
- **编辑测试**: 先创建条目 → 编辑修改 → 验证修改生效 → 删除测试数据
- **删除测试**: 先创建条目 → 执行删除 → 验证删除成功
- **搜索测试**: 使用现有数据搜索验证，不创建新数据

### beforeEach 数据准备
```typescript
test.beforeEach(async ({ page }) => {
  // 1. 导航到dashboard
  // 2. 选择草稿版本
  // 3. 切换到数据清单tab
  // 4. 确保进入编辑模式（草稿版本默认可编辑）
});
```

### 测试隔离
- 每个测试在草稿版本下操作，不影响已发布版本
- 创建的测试数据带标记名称（如 "E2E测试-新建"），便于识别和清理
- 使用 `test.afterEach` 或 `test.afterAll` 清理测试数据

## 实施优先级

**第一批实施**（核心CRUD + 右键菜单）:
1. CRUD闭环测试 (data-list-crud.spec.ts)
2. 右键菜单测试 (data-list-context-menu.spec.ts)

**第二批实施**（批量操作 + 版本划分）:
3. 批量操作测试 (data-list-batch.spec.ts)
4. 版本划分测试 (data-list-version-division.spec.ts)

**第三批实施**（搜索 + 高级功能）:
5. 搜索筛选测试 (data-list-search.spec.ts)
6. 高级功能测试 (data-list-advanced.spec.ts)

## Element Plus 交互策略备忘

| 组件 | Playwright定位方式 |
|------|-------------------|
| el-select | `getByPlaceholder()` + `.click()` → 等待选项出现 → `getByRole('option', { name })` |
| el-checkbox | `getByRole('checkbox', { name })` 或 `locator('.el-checkbox').filter({ hasText })` |
| el-dialog | `page.getByRole('dialog')` |
| el-dropdown | 触发按钮 `.click()` → `page.getByRole('menuitem', { name })` |
| el-message-box | `page.getByRole('dialog', { name })` → `getByRole('button', { name: '确认' })` |
| el-radio-button | `getByRole('radio', { name })` + `.click({ force: true })` |
| 自定义右键菜单 | `.click({ button: 'right' })` → `.ctx-menu .ctx-menu-item` filter |
