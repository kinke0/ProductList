# 产品清单 E2E 业务流程测试 实施计划

**Goal:** 为产品清单模块补充41个深度业务流程功能测试，覆盖CRUD、右键菜单、批量操作、版本划分、审批、文档生成等全部核心功能操作

**Architecture:** 扩展现有 Playwright Page Object 模式，新增 `DataListFunctionalPage` 类封装复杂业务交互（虚拟滚动定位、右键菜单、拖拽、批量操作对话框），6个独立spec文件按功能分组组织测试场景

**Tech Stack:** Playwright + TypeScript + vue-virtual-scroller交互 + Element Plus组件定位

## Global Constraints

- 所有测试在草稿版本下操作，不影响已发布版本数据
- 测试数据使用标记名称（"E2E测试-"前缀），便于识别和清理
- 测试创建的数据必须在测试结束后清理（afterEach/afterAll）
- 虚拟滚动列表中只有可见行被渲染，需要滚动才能定位不可见行
- 右键菜单通过 Teleport 渲染到 body，绝对定位在鼠标位置
- 拖拽使用原生 mousedown/mousemove/mouseup 事件，非HTML5 drag-and-drop

## File Structure

### 新建文件
| 文件 | 职责 |
|------|------|
| `e2e/pages/data-list-functional.page.ts` | 业务交互Page Object：虚拟滚动定位、右键菜单、拖拽、批量对话框、编辑表单 |
| `e2e/specs/product/data-list-crud.spec.ts` | CRUD闭环测试：6个场景 |
| `e2e/specs/product/data-list-context-menu.spec.ts` | 右键菜单测试：8个场景 |
| `e2e/specs/product/data-list-batch.spec.ts` | 批量操作测试：6个场景 |
| `e2e/specs/product/data-list-version-division.spec.ts` | 版本划分测试：5个场景 |
| `e2e/specs/product/data-list-search.spec.ts` | 搜索筛选测试：5个场景 |
| `e2e/specs/product/data-list-advanced.spec.ts` | 高级功能测试：11个场景 |

### 修改文件
| 文件 | 变更 |
|------|------|
| `VERSION.md` | V1.1.0 beta 变更说明追加E2E业务流程测试条目 |

### 不修改
| 文件 | 原因 |
|------|------|
| `e2e/pages/data-list.page.ts` | 基础定位器保留，新功能定位器放在FunctionalPage |
| `e2e/specs/product/data-list.spec.ts` | 12个基础可见性测试保留不冲突 |

---

### Task 1: 创建 DataListFunctionalPage 业务交互Page Object

**Files:**
- Create: `frontend/e2e/pages/data-list-functional.page.ts`
- Test: 后续所有spec文件依赖此Page Object

**Interfaces:**
- Consumes: 现有 `DataListPage` (继承基础定位器)、`BasePage` (navigateTo等方法)、`ROUTES`、`TEST_USER`
- Produces: 完整的业务交互API供6个spec文件使用

- [ ] **Step 1: 创建 DataListFunctionalPage 文件**

继承 BasePage，封装以下定位器和方法：

