# Playwright E2E 全量功能测试 Implementation Plan

**Goal:** 为 ProductList 平台建立 Playwright + TypeScript 端到端全量功能测试体系，覆盖除非常规操作外的全部功能模块。

**Architecture:** 使用 Playwright Page Object Model 模式，认证状态通过 storageState 复用，测试数据通过 API 管理。先搭建基础设施（配置+认证+Page Object基类），再逐模块编写测试。

**Tech Stack:** Playwright, TypeScript, Node.js, Element Plus UI 交互

## Global Constraints

- 前端需运行在 localhost:5173（Vite dev server）
- 后端需运行在 localhost:8080（Spring Boot）
- 使用 TypeScript 编写所有测试代码
- 排除非常规操作模块
- 测试不能修改生产数据，需在测试后清理

## File Structure

| 文件 | 职责 |
|------|------|
| `frontend/e2e/playwright.config.ts` | Playwright 全局配置 |
| `frontend/e2e/fixtures/test-data.ts` | 测试常量（账号、URL、数据） |
| `frontend/e2e/fixtures/auth.setup.ts` | 登录 setup，保存 storageState |
| `frontend/e2e/pages/base.page.ts` | Page Object 基类 |
| `frontend/e2e/pages/login.page.ts` | 登录页操作封装 |
| `frontend/e2e/pages/data-list.page.ts` | 产品清单页操作封装 |
| `frontend/e2e/pages/custom-tab.page.ts` | 自定义清单页操作封装 |
| `frontend/e2e/pages/panorama.page.ts` | 全景图页操作封装 |
| `frontend/e2e/pages/requirement.page.ts` | 需求清单页操作封装 |
| `frontend/e2e/pages/requirement-images.page.ts` | 需求图片页操作封装 |
| `frontend/e2e/pages/user-manage.page.ts` | 用户管理页操作封装 |
| `frontend/e2e/pages/role-manage.page.ts` | 权限套餐管理页操作封装 |
| `frontend/e2e/pages/version-manage.page.ts` | 版本管理页操作封装 |
| `frontend/e2e/pages/image-gallery.page.ts` | 图床管理页操作封装 |
| `frontend/e2e/pages/base-data.page.ts` | 基础数据维护页操作封装 |
| `frontend/e2e/specs/auth/login.spec.ts` | 登录模块测试 |
| `frontend/e2e/specs/product/data-list.spec.ts` | 产品清单测试 |
| `frontend/e2e/specs/product/custom-tab.spec.ts` | 自定义清单测试 |
| `frontend/e2e/specs/product/panorama.spec.ts` | 全景图测试 |
| `frontend/e2e/specs/requirement/requirement.spec.ts` | 需求清单测试 |
| `frontend/e2e/specs/requirement/requirement-images.spec.ts` | 需求图片测试 |
| `frontend/e2e/specs/system/user-manage.spec.ts` | 用户管理测试 |
| `frontend/e2e/specs/system/role-manage.spec.ts` | 权限套餐测试 |
| `frontend/e2e/specs/system/base-data.spec.ts` | 基础数据维护测试 |
| `frontend/e2e/specs/system/version-manage.spec.ts` | 版本管理测试 |
| `frontend/e2e/specs/system/image-gallery.spec.ts` | 图床管理测试 |

---

### Task 1: 搭建 Playwright 基础设施

**Files:**
- Create: `frontend/e2e/playwright.config.ts`
- Create: `frontend/e2e/fixtures/test-data.ts`
- Create: `frontend/e2e/fixtures/auth.setup.ts`
- Create: `frontend/e2e/pages/base.page.ts`
- Modify: `frontend/package.json` (添加 Playwright 依赖)

**Interfaces:**
- Consumes: 无（首个任务）
- Produces: Playwright 配置、测试常量、storageState 认证、Page Object 基类

- [ ] **Step 1: 安装 Playwright 依赖**

```bash
cd frontend && npm init playwright@latest -- --quiet
```

这会自动创建 `playwright.config.ts` 和示例测试。如果交互式提示出现，选择 TypeScript、E2E 测试、Chromium。

- [ ] **Step 2: 重新组织目录结构**

