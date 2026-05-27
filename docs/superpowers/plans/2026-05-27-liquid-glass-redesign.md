# Liquid Glass UI 风格重构 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将"添翼产品清单管理工具"从 MUI 蓝色亮色主题全面重构为 Liquid Glass 暗色风格

**Architecture:** 通过三层 CSS 文件（变量定义 → 动画关键帧 → Element Plus 全局主题覆盖）实现暗色液态玻璃效果。保留 Element Plus 组件 API 完全兼容，仅视觉层变更。背景使用 CSS radial-gradient 光斑 + border-radius 变形动画，玻璃层使用 backdrop-filter blur 叠加。

**Tech Stack:** Vue 3, Element Plus 2.5+, Vite, 纯 CSS (无额外库依赖)

---

## 文件结构

| 操作 | 文件 | 职责 |
|------|------|------|
| **新建** | `frontend/src/styles/liquid-glass-vars.css` | CSS 自定义属性（Design Token） |
| **新建** | `frontend/src/styles/liquid-glass-anim.css` | 动画 @keyframes |
| **新建** | `frontend/src/styles/liquid-glass-theme.css` | Element Plus 全局覆盖 + 组件主题 |
| **修改** | `frontend/index.html` | 字体引用替换 |
| **修改** | `frontend/src/main.js` | CSS 引入路径 |
| **修改** | `frontend/src/App.vue` | 全局 body 样式 |
| **修改** | `frontend/src/layout/MainLayout.vue` | 浮动玻璃布局 + 背景光斑 |
| **修改** | `frontend/src/views/login/LoginView.vue` | 登录页玻璃效果 |
| **修改** | `frontend/src/views/dashboard/DataWorkbench.vue` | 工作台玻璃面板 |
| **修改** | `frontend/src/components/StatsTab.vue` | 统计卡片玻璃化 |
| **修改** | `frontend/src/components/TreePanel.vue` | 树形控件玻璃样式 |
| **修改** | `frontend/src/components/DataListTab.vue` | 表格玻璃样式 |
| **删除** | `frontend/src/styles/mui-theme.css` | 旧 MUI 主题 |

---

### Task 1: 创建 CSS 变量文件

**Files:**
- Create: `frontend/src/styles/liquid-glass-vars.css`

- [ ] **Step 1: 写入 liquid-glass-vars.css**

```css
/* Liquid Glass Design Tokens */
:root {
  /* === Deep Space Background === */
  --glass-bg-deep: #09090B;
  --glass-bg-space: #0F0B1A;
  --glass-bg-base: #14101F;
  --glass-bg-elevated: #1E1830;

  /* === Glass Surfaces === */
  --glass-surface-sm: rgba(255, 255, 255, 0.03);
  --glass-surface-md: rgba(255, 255, 255, 0.05);
  --glass-surface-lg: rgba(255, 255, 255, 0.08);
  --glass-border-default: rgba(255, 255, 255, 0.08);
  --glass-border-accent: rgba(168, 139, 250, 0.15);
  --glass-border-strong: rgba(255, 255, 255, 0.12);

  /* === Blur Levels === */
  --glass-blur-sm: 8px;
  --glass-blur-md: 16px;
  --glass-blur-lg: 24px;
  --glass-blur-xl: 32px;

  /* === Iridescent Accents === */
  --color-primary: #A78BFA;
  --color-primary-soft: rgba(168, 139, 250, 0.15);
  --color-cyan: #22D3EE;
  --color-cyan-soft: rgba(34, 211, 238, 0.12);
  --color-pink: #F472B6;
  --color-pink-soft: rgba(244, 114, 182, 0.12);

  /* === Semantic Colors === */
  --color-success: #4ADE80;
  --color-success-soft: rgba(74, 222, 128, 0.12);
  --color-warning: #FBBF24;
  --color-warning-soft: rgba(251, 191, 36, 0.12);
  --color-danger: #F87171;
  --color-danger-soft: rgba(248, 113, 113, 0.12);
  --color-info: #22D3EE;

  /* === Text === */
  --text-primary: #F1F5F9;
  --text-secondary: #94A3B8;
  --text-muted: #64748B;
  --text-disabled: #475569;

  /* === Shadows === */
  --shadow-glass-sm: 0 2px 8px rgba(0, 0, 0, 0.3);
  --shadow-glass-md: 0 4px 16px rgba(0, 0, 0, 0.4);
  --shadow-glass-lg: 0 8px 32px rgba(0, 0, 0, 0.5);
  --shadow-glow-violet: 0 0 20px rgba(168, 139, 250, 0.15);
  --shadow-glow-cyan: 0 0 16px rgba(34, 211, 238, 0.12);

  /* === Radius === */
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 20px;

  /* === Timing Curves === */
  --ease-fluid: cubic-bezier(0.4, 0, 0.2, 1);
  --ease-glass: cubic-bezier(0.34, 1.56, 0.64, 1);
  --ease-iris: cubic-bezier(0.65, 0, 0.35, 1);

  /* === Typography === */
  --font-heading: 'Space Grotesk', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  --font-body: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;

  /* === Transitions === */
  --transition-fast: 150ms var(--ease-fluid);
  --transition-normal: 250ms var(--ease-fluid);
  --transition-slow: 500ms var(--ease-glass);
}
```

