# 添翼产品清单管理工具 — 实现计划（Phase 2：Vue3 前端开发）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 搭建 Vue3 + Element Plus 前端框架，实现登录、用户管理、版本管理和数据工作台核心页面。

**架构：** Vue3 + Vite + Element Plus + Vue Router + Pinia + Axios，前后端分离。

**Tech Stack:** Vue 3.4, Vite 5, Element Plus, Vue Router 4, Pinia, Axios, ECharts (统计视图)

---

### 项目文件结构

```
superpower-test/frontend/
├── package.json
├── vite.config.js
├── index.html
├── src/
│   ├── main.js
│   ├── App.vue
│   ├── router/
│   │   └── index.js
│   ├── store/
│   │   ├── auth.js
│   │   └── version.js
│   ├── api/
│   │   ├── auth.js
│   │   ├── user.js
│   │   ├── role.js
│   │   ├── version.js
│   │   └── data.js
│   ├── layout/
│   │   └── MainLayout.vue
│   ├── views/
│   │   ├── login/
│   │   │   └── LoginView.vue
│   │   ├── dashboard/
│   │   │   └── DataWorkbench.vue
│   │   ├── system/
│   │   │   ├── UserManage.vue
│   │   │   ├── RoleManage.vue
│   │   │   └── VersionManage.vue
│   │   └── components/
│   │       ├── VersionSelector.vue
│   │       ├── TreePanel.vue
│   │       ├── StatsTab.vue
│   │       └── DataListTab.vue
│   └── utils/
│       └── request.js
```

---

### Task 1: 前端项目初始化

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.js`
- Create: `frontend/index.html`
- Create: `frontend/src/main.js`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/utils/request.js`

项目初始化，配置 Vite 代理到后端 8080 端口，集成 Element Plus、Axios。

### Task 2: 路由 + 状态管理

**Files:**
- Create: `frontend/src/router/index.js`
- Create: `frontend/src/store/auth.js`

实现路由（登录页、数据工作台、用户管理、版本管理）和登录状态管理。

### Task 3: 登录页面 + API 层

**Files:**
- Create: `frontend/src/views/login/LoginView.vue`
- Create: `frontend/src/api/auth.js`
- Create: `frontend/src/api/user.js`
- Create: `frontend/src/api/role.js`
- Create: `frontend/src/api/version.js`
- Create: `frontend/src/api/data.js`

实现登录页面和所有后端 API 对接。

### Task 4: 主布局 + 导航

**Files:**
- Create: `frontend/src/layout/MainLayout.vue`

实现带侧边栏的主布局，根据用户角色显示不同菜单。

### Task 5: 数据工作台 - 版本选择 + 层级树

**Files:**
- Create: `frontend/src/views/dashboard/DataWorkbench.vue`
- Create: `frontend/src/components/VersionSelector.vue`
- Create: `frontend/src/components/TreePanel.vue`

实现版本选择弹窗、左侧 L1-L3 层级树。

### Task 6: 数据工作台 - 统计视图 Tab

**Files:**
- Create: `frontend/src/components/StatsTab.vue`
- Create: `frontend/src/store/version.js`

实现统计视图 Tab (产品/模块/功能数量统计卡片 + ECharts 图表)。

### Task 7: 数据工作台 - 数据清单 Tab

**Files:**
- Create: `frontend/src/components/DataListTab.vue`
- Create: `frontend/src/api/data.js` (追加)

实现数据清单 Tab (查询条件 + 可展开表格 + 编辑/删除/新建)。

### Task 8: 用户管理 + 角色管理 + 版本管理页面

**Files:**
- Create: `frontend/src/views/system/UserManage.vue`
- Create: `frontend/src/views/system/RoleManage.vue`
- Create: `frontend/src/views/system/VersionManage.vue`

实现管理员的管理页面。