```typescript
import { BasePage } from './base.page';
import { Locator, Page } from '@playwright/test';
import { ROUTES } from '../fixtures/test-data';

export class DataListFunctionalPage extends BasePage {
  // 工具栏按钮
  readonly newButton: Locator;
  readonly insertButton: Locator;
  readonly batchDropdown: Locator;
  readonly renumberButton: Locator;

  // 虚拟表格
  readonly virtualTable: Locator;
  readonly firstNonSepRow: Locator;

  // 编辑对话框
  readonly editDialog: Locator;
  readonly productNameInput: Locator;
  readonly intelligentCheckbox: Locator;
  readonly saveButton: Locator;
  readonly dialogCancelButton: Locator;
  readonly verYaoDialogCheckbox: Locator;
  readonly verYuanDialogCheckbox: Locator;
  readonly verChiDialogCheckbox: Locator;
  readonly verNonStdDialogCheckbox: Locator;

  // 右键菜单
  readonly contextMenu: Locator;

  // 批量对话框
  readonly batchStatusDialog: Locator;
  readonly batchSolutionDialog: Locator;
  readonly batchCategoryDialog: Locator;
  readonly batchVersionDialog: Locator;

  constructor(page: Page) {
    super(page);
    this.newButton = page.getByRole('button', { name: '新建' });
    this.insertButton = page.getByRole('button', { name: '插入待生成清单' });
    this.batchDropdown = page.getByRole('button', { name: '其他批量操作' });
    this.renumberButton = page.getByRole('button', { name: '编码重排序' });

    this.virtualTable = page.locator('.virtual-table');
    this.firstNonSepRow = page.locator('.vrow:not(.sep-row)').first();

    this.editDialog = page.getByRole('dialog');
    this.productNameInput = page.locator('.edit-form-compact').getByPlaceholder('产品/系统名称').or(page.locator('.edit-form-compact [label="产品/系统"] input'));
    this.intelligentCheckbox = page.locator('.intelligent-box .el-checkbox');
    this.saveButton = page.getByRole('button', { name: '保存' });
    this.dialogCancelButton = page.getByRole('button', { name: '取消' });

    this.contextMenu = page.locator('.ctx-menu');
    
    this.batchStatusDialog = page.locator('.el-dialog').filter({ hasText: '批量修改状态' });
    this.batchSolutionDialog = page.locator('.el-dialog').filter({ hasText: '批量修改解决方案' });
    this.batchCategoryDialog = page.locator('.el-dialog').filter({ hasText: '批量修改业务分类' });
    this.batchVersionDialog = page.locator('.el-dialog').filter({ hasText: '批量版本划分' });
  }

  // 核心方法
  rowById(id: number): Locator {
    return this.page.locator(`.row-id-${id}`);
  }

  async gotoDataList(): Promise<void> {
    await this.navigateTo(ROUTES.dashboard);
    // 选择版本 - 点击版本表格第一行
    const versionRow = this.page.locator('.version-pick .el-table__body .el-table__row').first();
    if (await versionRow.isVisible()) {
      await versionRow.click();
      await this.page.waitForLoadState('networkidle');
    }
    // 切换到数据清单tab
    const listTab = this.page.locator('.tab-list .el-tabs__item').filter({ hasText: '数据清单' }).or(this.page.getByText('数据清单', { exact: true }));
    if (await listTab.isVisible()) {
      await listTab.click();
      await this.page.waitForLoadState('networkidle');
    }
  }

  async rightClickRow(rowId: number): Promise<void> {
    const row = this.rowById(rowId);
    // 如果行不可见，需要先滚动
    if (!(await row.isVisible())) {
      await this.scrollToRow(rowId);
    }
    await row.click({ button: 'right' });
  }

  async rightClickFirstNonSepRow(): Promise<void> {
    await this.firstNonSepRow.click({ button: 'right' });
  }

  async clickContextMenuItem(name: string): Promise<void> {
    await this.contextMenu.locator('.ctx-menu-item').filter({ hasText: name }).click();
  }

  async selectRowCheckbox(rowId: number): Promise<void> {
    const row = this.rowById(rowId);
    if (!(await row.isVisible())) await this.scrollToRow(rowId);
    await row.locator('.el-checkbox').first().click();
  }

  async scrollToRow(rowId: number): Promise<void> {
    // 逐步滚动直到目标行可见
    const row = this.rowById(rowId);
    for (let i = 0; i < 50; i++) {
      if (await row.isVisible()) return;
      await this.page.mouse.wheel(0, 300);
      await this.page.waitForTimeout(100);
    }
  }

  async openEditForRow(rowId: number): Promise<void> {
    const row = this.rowById(rowId);
    if (!(await row.isVisible())) await this.scrollToRow(rowId);
    await row.locator('.op-btn.op-edit').filter({ hasText: '编辑' }).click();
    await this.page.waitForLoadState('networkidle');
    await expect(this.editDialog).toBeVisible();
  }

  async openEditForFirstRow(): Promise<void> {
    await this.firstNonSepRow.locator('.op-btn.op-edit').filter({ hasText: '编辑' }).click();
    await this.page.waitForLoadState('networkidle');
  }

  async fillProductName(name: string): Promise<void> {
    // 编辑对话框中的产品名称输入框
    const input = this.page.locator('.edit-form-compact').locator('input').first();
    await input.clear();
    await input.fill(name);
  }

  async saveEditForm(): Promise<void> {
    await this.saveButton.click();
    await this.page.waitForLoadState('networkidle');
  }

  async deleteRowViaButton(rowId: number): Promise<void> {
    const row = this.rowById(rowId);
    if (!(await row.isVisible())) await this.scrollToRow(rowId);
    await row.locator('.op-btn.op-del').filter({ hasText: '删除' }).click();
    // 确认删除对话框
    await this.page.getByRole('button', { name: '确认' }).or(this.page.locator('.el-message-box').getByRole('button', { name: '确认' })).click();
    await this.page.waitForLoadState('networkidle');
  }

  async openBatchDropdownCommand(command: string): Promise<void> {
    await this.batchDropdown.click();
    await this.page.getByRole('menuitem', { name: command }).click();
  }

  async confirmMessageBox(): Promise<void> {
    const confirmBtn = this.page.locator('.el-message-box__btns').getByRole('button', { name: /确认|确定/ });
    await confirmBtn.click();
  }

  async cancelMessageBox(): Promise<void> {
    const cancelBtn = this.page.locator('.el-message-box__btns').getByRole('button', { name: '取消' });
    await cancelBtn.click();
  }

  async getSuccessMessage(): Promise<string> {
    const msg = this.page.locator('.el-message--success');
    await msg.waitFor({ state: 'visible', timeout: 5000 });
    return await msg.textContent() || '';
  }
}
```