### Task 2: 创建动画关键帧文件

**Files:**
- Create: `frontend/src/styles/liquid-glass-anim.css`

- [ ] **Step 1: 写入 liquid-glass-anim.css**

```css
/* Liquid Glass Animation Keyframes */

/* Light sweep across glass surfaces */
@keyframes light-sweep {
  0% { left: -100%; }
  100% { left: 200%; }
}

/* Iridescent gradient shift */
@keyframes iris-shift {
  0% { background-position: 0% 50%; }
  100% { background-position: 300% 50%; }
}

/* Morphing blob background orb */
@keyframes morph-blob-1 {
  0% { border-radius: 60% 40% 50% 50% / 50% 60% 40% 50%; }
  25% { border-radius: 40% 60% 60% 40% / 60% 40% 60% 40%; }
  50% { border-radius: 50% 50% 40% 60% / 40% 50% 50% 60%; }
  75% { border-radius: 60% 40% 60% 40% / 40% 60% 40% 60%; }
  100% { border-radius: 60% 40% 50% 50% / 50% 60% 40% 50%; }
}

@keyframes morph-blob-2 {
  0% { border-radius: 40% 60% 65% 35% / 55% 45% 55% 45%; }
  25% { border-radius: 55% 45% 35% 65% / 40% 60% 40% 60%; }
  50% { border-radius: 35% 65% 50% 50% / 60% 40% 60% 40%; }
  75% { border-radius: 60% 40% 45% 55% / 35% 65% 35% 65%; }
  100% { border-radius: 40% 60% 65% 35% / 55% 45% 55% 45%; }
}

/* Glow pulse for primary elements */
@keyframes glow-pulse {
  0%, 100% { box-shadow: 0 0 10px rgba(168, 139, 250, 0.2); }
  50% { box-shadow: 0 0 25px rgba(168, 139, 250, 0.4); }
}

/* Reduced motion fallback */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
```

### Task 3: 创建核心主题文件

**Files:**
- Create: `frontend/src/styles/liquid-glass-theme.css`

- [ ] **Step 1: 写入 liquid-glass-theme.css**