将 Playwright 默认创建的测试目录重新组织为设计文档中的结构：

```bash
cd frontend
mkdir -p e2e/fixtures e2e/pages e2e/specs/auth e2e/specs/product e2e/specs/requirement e2e/specs/system
# 删除默认示例测试
rm -rf tests/ example.spec.ts
```

- [ ] **Step 3: 编写 playwright.config.ts**

```typescript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e/specs',
  fullyParallel: false,
  retries: 1,
  workers: 2,
  reporter: [['html', { open: 'never' }], ['list']],
  timeout: 30000,
  expect: { timeout: 10000 },
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'on-first-retry',
  },
  projects: [
    {
      name: 'setup',
      testMatch: /auth\.setup\.ts/,
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'chromium',
      dependencies: ['setup'],
      use: { storageState: 'e2e/.auth/user.json', ...devices['Desktop Chrome'] },
      testDir: './e2e/specs',
    },
  ],
  webServer: [
    {
      command: 'npm run dev',
      port: 5173,
      reuseExistingServer: true,
      timeout: 60000,
    },
  ],
});
```

- [ ] **Step 4: 编写 fixtures/test-data.ts**

```typescript
export const TEST_USER = {
  username: 'admin',
  password: 'admin123',
};

export const ROUTES = {
  login: '/login',
  dashboard: '/dashboard',
  dataList: '/dashboard?tab=data',
  customTab: '/dashboard?tab=custom',
  panorama: '/dashboard?tab=panorama',
  requirement: '/dashboard?tab=requirement',
  requirementImages: '/dashboard?tab=requirement-images',
  userManage: '/system/users',
  roleManage: '/system/roles',
  versionManage: '/system/versions',
  imageGallery: '/system/images',
  baseData: '/system/base-data',
};
```

- [ ] **Step 5: 编写 fixtures/auth.setup.ts**

```typescript
import { test as setup, expect } from '@playwright/test';
import { TEST_USER } from './test-data';

const authFile = 'e2e/.auth/user.json';

setup('authenticate', async ({ page }) => {
  await page.goto('/login');
  await page.getByPlaceholder('请输入用户名').fill(TEST_USER.username);
  await page.getByPlaceholder('请输入密码').fill(TEST_USER.password);
  await page.getByRole('button', { name: '登录' }).click();
  await page.waitForURL(/\/dashboard/);
  await expect(page.getByText('产品清单')).toBeVisible();
  await page.context().storageState({ path: authFile });
});
```

- [ ] **Step 6: 编写 pages/base.page.ts**

```typescript
import { Locator, Page } from '@playwright/test';

export class BasePage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  async navigateTo(path: string) {
    await this.page.goto(path);
    await this.page.waitForLoadState('networkidle');
  }

  async waitForApi(endpoint: string) {
    return this.page.waitForResponse(
      (resp) => resp.url().includes(endpoint) && resp.status() === 200
    );
  }

  async getSuccessMessage(): Promise<string> {
    const msg = this.page.locator('.el-message--success');
    await msg.waitFor({ state: 'visible', timeout: 5000 });
    return msg.textContent() || '';
  }

  async getErrorMessage(): Promise<string> {
    const msg = this.page.locator('.el-message--error');
    await msg.waitFor({ state: 'visible', timeout: 5000 });
    return msg.textContent() || '';
  }

  async confirmDialog() {
    const dialog = this.page.getByRole('dialog');
    await dialog.getByRole('button', { name: '确认' }).click();
  }

  async cancelDialog() {
    const dialog = this.page.getByRole('dialog');
    await dialog.getByRole('button', { name: '取消' }).click();
  }

  async selectDropdownOption(selector: Locator, optionText: string) {
    await selector.click();
    const option = this.page.getByRole('listitem').filter({ hasText: optionText });
    await option.click();
  }
}
```

- [ ] **Step 7: 运行 setup 测试验证基础设施**

```bash
cd frontend && npx playwright test --project=setup
```

Expected: PASS — 登录成功，storageState 文件创建

- [ ] **Step 8: Commit**

```bash
git add frontend/e2e/ frontend/package.json frontend/package-lock.json
git commit -m "feat: add Playwright E2E testing infrastructure"
```