注意：`productNameInput` 定位器可能需要根据实际DOM调整，因为 DataListTab 的编辑表单 label 使用的是动态 `productLabel`（如"产品/系统"），对应input的 label 是 `:label="productLabel"`。

- [ ] **Step 2: 验证 Page Object 无编译错误**

```bash
cd frontend && npx tsc --noEmit e2e/pages/data-list-functional.page.ts
```

如有编译错误则修复。注意需要安装或确认 `@playwright/test` TypeScript 类型可用。

---

### Task 2: CRUD闭环测试 (data-list-crud.spec.ts)

**Files:**
- Create: `frontend/e2e/specs/product/data-list-crud.spec.ts`
- Depends on: Task 1 (DataListFunctionalPage)

**Interfaces:**
- Consumes: DataListFunctionalPage
- Produces: 6个CRUD测试场景

- [ ] **Step 1: 编写CRUD测试spec文件**

6个测试场景：

```typescript
import { test, expect } from '@playwright/test';
import { DataListFunctionalPage } from '../../pages/data-list-functional.page';

const TEST_NAME_PREFIX = 'E2E测试';

test.describe('产品清单-CRUD闭环', () => {
  let funcPage: DataListFunctionalPage;

  test.beforeEach(async ({ page }) => {
    funcPage = new DataListFunctionalPage(page);
    await funcPage.gotoDataList();
  });

  // 场景1: 新建条目
  test('新建条目-填写名称保存成功', async ({ page }) => {
    // 确保选中L2级别树节点（新建按钮只在level=2时可见）
    // 点击新建按钮
    // 填写产品名称
    // 保存
    // 验证成功消息
    // 验证列表中出现新条目
    // 清理：删除测试条目
  });

  // 场景2: 编辑条目-修改名称保存
  test('编辑条目-修改名称保存成功', async ({ page }) => {
    // 打开第一行的编辑对话框
    // 修改产品名称
    // 保存
    // 验证成功消息
  });

  // 场景3: 添加子条目
  test('添加子条目-在L3产品下添加L4', async ({ page }) => {
    // 找到一条L3产品行
    // 点击"添加"按钮
    // 填写子条目名称
    // 保存
    // 验证成功消息
  });

  // 场景4: 删除条目-确认删除
  test('删除条目-确认删除成功', async ({ page }) => {
    // 先创建一条测试数据
    // 点击该行的"删除"按钮
    // 确认删除对话框
    // 验证删除成功消息
  });

  // 场景5: 查看条目详情
  test('查看条目-打开查看对话框', async ({ page }) => {
    // 非编辑模式下，点击"查看"按钮
    // 验证对话框打开且字段不可编辑
  });

  // 场景6: 分隔行添加产品
  test('分隔行-点击添加产品按钮', async ({ page }) => {
    // 找到分隔行 (.sep-row)
    // 点击 "+ 添加产品/系统" 按钮 (.sep-add-btn)
    // 验证编辑对话框打开
    // 取消对话框（不创建数据）
  });
});
```