```css
/* Liquid Glass Theme - Element Plus Global Override */
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=Inter:wght@300;400;500;600&display=swap');
@import './liquid-glass-vars.css';
@import './liquid-glass-anim.css';

/* === Element Plus CSS Variable Override === */
:root {
  --el-color-primary: var(--color-primary);
  --el-color-primary-light-3: rgba(168, 139, 250, 0.5);
  --el-color-primary-light-5: rgba(168, 139, 250, 0.3);
  --el-color-primary-light-7: rgba(168, 139, 250, 0.15);
  --el-color-primary-light-8: rgba(168, 139, 250, 0.08);
  --el-color-primary-light-9: rgba(168, 139, 250, 0.04);
  --el-color-primary-dark-2: #8B5CF6;

  --el-color-success: var(--color-success);
  --el-color-success-light-3: rgba(74, 222, 128, 0.5);
  --el-color-success-light-5: rgba(74, 222, 128, 0.3);

  --el-color-warning: var(--color-warning);
  --el-color-warning-light-3: rgba(251, 191, 36, 0.5);
  --el-color-warning-light-5: rgba(251, 191, 36, 0.3);

  --el-color-danger: var(--color-danger);
  --el-color-danger-light-3: rgba(248, 113, 113, 0.5);
  --el-color-danger-light-5: rgba(248, 113, 113, 0.3);

  --el-color-info: var(--color-info);
  --el-color-info-light-3: rgba(34, 211, 238, 0.5);

  --el-border-radius-base: var(--radius-md);
  --el-border-radius-small: var(--radius-sm);
  --el-border-radius-round: var(--radius-xl);

  --el-font-family: var(--font-body);
  --el-font-size-base: 14px;

  --el-bg-color: var(--glass-bg-base);
  --el-bg-color-overlay: var(--glass-bg-elevated);
  --el-bg-color-page: var(--glass-bg-deep);
  --el-text-color-primary: var(--text-primary);
  --el-text-color-regular: var(--text-secondary);
  --el-text-color-secondary: var(--text-muted);
  --el-text-color-placeholder: var(--text-disabled);
  --el-text-color-disabled: var(--text-disabled);

  --el-border-color: var(--glass-border-default);
  --el-border-color-light: rgba(255, 255, 255, 0.04);
  --el-border-color-lighter: rgba(255, 255, 255, 0.03);
  --el-border-color-dark: var(--glass-border-strong);

  --el-fill-color: var(--glass-surface-md);
  --el-fill-color-light: var(--glass-surface-sm);
  --el-fill-color-blank: var(--glass-bg-deep);

  --el-mask-color: rgba(0, 0, 0, 0.6);
  --el-mask-color-extra-light: rgba(0, 0, 0, 0.3);

  --el-box-shadow: var(--shadow-glass-sm);
  --el-box-shadow-light: var(--shadow-glass-sm);
  --el-box-shadow-dark: var(--shadow-glass-md);
}

/* === Global Body === */
body {
  margin: 0;
  padding: 0;
  font-family: var(--font-body);
  background: var(--glass-bg-deep);
  color: var(--text-primary);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* === Glass Scrollbar === */
::-webkit-scrollbar { width: 5px; height: 5px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb {
  background: rgba(168, 139, 250, 0.2);
  border-radius: 10px;
}
::-webkit-scrollbar-thumb:hover {
  background: rgba(168, 139, 250, 0.35);
}

/* === Glass Card === */
.el-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--glass-border-default);
  background: var(--glass-surface-md);
  backdrop-filter: blur(var(--glass-blur-md));
  -webkit-backdrop-filter: blur(var(--glass-blur-md));
  box-shadow: var(--shadow-glass-sm);
  position: relative;
  overflow: hidden;
}
.el-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.15), transparent);
}

/* === Glass Button === */
.el-button {
  font-weight: 500;
  letter-spacing: 0.3px;
  border-radius: 10px;
  transition: all var(--transition-normal);
  cursor: pointer;
}
.el-button--primary {
  background: linear-gradient(135deg, rgba(168, 139, 250, 0.3), rgba(168, 139, 250, 0.15));
  border: 1px solid rgba(168, 139, 250, 0.4);
  color: #C4B5FD;
  box-shadow: 0 0 20px rgba(168, 139, 250, 0.15);
}
.el-button--primary:hover {
  background: linear-gradient(135deg, rgba(168, 139, 250, 0.4), rgba(168, 139, 250, 0.2));
  border-color: rgba(168, 139, 250, 0.6);
  box-shadow: 0 0 30px rgba(168, 139, 250, 0.25);
  color: #DDD6FE;
}
.el-button--primary:active {
  background: linear-gradient(135deg, rgba(168, 139, 250, 0.25), rgba(168, 139, 250, 0.1));
}
.el-button--default {
  background: var(--glass-surface-md);
  border: 1px solid var(--glass-border-strong);
  color: var(--text-secondary);
}
.el-button--default:hover {
  background: var(--glass-surface-lg);
  border-color: rgba(255, 255, 255, 0.2);
  color: var(--text-primary);
}
.el-button--danger {
  background: linear-gradient(135deg, rgba(248, 113, 113, 0.25), rgba(248, 113, 113, 0.1));
  border: 1px solid rgba(248, 113, 113, 0.35);
  color: #FCA5A5;
  box-shadow: 0 0 16px rgba(248, 113, 113, 0.12);
}
.el-button--danger:hover {
  background: linear-gradient(135deg, rgba(248, 113, 113, 0.35), rgba(248, 113, 113, 0.15));
  border-color: rgba(248, 113, 113, 0.5);
  box-shadow: 0 0 24px rgba(248, 113, 113, 0.2);
  color: #FECACA;
}
.el-button--warning {
  background: linear-gradient(135deg, rgba(251, 191, 36, 0.2), rgba(251, 191, 36, 0.08));
  border: 1px solid rgba(251, 191, 36, 0.3);
  color: #FCD34D;
}
.el-button--warning:hover {
  background: linear-gradient(135deg, rgba(251, 191, 36, 0.3), rgba(251, 191, 36, 0.12));
  border-color: rgba(251, 191, 36, 0.45);
}
.el-button--success {
  background: linear-gradient(135deg, rgba(74, 222, 128, 0.2), rgba(74, 222, 128, 0.08));
  border: 1px solid rgba(74, 222, 128, 0.3);
  color: #86EFAC;
}
.el-button--success:hover {
  background: linear-gradient(135deg, rgba(74, 222, 128, 0.3), rgba(74, 222, 128, 0.12));
  border-color: rgba(74, 222, 128, 0.45);
}
.el-button.is-disabled,
.el-button.is-disabled:hover {
  opacity: 0.4;
  cursor: not-allowed;
}

/* === Glass Input === */
.el-input__wrapper {
  border-radius: 10px;
  background: var(--glass-surface-sm);
  border: 1px solid var(--glass-border-default);
  box-shadow: none;
  transition: all var(--transition-normal);
}
.el-input__wrapper:hover {
  border-color: rgba(255, 255, 255, 0.15);
}
.el-input.is-focus .el-input__wrapper {
  border-color: rgba(168, 139, 250, 0.5);
  box-shadow: 0 0 0 3px rgba(168, 139, 250, 0.1);
}
.el-input__inner {
  color: var(--text-primary);
}
.el-input__inner::placeholder {
  color: var(--text-disabled);
}

/* === Glass Select === */
.el-select .el-input__wrapper {
  border-radius: 10px;
}

/* === Glass Dialog === */
.el-dialog {
  border-radius: var(--radius-xl);
  background: rgba(20, 16, 31, 0.95);
  backdrop-filter: blur(var(--glass-blur-xl));
  -webkit-backdrop-filter: blur(var(--glass-blur-xl));
  border: 1px solid rgba(168, 139, 250, 0.15);
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.5), 0 0 40px rgba(168, 139, 250, 0.08);
  position: relative;
  overflow: hidden;
}
.el-dialog::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
}
.el-dialog__header {
  padding: 20px 24px 16px;
  border-bottom: 1px solid var(--glass-border-default);
}
.el-dialog__title {
  font-family: var(--font-heading);
  font-weight: 600;
  color: var(--text-primary);
}
.el-dialog__body {
  padding: 20px 24px;
  color: var(--text-secondary);
}
.el-dialog__headerbtn .el-dialog__close {
  color: var(--text-muted);
}
.el-dialog__headerbtn .el-dialog__close:hover {
  color: var(--text-primary);
}
.el-overlay {
  background-color: rgba(0, 0, 0, 0.6) !important;
}

/* === Glass Table === */
.el-table {
  --el-table-border-color: var(--glass-border-default);
  --el-table-header-bg-color: rgba(255, 255, 255, 0.04);
  --el-table-row-hover-bg-color: rgba(255, 255, 255, 0.04);
  --el-table-tr-bg-color: transparent;
  background: transparent;
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--text-secondary);
}
.el-table th.el-table__cell {
  background-color: rgba(255, 255, 255, 0.03);
  color: var(--text-muted);
  font-weight: 500;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid var(--glass-border-default);
}
.el-table td.el-table__cell {
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
}
.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell {
  background-color: rgba(255, 255, 255, 0.015);
}
.el-table .el-table__body tr:not(.domain-separator-row):hover > td.el-table__cell {
  background-color: rgba(255, 255, 255, 0.04) !important;
}

/* === Glass Tag === */
.el-tag {
  border-radius: 6px;
  font-weight: 500;
  border: 1px solid;
}
.el-tag--primary {
  background: var(--color-primary-soft);
  border-color: rgba(168, 139, 250, 0.3);
  color: #C4B5FD;
}
.el-tag--success {
  background: var(--color-success-soft);
  border-color: rgba(74, 222, 128, 0.25);
  color: #86EFAC;
}
.el-tag--warning {
  background: var(--color-warning-soft);
  border-color: rgba(251, 191, 36, 0.25);
  color: #FCD34D;
}
.el-tag--danger {
  background: var(--color-danger-soft);
  border-color: rgba(248, 113, 113, 0.25);
  color: #FCA5A5;
}
.el-tag--info {
  background: var(--color-cyan-soft);
  border-color: rgba(34, 211, 238, 0.25);
  color: #67E8F9;
}

/* === Glass Tabs === */
.el-tabs__header {
  border-bottom: 1px solid var(--glass-border-default);
}
.el-tabs__item {
  font-weight: 500;
  color: var(--text-muted);
  transition: color var(--transition-fast);
}
.el-tabs__item.is-active {
  color: var(--color-primary);
}
.el-tabs__active-bar {
  background-color: var(--color-primary);
}
.el-tabs__nav-wrap::after {
  background-color: var(--glass-border-default);
}

/* === Glass Menu === */
.el-menu {
  border-right: none;
  background: transparent;
}
.el-menu-item {
  color: var(--text-secondary);
  font-weight: 400;
  border-radius: 10px;
  transition: all var(--transition-fast);
}
.el-menu-item:hover {
  background: var(--glass-surface-md);
  color: var(--text-primary);
}
.el-menu-item.is-active {
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-weight: 600;
}

/* === Glass Dropdown === */
.el-dropdown-menu {
  background: rgba(20, 16, 31, 0.95);
  backdrop-filter: blur(var(--glass-blur-lg));
  -webkit-backdrop-filter: blur(var(--glass-blur-lg));
  border: 1px solid var(--glass-border-accent);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-glass-md);
}
.el-dropdown-menu__item {
  color: var(--text-secondary);
}
.el-dropdown-menu__item:hover {
  background: var(--glass-surface-md);
  color: var(--text-primary);
}

/* === Glass Form === */
.el-form-item__label {
  color: var(--text-secondary);
}

/* === Glass Divider === */
.el-divider {
  border-color: var(--glass-border-default);
}

/* === Glass Radio === */
.el-radio__label {
  color: var(--text-secondary);
}
.el-radio__input.is-checked + .el-radio__label {
  color: var(--color-primary);
}
.el-radio__inner {
  border-color: var(--glass-border-strong);
  background: var(--glass-surface-sm);
}
.el-radio__input.is-checked .el-radio__inner {
  border-color: var(--color-primary);
  background: var(--color-primary);
}

/* === Glass Checkbox === */
.el-checkbox__label {
  color: var(--text-secondary);
}
.el-checkbox__inner {
  border-color: var(--glass-border-strong);
  background: var(--glass-surface-sm);
}

/* === Message Box === */
.el-message-box {
  background: rgba(20, 16, 31, 0.95);
  backdrop-filter: blur(var(--glass-blur-xl));
  -webkit-backdrop-filter: blur(var(--glass-blur-xl));
  border: 1px solid rgba(168, 139, 250, 0.15);
  border-radius: var(--radius-xl);
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.5);
}
.el-message-box__title {
  font-family: var(--font-heading);
  color: var(--text-primary);
}
.el-message-box__message {
  color: var(--text-secondary);
}
.el-message-box__headerbtn .el-message-box__close {
  color: var(--text-muted);
}

/* === Loading === */
.el-loading-mask {
  background: rgba(9, 9, 11, 0.6);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

/* === Pagination === */
.el-pagination .btn-prev,
.el-pagination .btn-next {
  background: var(--glass-surface-md);
  border: 1px solid var(--glass-border-default);
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
}
.el-pagination .btn-prev:hover,
.el-pagination .btn-next:hover {
  color: var(--color-primary);
  border-color: rgba(168, 139, 250, 0.3);
}
.el-pager li {
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
}
.el-pager li.is-active {
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-weight: 600;
}
.el-pager li:hover {
  color: var(--color-primary);
}

/* === Tooltip / Popover === */
.el-tooltip__popper,
.el-popover {
  background: rgba(20, 16, 31, 0.95) !important;
  backdrop-filter: blur(var(--glass-blur-lg));
  -webkit-backdrop-filter: blur(var(--glass-blur-lg));
  border: 1px solid var(--glass-border-accent) !important;
  border-radius: var(--radius-md) !important;
  box-shadow: var(--shadow-glass-md);
}
```

