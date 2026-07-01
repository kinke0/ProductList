# Playwright E2E 全量功能测试设计

## 目标

为 ProductList（SuperPower）平台建立基于 Playwright + TypeScript 的端到端全量功能测试体系，覆盖除非常规操作之外的所有功能模块和操作流程，确保每次迭代后核心功能无回归缺陷。

## 技术选型

- **测试框架**：Playwright（~89.8K GitHub Stars，微软维护）
- **编写语言**：TypeScript
- **设计模式**：Page Object Model（POM）
- **运行环境**：本地开发环境（前端 localhost:5173 + 后端 localhost:8080）
- **浏览器**：Chromium 为主，后续扩展 Firefox

## 项目结构

```
frontend/
  e2e/
    fixtures/
      test-data.ts              # 测试账号、预设数据常量
      auth.setup.ts             # 登录状态 setup（保存 storageState）
    pages/
      login.page.ts             # 登录页 Page Object
      data-list.page.ts         # 产品清单页 Page Object
      custom-tab.page.ts        # 自定义清单页 Page Object
      panorama.page.ts          # 产品全景图页 Page Object
      requirement.page.ts       # 需求清单页 Page Object
      requirement-images.page.ts # 需求图片页 Page Object
      user-manage.page.ts       # 用户管理页 Page Object
      role-manage.page.ts       # 权限套餐管理页 Page Object
      version-manage.page.ts    # 版本管理页 Page Object
      image-gallery.page.ts     # 图床管理页 Page Object
      base-data.page.ts         # 基础数据维护页 Page Object
    specs/
      auth/
        login.spec.ts           # 登录/注册/登出/403跳转
      product/
        data-list.spec.ts       # 产品清单全量操作
        custom-tab.spec.ts      # 自定义清单全量操作
        panorama.spec.ts        # 全景图交互
      requirement/
        requirement.spec.ts     # 需求清单+审批流程
        requirement-images.spec.ts # 需求图片管理
      system/
        user-manage.spec.ts     # 用户管理
        role-manage.spec.ts     # 权限套餐管理
        base-data.spec.ts       # 基础数据维护（分类/解决方案/系统类型/功能状态）
        version-manage.spec.ts  # 版本管理
        image-gallery.spec.ts   # 图床管理
    playwright.config.ts        # Playwright 配置文件
```

## 核心测试场景清单

### 1. 登录模块 (login.spec.ts)

| # | 测试场景 | 验证要点 |
|---|---------|---------|
| 1 | 正确密码登录 | 登录成功跳转仪表盘，token 存储 |
| 2 | 错误密码登录 | 显示错误提示，不跳转 |
| 3 | 空用户名/密码提交 | 前端校验拦截 |
| 4 | 注册新用户 | 注册成功后自动登录 |
| 5 | 登出 | 清除 token，跳转登录页 |
| 6 | Token 过期 403 | 自动清除 token 并跳转登录页 |

### 2. 产品清单 (data-list.spec.ts)

| # | 测试场景 | 验证要点 |
|---|---------|---------|
| 1 | 加载清单数据 | 列表正确显示，层级结构正确 |
| 2 | 搜索/过滤 | 按关键词、业务分类、业务域、智能化过滤 |
| 3 | 重置搜索条件 | 清空所有过滤条件 |
| 4 | 展开全部/折叠全部 | 树结构展开/折叠正确 |
| 5 | 左侧导航树点击 | 选中节点后列表过滤到对应节点 |
| 6 | 添加新条目 | 表单填写保存，列表新增一行 |
| 7 | 编辑条目 | 修改各字段保存成功，列表数据更新 |
| 8 | 删除条目 | 确认删除后条目消失 |
| 9 | 备注标签显示 | 有备注的条目显示橙色标签，悬浮显示内容 |
| 10 | 智能化标签显示 | 标记智能化的条目显示 AI 标签 |
| 11 | 版本划分复选框 | 曜/远/驰三段式点击、最小集标记 |
| 12 | 批量操作 | 批量修改版本划分、业务域 |
| 13 | 右键菜单（复制/剪切/粘贴） | 复制到同级/子级，剪切移动 |
| 14 | 右键菜单（升级/降级/上移/下移） | 层级变更和排序交换 |
| 15 | 拖拽改变层级 | 拖拽升级/降级，指示线正确 |
| 16 | 文档生成（Word） | 生成进度正常，文件可下载 |
| 17 | 文档生成（Excel） | 生成进度正常，文件可下载 |
| 18 | 跨版本复制 | 从已发布版本复制到编辑中版本 |
| 19 | 预览 | 预览页面正确显示内容、图片 |