- [ ] **Step 2: 运行CRUD测试验证**

```bash
cd frontend && npx playwright test e2e/specs/product/data-list-crud.spec.ts --project=chromium
```

逐个测试调试定位器，确保6个场景全部通过。失败的定位器根据实际DOM调整。

- [ ] **Step 3: 调试并修复失败的定位器**

根据实际运行结果调整：
- `productNameInput` 定位策略
- `newButton` 可见性条件（需要选中L2树节点）
- `saveButton` 定位器
- `editDialog` 定位器
- `getSuccessMessage()` 方法

---

### Task 3: 右键菜单测试 (data-list-context-menu.spec.ts)

**Files:**
- Create: `frontend/e2e/specs/product/data-list-context-menu.spec.ts`
- Depends on: Task 1 (DataListFunctionalPage)

**Interfaces:**
- Consumes: DataListFunctionalPage.rightClickRow()、clickContextMenuItem()
- Produces: 8个右键菜单测试场景

- [ ] **Step 1: 编写右键菜单测试spec文件**

8个测试场景：

```typescript
import { test, expect } from '@playwright/test';
import { DataListFunctionalPage } from '../../pages/data-list-functional.page';

test.describe('产品清单-右键菜单', () => {
  let funcPage: DataListFunctionalPage;

  test.beforeEach(async ({ page }) => {
    funcPage = new DataListFunctionalPage(page);
    await funcPage.gotoDataList();
  });

  // 场景1: 右键菜单-复制
  test('右键复制-菜单出现并点击复制', async ({ page }) => {
    await funcPage.rightClickFirstNonSepRow();
    await expect(funcPage.contextMenu).toBeVisible();
    await funcPage.clickContextMenuItem('复制');
    // 验证成功消息包含"已复制"
  });

  // 场景2: 右键菜单-剪切
  test('右键剪切-菜单出现并点击剪切', async ({ page }) => {
    await funcPage.rightClickFirstNonSepRow();
    await funcPage.clickContextMenuItem('剪切');
    // 验证成功消息包含"已剪切"
  });

  // 场景3: 粘贴到同级
  test('复制后粘贴到同级', async ({ page }) => {
    // 先复制一条
    await funcPage.rightClickFirstNonSepRow();
    await funcPage.clickContextMenuItem('复制');
    // 再右键另一行，粘贴到同级
    // 验证粘贴成功消息
  });

  // 场景4: 粘贴到下级
  test('复制后粘贴到下级', async ({ page }) => {
    // 先复制一条
    await funcPage.rightClickFirstNonSepRow();
    await funcPage.clickContextMenuItem('复制');
    // 再右键一条L3行，粘贴到下级
    // 验证粘贴成功消息
  });

  // 场景5: 升级
  test('右键升级-L4行升级为L3', async ({ page }) => {
    // 找到一条L4行，右键点击升级
    // 验证升级成功
  });

  // 场景6: 降级
  test('右键降级-L4行降级为L5', async ({ page }) => {
    // 找到一条L4行，右键点击降级
    // 验证降级成功
  });

  // 场景7: 上移
  test('右键上移-行顺序变化', async ({ page }) => {
    await funcPage.rightClickFirstNonSepRow();
    await funcPage.clickContextMenuItem('上移');
    // 验证上移成功消息或行顺序变化
  });

  // 场景8: 下移
  test('右键下移-行顺序变化', async ({ page }) => {
    // 右键第二行，点击下移
    // 验证下移成功
  });
});
```

- [ ] **Step 2: 运行右键菜单测试验证**

```bash
cd frontend && npx playwright test e2e/specs/product/data-list-context-menu.spec.ts --project=chromium
```