### Task 4: 更新 index.html 字体引用

**Files:**
- Modify: `frontend/index.html`

- [ ] **Step 1: 在 `<head>` 中添加字体 CSS import**

找到 `<title>` 标签上方，插入字体引用：

将：
```html
  <title>添翼产品清单管理工具</title>
```

改为：
```html
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=Inter:wght@300;400;500;600&display=swap" rel="stylesheet">
  <title>添翼产品清单管理工具</title>
```

### Task 5: 更新 main.js CSS 引入

**Files:**
- Modify: `frontend/src/main.js`

- [ ] **Step 1: 替换 CSS import**

将：
```js
import './styles/mui-theme.css'
```

改为：
```js
import './styles/liquid-glass-theme.css'
```

### Task 6: 更新 App.vue 全局样式

**Files:**
- Modify: `frontend/src/App.vue`

- [ ] **Step 1: 替换 body 样式**

将 `<style>` 块替换为：

```css
<style>
body {
  margin: 0;
  padding: 0;
  background: #09090B;
}
</style>
```

### Task 7: 重构 MainLayout.vue

**Files:**
- Modify: `frontend/src/layout/MainLayout.vue`

- [ ] **Step 1: 替换 template**

替换整个 `<template>` 块为：

```html
<template>
  <div class="glass-layout">
    <div class="glass-bg-orbs">
      <div class="orb orb-violet"></div>
      <div class="orb orb-cyan"></div>
      <div class="orb orb-pink"></div>
    </div>

    <div class="glass-sidebar">
      <div class="logo-area">
        <span class="logo-text">添翼</span>
        <span class="logo-sub">PRO</span>
      </div>
      <el-menu
        :default-active="route.path"
        router
        class="glass-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Monitor /></el-icon>
          <span>产品清单</span>
        </el-menu-item>
        <el-sub-menu v-if="authStore.isAdmin()" index="admin">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/roles">
            <el-icon><Ticket /></el-icon>
            <span>权限套餐管理</span>
          </el-menu-item>
          <el-menu-item index="/versions">
            <el-icon><Document /></el-icon>
            <span>版本管理</span>
          </el-menu-item>
          <el-sub-menu index="base-data">
            <template #title>
              <el-icon><Grid /></el-icon>
              <span>基础数据维护</span>
            </template>
            <el-menu-item index="/base-data/category">
              <el-icon><List /></el-icon>
              <span>业务分类维护</span>
            </el-menu-item>
            <el-menu-item index="/base-data/solution">
              <el-icon><Coin /></el-icon>
              <span>解决方案维护</span>
            </el-menu-item>
            <el-menu-item index="/base-data/app-role">
              <el-icon><UserFilled /></el-icon>
              <span>应用角色维护</span>
            </el-menu-item>
            <el-menu-item index="/base-data/status">
              <el-icon><Flag /></el-icon>
              <span>功能状态维护</span>
            </el-menu-item>
          </el-sub-menu>
        </el-sub-menu>
      </el-menu>
    </div>

    <div class="glass-main">
      <div class="glass-header">
        <div class="header-left">
          <span class="header-breadcrumb">{{ pageTitle }}</span>
        </div>
        <el-dropdown @command="handleCommand">
          <span class="header-user">
            {{ nickname || '用户' }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <div class="glass-content">
        <router-view :key="$route.fullPath" />
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 2: 替换 `<style scoped>`**

替换整个 `<style scoped>` 块为：

```css
<style scoped>
.glass-layout {
  display: flex;
  height: 100vh;
  background: var(--glass-bg-deep);
  position: relative;
  overflow: hidden;
  padding: 12px;
  gap: 12px;
}