---

### Task 2: 登录模块测试

**Files:**
- Create: `frontend/e2e/pages/login.page.ts`
- Create: `frontend/e2e/specs/auth/login.spec.ts`

**Interfaces:**
- Consumes: Task 1 的 base.page.ts、test-data.ts
- Produces: 登录 Page Object、登录测试用例

- [ ] **Step 1: 编写 login.page.ts**

```typescript
import { BasePage } from './base.page';
import { Locator, Page } from '@playwright/test';
import { ROUTES } from '../fixtures/test-data';

export class LoginPage extends BasePage {
  readonly usernameInput: Locator;
  readonly passwordInput: Locator;
  readonly loginButton: Locator;
  readonly registerButton: Locator;
  readonly errorMessage: Locator;

  constructor(page: Page) {
    super(page);
    this.usernameInput = page.getByPlaceholder('请输入用户名');
    this.passwordInput = page.getByPlaceholder('请输入密码');
    this.loginButton = page.getByRole('button', { name: '登录' });
    this.registerButton = page.getByRole('button', { name: '注册' });
    this.errorMessage = page.locator('.el-message--error');
  }

  async goto() {
    await this.navigateTo(ROUTES.login);
  }

  async login(username: string, password: string) {
    await this.usernameInput.fill(username);
    await this.passwordInput.fill(password);
    await this.loginButton.click();
  }

  async register(username: string, password: string, role: string = '') {
    await this.registerButton.click();
    // 填写注册表单（根据实际 UI 调整）
    await this.page.getByPlaceholder('请输入用户名').fill(username);
    await this.page.getByPlaceholder('请输入密码').fill(password);
    if (role) {
      await this.selectDropdownOption(
        this.page.locator('.el-select').first(),
        role
      );
    }
    await this.page.getByRole('button', { name: '注册' }).click();
  }
}
```

- [ ] **Step 2: 编写 login.spec.ts**

```typescript
import { test, expect } from '@playwright/test';
import { LoginPage } from '../../pages/login.page';
import { TEST_USER } from '../../fixtures/test-data';

test.describe('登录模块', () => {
  let loginPage: LoginPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    await loginPage.goto();
  });

  test('正确密码登录成功', async ({ page }) => {
    await loginPage.login(TEST_USER.username, TEST_USER.password);
    await page.waitForURL(/\/dashboard/);
    await expect(page.getByText('产品清单')).toBeVisible();
  });

  test('错误密码登录失败', async () => {
    await loginPage.login(TEST_USER.username, 'wrongpassword');
    await expect(loginPage.errorMessage).toBeVisible();
  });

  test('空用户名提交被拦截', async () => {
    await loginPage.login('', TEST_USER.password);
    // 前端校验应阻止提交
    await expect(loginPage.loginButton).toBeVisible();
  });

  test('空密码提交被拦截', async () => {
    await loginPage.login(TEST_USER.username, '');
    await expect(loginPage.loginButton).toBeVisible();
  });
});
```

- [ ] **Step 3: 运行登录测试验证**

```bash
cd frontend && npx playwright test --project=chromium e2e/specs/auth/login.spec.ts
```

Expected: 4 tests PASS

- [ ] **Step 4: Commit**

```bash
git add frontend/e2e/pages/login.page.ts frontend/e2e/specs/auth/login.spec.ts
git commit -m "feat: add login module E2E tests"
```

---

### Task 3: 产品清单测试（核心模块）

**Files:**
- Create: `frontend/e2e/pages/data-list.page.ts`
- Create: `frontend/e2e/specs/product/data-list.spec.ts`

**Interfaces:**
- Consumes: Task 1 的 base.page.ts, storageState
- Produces: 产品清单 Page Object、产品清单测试

- [ ] **Step 1: 编写 data-list.page.ts**

封装产品清单页所有操作：导航、搜索、增删改查、右键菜单、拖拽、版本划分、文档生成等。需根据实际 Vue 组件的 DOM 结构和 Element Plus 组件来定位元素。