重点调试：
- 右键菜单是否正确出现（Teleport到body的定位器）
- `.ctx-menu-item` 的文本匹配是否准确
- 升级/降级是否需要行level满足条件
- 粘贴是否需要先复制/剪切建立剪贴板状态

- [ ] **Step 3: 修复定位器并重新验证**

---

### Task 4: 批量操作测试 (data-list-batch.spec.ts)

**Files:**
- Create: `frontend/e2e/specs/product/data-list-batch.spec.ts`
- Depends on: Task 1 (DataListFunctionalPage)

**Interfaces:**
- Consumes: DataListFunctionalPage.selectRowCheckbox()、openBatchDropdownCommand()
- Produces: 6个批量操作测试场景

- [ ] **Step 1: 编写批量操作测试spec文件**

```typescript
import { test, expect } from '@playwright/test';
import { DataListFunctionalPage } from '../../pages/data-list-functional.page';

test.describe('产品清单-批量操作', () => {
  let funcPage: DataListFunctionalPage;

  test.beforeEach(async ({ page }) => {
    funcPage = new DataListFunctionalPage(page);
    await funcPage.gotoDataList();
    // 勾选几条数据行准备批量操作
  });

  test('批量修改状态-打开对话框并选择状态', async ({ page }) => {
    // 勾选2条行
    // 点击"其他批量操作" → "状态修改"
    // 验证批量状态对话框出现
    // 选择状态并保存
  });

  test('批量修改解决方案-替换模式', async ({ page }) => {
    // 勾选2条行
    // 点击"其他批量操作" → "解决方案"
    // 选择解决方案，选择替换模式
    // 保存
  });

  test('批量指定产品经理', async ({ page }) => {
    // 勾选2条行
    // 点击"其他批量操作" → "指定产品经理"
    // 输入产品经理名称
    // 保存
  });

  test('批量修改业务分类域-打开对话框', async ({ page }) => {
    // 勾选2条行
    // 点击"其他批量操作" → "修改业务分类/业务域"
    // 验证对话框出现，选择分类和域
  });

  test('批量版本划分-打开对话框并勾选系列', async ({ page }) => {
    // 勾选2条行
    // 点击"其他批量操作" → "版本划分"
    // 验证对话框出现，勾选曜系列
  });

  test('批量删除-确认对话框出现', async ({ page }) => {
    // 勾选2条行
    // 点击"其他批量操作" → "批量删除"
    // 验证确认删除对话框出现
    // 取消删除（不执行真实删除）
  });
});
```

- [ ] **Step 2: 运行批量操作测试验证**

```bash
cd frontend && npx playwright test e2e/specs/product/data-list-batch.spec.ts --project=chromium
```

重点调试：
- el-dropdown 的 menuitem 定位器
- 批量对话框的文本匹配定位器
- 勾选行后批量按钮的 enabled 状态

- [ ] **Step 3: 修复并重新验证**

---

### Task 5: 版本划分测试 (data-list-version-division.spec.ts)

**Files:**
- Create: `frontend/e2e/specs/product/data-list-version-division.spec.ts`
- Depends on: Task 1 (DataListFunctionalPage)

**Interfaces:**
- Consumes: DataListFunctionalPage
- Produces: 5个版本划分测试场景

- [ ] **Step 1: 编写版本划分测试spec文件**

5个场景：直接在列表行内点击版本划分checkbox（曜/远/驰/非标配）并验证状态变化

```typescript
import { test, expect } from '@playwright/test';
import { DataListFunctionalPage } from '../../pages/data-list-functional.page';

test.describe('产品清单-版本划分', () => {
  let funcPage: DataListFunctionalPage;

  test.beforeEach(async ({ page }) => {
    funcPage = new DataListFunctionalPage(page);
    await funcPage.gotoDataList();
  });

  test('行内勾选曜系列-checkbox状态变化', async ({ page }) => {
    // 找到一条L3/L4行
    // 点击曜checkbox
    // 等待API响应
    // 验证checkbox被选中
  });

  test('行内勾选远系列', async ({ page }) => {
    // 点击远checkbox
    // 验证选中状态
  });

  test('行内勾选驰系列', async ({ page }) => {
    // 点击驰checkbox
    // 验证选中状态
  });

  test('行内勾选非标配', async ({ page }) => {
    // 点击非标配checkbox
    // 验证选中状态（曜/远/驰应被禁用）
  });

  test('最小集标记-二次点击显示绿色徽标', async ({ page }) => {
    // 先勾选曜系列
    // 再次点击曜checkbox → 应标记为最小集
    // 验证 .ver-min-badge CSS类出现
  });
});
```