### 3. 自定义清单 (custom-tab.spec.ts)

| # | 测试场景 | 验证要点 |
|---|---------|---------|
| 1 | 创建自定义清单 | 新 Tab 创建成功 |
| 2 | 添加条目到清单 | 从主清单添加条目 |
| 3 | 移除清单条目 | 条目移除后清单更新 |
| 4 | 编辑清单条目 | 编辑表单保存 |
| 5 | 删除自定义清单 | 删除 Tab 及关联数据 |
| 6 | 生成文档（Word/Excel） | 自定义清单文档生成 |
| 7 | 搜索/过滤条件同步 | 搜索条件与主清单一致 |

### 4. 产品全景图 (panorama.spec.ts)

| # | 测试场景 | 验证要点 |
|---|---------|---------|
| 1 | 切换版本 | 版本切换后全景图数据更新 |
| 2 | CostProfitChart 交互 | 图表切换解决方案、展开/折叠 |

### 5. 需求清单 (requirement.spec.ts)

| # | 测试场景 | 验证要点 |
|---|---------|---------|
| 1 | 创建需求 | 填写表单创建成功 |
| 2 | 编辑需求 | 修改标题/描述/优先级等 |
| 3 | 审批流程-确认 | 状态从"提出"变为"已确认" |
| 4 | 审批流程-开发 | 状态变为"开发中" |
| 5 | 审批流程-就绪 | 状态变为"已就绪" |
| 6 | 审批流程-发布 | 选择版本号发布 |
| 7 | 审批流程-驳回 | 输入驳回原因，状态变为"已驳回" |
| 8 | 撤销需求 | 状态变为"已撤销" |
| 9 | 删除需求 | 确认删除后消失 |
| 10 | 查看审批日志 | 日志弹窗正确显示流程记录 |

### 6. 需求图片 (requirement-images.spec.ts)

| # | 测试场景 | 验证要点 |
|---|---------|---------|
| 1 | 上传图片 | 图片上传成功显示 |
| 2 | 删除图片 | 图片删除成功 |

### 7. 用户管理 (user-manage.spec.ts)

| # | 测试场景 | 验证要点 |
|---|---------|---------|
| 1 | 添加用户 | 新用户创建成功 |
| 2 | 编辑用户 | 修改用户信息 |
| 3 | 删除用户 | 用户删除成功 |
| 4 | 查看在线状态 | 在线状态列正确显示 |
| 5 | 查看操作日志 | 日志弹窗正确展示 |

### 8. 权限套餐管理 (role-manage.spec.ts)

| # | 测试场景 | 验证要点 |
|---|---------|---------|
| 1 | 创建角色 | 新角色创建成功 |
| 2 | 编辑角色权限 | 修改权限勾选 |
| 3 | 删除角色 | 角色删除成功 |

### 9. 基础数据维护 (base-data.spec.ts)

| # | 测试场景 | 验证要点 |
|---|---------|---------|
| 1 | 产品分类维护 CRUD | 增删改查分类/域 |
| 2 | 解决方案维护 CRUD | 增删改查解决方案 |
| 3 | 系统类型维护 CRUD | 增删改查系统类型 |
| 4 | 功能状态维护 CRUD | 增删改查功能状态 |