/* === Background Orbs === */
.glass-bg-orbs {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
}
.orb-violet {
  width: 50vw;
  height: 50vw;
  top: -15%;
  left: -10%;
  background: radial-gradient(circle, rgba(168, 139, 250, 0.3), transparent 70%);
  animation: morph-blob-1 8s ease-in-out infinite;
}
.orb-cyan {
  width: 40vw;
  height: 40vw;
  bottom: -10%;
  right: -5%;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.2), transparent 70%);
  animation: morph-blob-2 10s ease-in-out infinite;
}
.orb-pink {
  width: 35vw;
  height: 35vw;
  top: 40%;
  left: 50%;
  background: radial-gradient(circle, rgba(244, 114, 182, 0.15), transparent 70%);
  animation: morph-blob-1 12s ease-in-out infinite reverse;
}

/* === Glass Sidebar === */
.glass-sidebar {
  position: relative;
  z-index: 2;
  width: 240px;
  flex-shrink: 0;
  background: var(--glass-surface-sm);
  backdrop-filter: blur(var(--glass-blur-lg));
  -webkit-backdrop-filter: blur(var(--glass-blur-lg));
  border: 1px solid rgba(168, 139, 250, 0.1);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.glass-sidebar::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.1), transparent);
  z-index: 1;
}