- [ ] **Step 2: 运行版本划分测试验证**

```bash
cd frontend && npx playwright test e2e/specs/product/data-list-version-division.spec.ts --project=chromium
```

重点调试：toggleVer 的3态循环交互逻辑

- [ ] **Step 3: 修复并重新验证**

---

### Task 6: 搜索筛选测试 (data-list-search.spec.ts)

**Files:**
- Create: `frontend/e2e/specs/product/data-list-search.spec.ts`
- Depends on: Task 1 (DataListFunctionalPage)

**Interfaces:**
- Consumes: DataListFunctionalPage、现有DataListPage的searchNameInput等
- Produces: 5个搜索筛选测试场景

- [ ] **Step 1: 编写搜索筛选测试spec文件**

5个场景：与现有 `data-list.spec.ts` 的基础搜索测试互补，增加验证搜索后数据变化的逻辑

```typescript
import { test, expect } from '@playwright/test';
import { DataListFunctionalPage } from '../../pages/data-list-functional.page';
import { DataListPage } from '../../pages/data-list.page';

test.describe('产品清单-搜索筛选', () => {
  let funcPage: DataListFunctionalPage;
  let dataListPage: DataListPage;

  test.beforeEach(async ({ page }) => {
    funcPage = new DataListFunctionalPage(page);
    dataListPage = new DataListPage(page);
    await funcPage.gotoDataList();
  });

  test('按名称搜索-结果数量减少', async ({ page }) => {
    // 记录初始记录数
    const initialCount = await page.locator('.record-count').first().textContent();
    // 输入搜索关键词
    await dataListPage.searchNameInput.fill('测试');
    await dataListPage.searchButton.click();
    await page.waitForLoadState('networkidle');
    // 验证搜索后记录数减少或变化
    const afterCount = await page.locator('.record-count').first().textContent();
    // afterCount应不同于initialCount或为0
  });

  test('按状态筛选-选择可交付', async ({ page }) => {
    // 在状态下拉选择"可交付"
    // 点击查询
    // 验证筛选生效
  });

  test('按解决方案筛选', async ({ page }) => {
    // 在解决方案下拉选择一个选项
    // 点击查询
    // 验证筛选生效
  });

  test('按产品经理筛选', async ({ page }) => {
    // 输入产品经理名称
    // 点击查询
    // 验证筛选生效
  });

  test('智能化过滤-勾选后仅显示AI标记条目', async ({ page }) => {
    // 勾选智能化checkbox
    // 点击查询
    // 验证结果中每条都有AI徽标或为空
  });
});
```

- [ ] **Step 2: 运行搜索筛选测试验证**

```bash
cd frontend && npx playwright test e2e/specs/product/data-list-search.spec.ts --project=chromium
```

- [ ] **Step 3: 修复并重新验证**

---

### Task 7: 高级功能测试 (data-list-advanced.spec.ts)

**Files:**
- Create: `frontend/e2e/specs/product/data-list-advanced.spec.ts`
- Depends on: Task 1 (DataListFunctionalPage)

**Interfaces:**
- Consumes: DataListFunctionalPage全部方法
- Produces: 11个高级功能测试场景

- [ ] **Step 1: 编写高级功能测试spec文件**

11个场景：编码重排序(2)、审批流程(3)、文档生成(2)、版本切换(2)、智能化标签(2)、预览(1)、插入待生成清单(1)