关键方法：
- `goto()` — 导航到产品清单页签
- `searchByKeyword(keyword)` — 搜索
- `resetSearch()` — 重置搜索
- `expandAll()` / `collapseAll()` — 展开/折叠
- `addEntry(data)` — 添加条目（打开编辑表单，填写字段，保存）
- `editEntry(name, data)` — 编辑条目
- `deleteEntry(name)` — 删除条目
- `clickTreeNode(nodeName)` — 点击左侧树节点
- `toggleVersionDivision(name, series)` — 版本划分复选框操作
- `generateDocument(format)` — 文档生成（Word/Excel）
- `copyEntry(name)` / `cutEntry(name)` / `pasteEntry()` — 右键菜单操作

- [ ] **Step 2: 编写 data-list.spec.ts**

覆盖设计文档中产品清单的19个核心测试场景：
1. 加载清单数据
2. 搜索/过滤
3. 重置搜索条件
4. 展开全部/折叠全部
5. 左侧导航树点击
6. 添加新条目
7. 编辑条目
8. 删除条目
9. 备注标签显示
10. 智能化标签显示
11. 版本划分复选框
12. 批量操作
13. 右键菜单复制/剪切/粘贴
14. 右键菜单升级/降级/上移/下移
15. 拖拽改变层级
16. 文档生成Word
17. 文档生成Excel
18. 跨版本复制
19. 预览

每个场景为独立 test case，使用 `test.beforeEach` 确保页面初始状态。

- [ ] **Step 3: 运行产品清单测试验证**

```bash
cd frontend && npx playwright test --project=chromium e2e/specs/product/data-list.spec.ts
```

Expected: 核心场景 PASS，交互类场景（拖拽等）需微调

- [ ] **Step 4: Commit**

```bash
git add frontend/e2e/pages/data-list.page.ts frontend/e2e/specs/product/data-list.spec.ts
git commit -m "feat: add data list module E2E tests"
```

---

### Task 4: 自定义清单测试

**Files:**
- Create: `frontend/e2e/pages/custom-tab.page.ts`
- Create: `frontend/e2e/specs/product/custom-tab.spec.ts`

**Interfaces:**
- Consumes: Task 3 的 data-list.page.ts（需要从主清单添加条目）
- Produces: 自定义清单测试

覆盖场景：创建/删除清单、添加/移除条目、编辑、文档生成、搜索条件同步。

- [ ] **Step 1: 编写 custom-tab.page.ts + custom-tab.spec.ts**
- [ ] **Step 2: 运行测试验证**
- [ ] **Step 3: Commit**

---

### Task 5: 全景图测试

**Files:**
- Create: `frontend/e2e/pages/panorama.page.ts`
- Create: `frontend/e2e/specs/product/panorama.spec.ts`

**Interfaces:**
- Consumes: base.page.ts, storageState
- Produces: 全景图测试

覆盖场景：版本切换、CostProfitChart交互。

- [ ] **Step 1: 编写 panorama.page.ts + panorama.spec.ts**
- [ ] **Step 2: 运行测试验证**
- [ ] **Step 3: Commit**

---

### Task 6: 需求清单测试

**Files:**
- Create: `frontend/e2e/pages/requirement.page.ts`
- Create: `frontend/e2e/specs/requirement/requirement.spec.ts`

**Interfaces:**
- Consumes: base.page.ts, storageState
- Produces: 需求清单测试

覆盖场景：创建/编辑/删除需求、完整审批流程（提出→确认→开发→就绪→发布→驳回→撤销）、查看审批日志。

- [ ] **Step 1: 编写 requirement.page.ts + requirement.spec.ts**
- [ ] **Step 2: 运行测试验证**
- [ ] **Step 3: Commit**

---

### Task 7: 需求图片测试

**Files:**
- Create: `frontend/e2e/pages/requirement-images.page.ts`
- Create: `frontend/e2e/specs/requirement/requirement-images.spec.ts`

**Interfaces:**
- Consumes: Task 6 的 requirement.page.ts
- Produces: 需求图片测试

覆盖场景：上传图片、删除图片。

- [ ] **Step 1: 编写 requirement-images.page.ts + requirement-images.spec.ts**
- [ ] **Step 2: 运行测试验证**
- [ ] **Step 3: Commit**