.logo-area {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  flex-shrink: 0;
  border-bottom: 1px solid var(--glass-border-default);
}
.logo-text {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #A78BFA, #22D3EE);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.logo-sub {
  font-family: var(--font-heading);
  font-size: 10px;
  color: var(--text-muted);
  margin-left: 6px;
  font-weight: 600;
  letter-spacing: 2px;
}

.glass-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  background: transparent;
  padding: 8px;
}
.glass-menu .el-menu-item {
  color: var(--text-secondary);
  height: 40px;
  line-height: 40px;
  margin: 2px 0;
  border-radius: 10px;
  transition: all var(--transition-fast);
}
.glass-menu .el-menu-item:hover {
  background: var(--glass-surface-md);
  color: var(--text-primary);
}
.glass-menu .el-menu-item.is-active {
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-weight: 600;
}
.glass-menu .el-sub-menu .el-sub-menu__title {
  color: var(--text-secondary);
  height: 40px;
  line-height: 40px;
  margin: 2px 0;
  border-radius: 10px;
}
.glass-menu .el-sub-menu .el-sub-menu__title:hover {
  background: var(--glass-surface-md);
  color: var(--text-primary);
}

/* === Glass Header === */
.glass-main {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.glass-header {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 52px;
  flex-shrink: 0;
  background: var(--glass-surface-sm);
  backdrop-filter: blur(var(--glass-blur-md));
  -webkit-backdrop-filter: blur(var(--glass-blur-md));
  border: 1px solid var(--glass-border-default);
  border-radius: 14px;
  margin-bottom: 4px;
}
.glass-header::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.12), transparent);
}

.header-left {
  display: flex;
  align-items: center;
}
.header-breadcrumb {
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
}

.header-user {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 13px;
  padding: 6px 12px;
  border-radius: 8px;
  transition: all var(--transition-fast);
}
.header-user:hover {
  background: var(--glass-surface-md);
  color: var(--text-primary);
}

/* === Content Area === */
.glass-content {
  flex: 1;
  overflow-y: auto;
  padding: 0;
  min-height: 0;
}
</style>
```

### Task 8: 重构 LoginView.vue

**Files:**
- Modify: `frontend/src/views/login/LoginView.vue`

- [ ] **Step 1: 替换 `<style scoped>`**

只替换 `<style scoped>` 块：

```css
<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--glass-bg-deep);
  position: relative;
  overflow: hidden;
}
.login-container::before {
  content: '';
  position: absolute;
  width: 60vw;
  height: 60vw;
  top: -20%;
  left: -15%;
  background: radial-gradient(circle, rgba(168, 139, 250, 0.2), transparent 70%);
  border-radius: 50%;
  filter: blur(60px);
  animation: morph-blob-1 8s ease-in-out infinite;
}
.login-container::after {
  content: '';
  position: absolute;
  width: 45vw;
  height: 45vw;
  bottom: -15%;
  right: -10%;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.15), transparent 70%);
  border-radius: 50%;
  filter: blur(60px);
  animation: morph-blob-2 10s ease-in-out infinite;
}

.login-card {
  position: relative;
  z-index: 1;
  width: 400px;
  padding: 48px 40px;
  background: rgba(20, 16, 31, 0.8);
  backdrop-filter: blur(var(--glass-blur-xl));
  -webkit-backdrop-filter: blur(var(--glass-blur-xl));
  border: 1px solid rgba(168, 139, 250, 0.15);
  border-radius: var(--radius-xl);
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.5), 0 0 40px rgba(168, 139, 250, 0.08);
  overflow: hidden;
}
.login-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
}

.login-brand {
  text-align: center;
  margin-bottom: 36px;
}