```typescript
import { test, expect } from '@playwright/test';
import { DataListFunctionalPage } from '../../pages/data-list-functional.page';

test.describe('产品清单-高级功能', () => {
  let funcPage: DataListFunctionalPage;

  test.beforeEach(async ({ page }) => {
    funcPage = new DataListFunctionalPage(page);
    await funcPage.gotoDataList();
  });

  // 编码重排序
  test('编码重排序-打开对话框', async ({ page }) => {
    // 勾选L3级别行
    // 点击编码重排序按钮
    // 验证对话框出现，表格有数据
  });

  test('编码重排序-执行重排序', async ({ page }) => {
    // 勾选L3级别行
    // 打开重排序对话框
    // 输入新编码前缀
    // 点击确认重排序
    // 验证成功消息
  });

  // 审批流程
  test('审批-提交审批', async ({ page }) => {
    // 找到一条可提交的行（"提交"按钮可见）
    // 点击提交按钮
    // 验证审批状态变化
  });

  test('审批-通过审批', async ({ page }) => {
    // 找到一条待审批的行
    // 点击"通过"按钮
    // 验证审批状态变为"已通过"
  });

  test('审批-驳回审批', async ({ page }) => {
    // 找到一条可驳回的行
    // 点击"驳回"按钮
    // 验证驳回对话框出现
    // 输入驳回原因
    // 确认驳回
  });

  // 文档生成
  test('生成Word文档-点击生成按钮', async ({ page }) => {
    // 在自定义清单tab中（或切换到自定义清单）
    // 点击生成文档按钮
    // 验证生成开始（进度条出现）
  });

  test('生成Excel文档-点击生成按钮', async ({ page }) => {
    // 点击生成文档按钮
    // 选择Excel格式
    // 验证生成开始
  });

  // 版本切换
  test('版本切换-打开切换版本对话框', async ({ page }) => {
    // 点击切换版本按钮
    // 验证对话框出现
  });

  test('版本切换-确认切换版本', async ({ page }) => {
    // 打开切换版本对话框
    // 选择一个版本
    // 确认切换
    // 验证版本徽章变化
  });

  // 智能化标签
  test('智能化标签-编辑表单勾选智能化', async ({ page }) => {
    // 打开编辑对话框
    // 勾选智能化checkbox
    // 保存
    // 验证行上出现AI徽标
  });

  test('智能化标签-AI徽标显示验证', async ({ page }) => {
    // 找到一条已标记智能化的行
    // 验证 .ai-badge 可见
  });

  // 预览
  test('预览-L3行点击预览按钮', async ({ page }) => {
    // 找到一条L3行
    // 点击"预览"按钮
    // 验证预览面板出现
  });

  // 插入待生成清单
  test('插入待生成清单-勾选后点击插入按钮', async ({ page }) => {
    // 勾选几条行
    // 点击"插入待生成清单"按钮
    // 验证操作提示（数据加载遮罩或成功消息）
  });
});
```

- [ ] **Step 2: 运行高级功能测试验证**

```bash
cd frontend && npx playwright test e2e/specs/product/data-list-advanced.spec.ts --project=chromium
```

- [ ] **Step 3: 修复并重新验证**

---

### Task 8: 全量回归测试 + VERSION.md更新

**Files:**
- Modify: `VERSION.md`
- Depends on: Task 2-7 (所有spec文件)

**Interfaces:**
- Consumes: 所有已完成的测试文件
- Produces: 全量回归验证结果 + VERSION变更说明

- [ ] **Step 1: 运行全量回归测试**

```bash
cd frontend && npx playwright test --project=chromium
```

包含原有57个基础测试 + 新增41个业务流程测试，共约98个测试全部通过。

- [ ] **Step 2: 修复剩余失败的测试**

根据回归结果逐个修复定位器和交互逻辑。

- [ ] **Step 3: 更新 VERSION.md**

在 V1.1.0 beta 产品清单变更说明中追加：

```
### 产品清单
- 新增 Playwright E2E 全量功能测试框架：数据清单12个、自定义清单3个、全景图2个共17个测试场景
- 新增 E2E 业务流程测试41个场景：CRUD闭环6个、右键菜单8个、批量操作6个、版本划分5个、搜索筛选5个、编码重排序2个、审批流程3个、文档生成2个、版本切换2个、智能化标签2个、预览1个、插入清单1个
```

- [ ] **Step 4: 最终验证**

再次运行全量回归测试确认所有约98个测试通过：
```bash
cd frontend && npx playwright test --project=chromium
```

前端构建验证：
```bash
cd frontend && npm run build
```