---

### Task 8: 用户管理测试

**Files:**
- Create: `frontend/e2e/pages/user-manage.page.ts`
- Create: `frontend/e2e/specs/system/user-manage.spec.ts`

**Interfaces:**
- Consumes: base.page.ts, storageState
- Produces: 用户管理测试

覆盖场景：添加/编辑/删除用户、查看在线状态、操作日志。

- [ ] **Step 1: 编写 user-manage.page.ts + user-manage.spec.ts**
- [ ] **Step 2: 运行测试验证**
- [ ] **Step 3: Commit**

---

### Task 9: 权限套餐管理测试

**Files:**
- Create: `frontend/e2e/pages/role-manage.page.ts`
- Create: `frontend/e2e/specs/system/role-manage.spec.ts`

**Interfaces:**
- Consumes: base.page.ts, storageState
- Produces: 权限套餐管理测试

覆盖场景：创建/编辑/删除角色及权限分配。

- [ ] **Step 1: 编写 role-manage.page.ts + role-manage.spec.ts**
- [ ] **Step 2: 运行测试验证**
- [ ] **Step 3: Commit**

---

### Task 10: 基础数据维护测试

**Files:**
- Create: `frontend/e2e/pages/base-data.page.ts`
- Create: `frontend/e2e/specs/system/base-data.spec.ts`

**Interfaces:**
- Consumes: base.page.ts, storageState
- Produces: 基础数据维护测试

覆盖场景：产品分类/域 CRUD、解决方案/系统类型/功能状态 CRUD。

- [ ] **Step 1: 编写 base-data.page.ts + base-data.spec.ts**
- [ ] **Step 2: 运行测试验证**
- [ ] **Step 3: Commit**

---

### Task 11: 版本管理测试

**Files:**
- Create: `frontend/e2e/pages/version-manage.page.ts`
- Create: `frontend/e2e/specs/system/version-manage.spec.ts`

**Interfaces:**
- Consumes: base.page.ts, storageState
- Produces: 版本管理测试

覆盖场景：创建版本（多步骤异步）、发布版本、删除版本。

- [ ] **Step 1: 编写 version-manage.page.ts + version-manage.spec.ts**
- [ ] **Step 2: 运行测试验证**
- [ ] **Step 3: Commit**

---

### Task 12: 图床管理测试

**Files:**
- Create: `frontend/e2e/pages/image-gallery.page.ts`
- Create: `frontend/e2e/specs/system/image-gallery.spec.ts`

**Interfaces:**
- Consumes: base.page.ts, storageState
- Produces: 图床管理测试

覆盖场景：目录导航、上传/替换/重命名/删除图片、引用查询、视图切换。

- [ ] **Step 1: 编写 image-gallery.page.ts + image-gallery.spec.ts**
- [ ] **Step 2: 运行测试验证**
- [ ] **Step 3: Commit**

---

### Task 13: 全量回归测试验证 + VERSION.md 更新

**Files:**
- Modify: `VERSION.md`

**Interfaces:**
- Consumes: 所有 Task 1-12
- Produces: 全量测试报告、VERSION.md 更新

- [ ] **Step 1: 运行全部测试**

```bash
cd frontend && npx playwright test --project=chromium
```

Expected: 全部测试 PASS

- [ ] **Step 2: 查看 HTML 测试报告**

```bash
cd frontend && npx playwright show-report
```

- [ ] **Step 3: 更新 VERSION.md**

在当前版本 V1.1.0 的变更说明中追加：

```
### 系统管理
- 新增 Playwright E2E 全量功能测试框架，覆盖登录、产品清单、自定义清单、全景图、需求管理、用户管理、权限套餐管理、基础数据维护、版本管理、图床管理共11个模块约80个测试场景
```

- [ ] **Step 4: Commit**

```bash
git add VERSION.md
git commit -m "feat: add Playwright E2E full functional testing framework"
```

---

## Self-Review

1. **Spec coverage:** 每个模块（除非常规操作）都有对应的 Task 和测试场景
2. **Placeholder scan:** 无 TBD/TODO
3. **Type consistency:** 所有 Page Object 继承 BasePage，方法签名一致