.login-logo {
  width: 56px;
  height: 56px;
  line-height: 56px;
  margin: 0 auto 16px;
  background: linear-gradient(135deg, #A78BFA, #22D3EE);
  color: #09090B;
  font-size: 24px;
  font-weight: 700;
  border-radius: 16px;
  font-family: var(--font-heading);
}

.login-title {
  margin: 0 0 6px;
  color: var(--text-primary);
  font-size: 22px;
  font-weight: 600;
  font-family: var(--font-heading);
}

.login-subtitle {
  margin: 0;
  color: var(--text-muted);
  font-size: 14px;
}
</style>
```

### Task 9: 重构 StatsTab.vue

**Files:**
- Modify: `frontend/src/components/StatsTab.vue`

- [ ] **Step 1: 替换 `<style scoped>`**

```css
<style scoped>
.stats-tab {
  padding: 16px;
  overflow-y: auto;
  height: 100%;
}
.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  flex: 1;
  background: var(--glass-surface-md);
  backdrop-filter: blur(var(--glass-blur-md));
  -webkit-backdrop-filter: blur(var(--glass-blur-md));
  border: 1px solid var(--glass-border-default);
  border-radius: var(--radius-lg);
  padding: 20px;
  text-align: center;
  position: relative;
  overflow: hidden;
}
.stat-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(168, 139, 250, 0.3), transparent);
}
.stat-label {
  color: var(--text-muted);
  font-size: 12px;
  margin-bottom: 8px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 1px;
}
.stat-value {
  color: var(--text-primary);
  font-size: 32px;
  font-weight: 700;
  font-family: var(--font-heading);
}
.stat-charts {
  display: flex;
  gap: 16px;
}
.chart-container {
  flex: 1;
  background: var(--glass-surface-sm);
  backdrop-filter: blur(var(--glass-blur-md));
  -webkit-backdrop-filter: blur(var(--glass-blur-md));
  border: 1px solid var(--glass-border-default);
  border-radius: var(--radius-lg);
  padding: 16px;
  position: relative;
}
.chart-container::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.1), transparent);
}
.chart-title {
  margin: 0 0 12px;
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
  font-family: var(--font-heading);
}
</style>
```

- [ ] **Step 2: 更新 ECharts 颜色配置**

在 `renderBar` 函数中，将 `itemStyle: { color: '#1976d2' }` 改为：

```js
itemStyle: { color: '#A78BFA' }
```

并在 `renderPie` 的 `series` 中增加颜色配置：

```js
series: [{
  type: 'pie',
  radius: ['30%', '60%'],
  data: data.length > 0 ? data : [{ name: '暂无数据', value: 1 }],
  label: { show: true, formatter: '{b}: {c}', color: '#94A3B8' },
  itemStyle: { borderColor: 'rgba(0,0,0,0.3)', borderWidth: 2 }
}]
```

### Task 10: 重构 TreePanel.vue

**Files:**
- Modify: `frontend/src/components/TreePanel.vue`

- [ ] **Step 1: 替换 `<style scoped>`**

```css
<style scoped>
.tree-panel {
  padding: 12px;
}
.tree-title {
  font-weight: 600;
  font-size: 12px;
  padding: 8px 12px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 1px;
  border-bottom: 1px solid var(--glass-border-default);
  margin-bottom: 6px;
  font-family: var(--font-heading);
}
.all-node {
  padding: 8px 12px;
  cursor: pointer;
  font-weight: 600;
  color: var(--text-secondary);
  border-radius: 8px;
  margin: 2px 0;
  transition: all var(--transition-fast);
}
.all-node.active {
  background: var(--color-primary-soft);
  color: var(--color-primary);
}
.all-node:hover {
  background: var(--glass-surface-md);
  color: var(--text-primary);
}
:deep(.el-tree-node__content) {
  border-radius: 8px;
  margin: 1px 0;
  transition: background-color var(--transition-fast);
  color: var(--text-secondary);
}
:deep(.el-tree-node__content:hover) {
  background-color: var(--glass-surface-md);
  color: var(--text-primary);
}
:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background-color: var(--color-primary-soft);
  color: var(--color-primary);
  font-weight: 500;
}
:deep(.el-tree) {
  background: transparent;
  color: var(--text-secondary);
}
</style>
```

### Task 11: 重构 DataWorkbench.vue

**Files:**
- Modify: `frontend/src/views/dashboard/DataWorkbench.vue`

- [ ] **Step 1: 替换 `<style scoped>`**

```css
<style scoped>
.version-pick {
  padding: 40px;
  max-width: 600px;
  margin: 0 auto;
}
.version-pick h3 {
  margin-bottom: 16px;
  font-size: 18px;
  font-family: var(--font-heading);
  color: var(--text-primary);
}
.workbench {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.workbench-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: var(--glass-surface-sm);
  backdrop-filter: blur(var(--glass-blur-md));
  -webkit-backdrop-filter: blur(var(--glass-blur-md));
  border: 1px solid var(--glass-border-default);
  border-radius: var(--radius-md);
  margin-bottom: 12px;
  flex-shrink: 0;
}
.workbench-header::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.1), transparent);
}
.version-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.version-badge {
  font-weight: 700;
  font-size: 16px;
  font-family: var(--font-heading);
  color: var(--color-primary);
}
.version-date {
  color: var(--text-muted);
  font-size: 13px;
}
.readonly-tip {
  color: var(--color-danger);
  font-size: 12px;
}
.tabs-wrapper {
  position: relative;
  height: 100%;
}
.tabs-wrapper :deep(.el-tabs__header) {
  padding-right: 120px;
  padding-left: 8px;
}
.add-list-btn {
  position: absolute;
  top: 8px;
  right: 4px;
  z-index: 1;
}
.workbench-body {
  display: flex;
  flex: 1;
  gap: 12px;
  overflow: hidden;
  min-height: 0;
}
.left-panel {
  width: 260px;
  background: var(--glass-surface-sm);
  backdrop-filter: blur(var(--glass-blur-md));
  -webkit-backdrop-filter: blur(var(--glass-blur-md));
  border: 1px solid var(--glass-border-default);
  border-radius: var(--radius-lg);
  overflow-y: auto;
  flex-shrink: 0;
}
.left-panel::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.1), transparent);
}
.right-panel {
  flex: 1;
  background: var(--glass-surface-sm);
  backdrop-filter: blur(var(--glass-blur-md));
  -webkit-backdrop-filter: blur(var(--glass-blur-md));
  border: 1px solid var(--glass-border-default);
  border-radius: var(--radius-lg);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.right-panel::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.1), transparent);
}
:deep(.el-tabs) { display: flex; flex-direction: column; height: 100%; }
:deep(.el-tabs__header) { flex-shrink: 0; }
:deep(.el-tabs__content) { flex: 1; overflow: hidden; min-height: 0; }
:deep(.el-tab-pane) { height: 100%; overflow: hidden; }