### 10. 版本管理 (version-manage.spec.ts)

| # | 测试场景 | 验证要点 |
|---|---------|---------|
| 1 | 创建版本 | 多步骤异步流程，进度显示 |
| 2 | 发布版本 | 版本状态变为"已发布" |
| 3 | 删除草稿版本 | 分步清理进度 |
| 4 | 版本列表显示 | 版本号、状态、操作按钮正确 |

### 11. 图床管理 (image-gallery.spec.ts)

| # | 测试场景 | 验证要点 |
|---|---------|---------|
| 1 | 目录导航 | 左侧树导航到 L3 产品 |
| 2 | 上传图片 | 图片上传成功 |
| 3 | 替换图片 | 新图片替换旧图片 |
| 4 | 重命名图片 | 名称修改成功 |
| 5 | 删除图片 | 图片删除成功 |
| 6 | 引用查询 | 显示引用列表 |
| 7 | 复制图片URL | URL 复制功能 |
| 8 | 网格/列表视图切换 | 两种视图模式切换 |

## 技术要点

### 认证处理

- 使用 Playwright `project` 依赖机制：先运行 `auth.setup.ts` 登录并保存 `storageState`
- 其他所有测试项目依赖此 setup，自动复用登录状态
- 避免每个测试重新登录，大幅提升执行效率

### Page Object Model

每个 Page Object 封装：
- 页面导航路径
- Element Plus 组件定位策略（使用 data-testid 或角色选择器）
- 业务操作方法（如 `addEntry()`, `deleteEntry()`, `searchByKeyword()`）
- 数据获取方法（如 `getEntryList()`, `getTotalCount()`）

### Element Plus 组件交互策略

- **下拉选择器**：先 click 触发弹出，再 click 选项（使用 `getByRole('option')`）
- **对话框**：使用 `getByRole('dialog')` 定位，配合按钮文字查找
- **表格**：使用 `getByRole('row')` + `getByRole('cell')` 定位
- **消息提示**：使用 `getByText()` 验证成功/错误提示
- **虚拟滚动列表**：Playwright 的 auto-scroll 机制处理

### 数据管理

- 测试前通过 API 创建必要的测试数据（版本、分类、域等）
- 测试后清理新增数据，保持测试环境干净
- 使用 `test.beforeAll` / `test.afterAll` 管理数据生命周期

### 等待策略

- 优先依赖 Playwright 内置 auto-waiting
- 长时间操作（如文档生成）使用 `waitForResponse` 监听 API 返回
- 加载遮罩场景使用 `waitForSelector` 等遮罩消失

### 截图与调试

- 失败时自动截图 + Trace 录制
- 关键步骤可选截图留存（通过 `testInfo.attach`）

## Playwright 配置

```typescript
// playwright.config.ts 核心配置要点
{
  baseURL: 'http://localhost:5173',
  timeout: 30000,
  expect: { timeout: 10000 },
  retries: 1,
  workers: 2,
  reporter: [['html', { open: 'never' }], ['list']],
  use: {
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'on-first-retry',
  },
  projects: [
    { name: 'setup', testMatch: /auth\.setup\.ts/ },
    {
      name: 'chromium',
      dependencies: ['setup'],
      use: { storageState: 'e2e/.auth/user.json' },
      testDir: './specs',
    },
  ],
  webServer: [
    { command: 'cd frontend && npm run dev', port: 5173, reuseExistingServer: true },
  ],
}
```

## 不覆盖范围

- **非常规操作模块**（SQL脚本执行、迁移图片、修复层级等）：按用户要求排除
- **性能测试**：不在 E2E 测试范围内
- **后端单元测试**：不在 E2E 测试范围内

## 后续演进方向

1. 增加 Firefox/WebKit 浏览器项目
2. 增加 API 测试层（Playwright APIRequest）验证边界场景
3. 集成到 CI 流水线（GitHub Actions）
4. 增加可视化测试报告（HTML Reporter）