@keyframes spin-glow {
  0% { box-shadow: 0 0 0 0 rgba(168, 139, 250, 0.6); }
  50% { box-shadow: 0 0 6px 3px rgba(168, 139, 250, 0.4); }
  100% { box-shadow: 0 0 0 0 rgba(168, 139, 250, 0.6); }
}

@keyframes spin-rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.spinning-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  border: 2px solid var(--color-primary);
  border-top-color: transparent;
  margin-right: 4px;
  vertical-align: -1px;
  animation: spin-rotate 0.8s linear infinite, spin-glow 1.2s ease-in-out infinite;
}
</style>
```

### Task 12: 重构 DataListTab.vue

**Files:**
- Modify: `frontend/src/components/DataListTab.vue`

- [ ] **Step 1: 替换 `<style scoped>`**

```css
<style scoped>
.data-list-tab {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.query-bar {
  flex-shrink: 0;
  background: var(--glass-surface-sm);
  backdrop-filter: blur(var(--glass-blur-sm));
  -webkit-backdrop-filter: blur(var(--glass-blur-sm));
  border: 1px solid var(--glass-border-default);
  border-radius: var(--radius-md);
  padding: 10px 14px;
  margin-bottom: 10px;
}
.table-toolbar {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.table-body {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding-bottom: 40px;
  background: var(--glass-surface-sm);
  backdrop-filter: blur(var(--glass-blur-md));
  -webkit-backdrop-filter: blur(var(--glass-blur-md));
  border: 1px solid var(--glass-border-default);
  border-radius: var(--radius-lg);
  padding: 12px;
}
.toolbar-title { font-weight: 600; font-size: 14px; color: var(--text-primary); font-family: var(--font-heading); }
:deep(.el-table .cell) { padding: 0 6px !important; }
:deep(.drag-col) { cursor: grab; text-align: center !important; }
:deep(.domain-separator-row > td) {
  height: 28px !important;
  padding: 0 12px !important;
  background-color: var(--color-primary-soft) !important;
  border-bottom: 2px solid rgba(168, 139, 250, 0.3) !important;
  cursor: pointer !important;
  transition: background-color var(--transition-fast);
}
:deep(.domain-separator-row:hover > td) {
  background-color: rgba(168, 139, 250, 0.22) !important;
}
:deep(.el-table__body tr.domain-separator-row:hover > td) {
  background-color: rgba(168, 139, 250, 0.22) !important;
}
:deep(.domain-separator-row .cell) {
  height: 28px !important;
  padding: 0 !important;
  display: flex;
  align-items: center;
}
.sep-label {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  width: 100%;
  gap: 8px;
}
.sep-toggle {
  font-size: 14px;
  flex-shrink: 0;
  color: var(--color-primary);
}
.sep-add-btn {
  margin-left: auto;
  flex-shrink: 0;
  height: 24px;
  font-size: 12px;
  padding: 0 10px;
  background: rgba(168, 139, 250, 0.15);
  border: 1px solid rgba(168, 139, 250, 0.3);
  color: var(--color-primary);
  border-radius: 6px;
  cursor: pointer;
  transition: all var(--transition-fast);
}
.sep-add-btn:hover {
  background: rgba(168, 139, 250, 0.25);
  border-color: rgba(168, 139, 250, 0.5);
}
:deep(.domain-separator-row) {
  user-select: none;
}
:deep(.el-table__row:not(.domain-separator-row) > td) {
  max-height: 200px;
  overflow: hidden;
  transition: max-height 0.5s ease, padding 0.5s ease, opacity 0.5s ease, border-width 0.5s ease;
}
:deep(.collapsed-row) {
  display: none;
}
.row-num { color: var(--text-muted); font-size: 12px; user-select: none; cursor: pointer; }
.op-btn { cursor: pointer; font-size: 12px; margin-right: 8px; user-select: none; }
.op-edit { color: var(--color-primary); }
.op-add { color: var(--color-success); }
.op-del { color: var(--color-danger); }
.op-btn:hover { text-decoration: underline; }
.version-inline { display: flex; gap: 2px; white-space: nowrap; }
.version-row { display: flex; align-items: center; }
.record-count { float: right; color: var(--text-muted); font-size: 12px; margin-left: 6px; white-space: nowrap; }
.level-tag { margin: 0 6px; vertical-align: middle; }
</style>
```

### Task 13: 删除旧主题文件

**Files:**
- Delete: `frontend/src/styles/mui-theme.css`

- [ ] **Step 1: 删除文件**

```bash
rm frontend/src/styles/mui-theme.css
```

### Task 14: 构建验证

**Files:** N/A (验证步骤)

- [ ] **Step 1: 安装依赖（如未安装）并构建**

```bash
cd /Users/craneking/workspace/工程设计/superPowerTest/frontend && npm install
```

- [ ] **Step 2: 执行构建**

```bash
cd /Users/craneking/workspace/工程设计/superPowerTest/frontend && npm run build
```

期望：构建成功，无错误。

- [ ] **Step 3: 清理 visual companion 文件**

```bash
rm -rf /Users/craneking/workspace/工程设计/superPowerTest/.superpowers/brainstorm/58278-1779845264
```
