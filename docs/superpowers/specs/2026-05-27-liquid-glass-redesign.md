# Liquid Glass UI 风格重构 — 设计规格说明

> 项目：添翼产品清单管理工具
> 日期：2026-05-27
> 状态：已确认

---

## 1. 概述

将"添翼产品清单管理工具"从现有 Material Design (MUI) 蓝色亮色主题，全面重构为 **Liquid Glass（液态玻璃）暗色风格**。采用"方案 B：全面液态玻璃"，在 Vue 3 + Element Plus 基础上实现深空暗色基底、iridescent 彩虹渐变、多层 backdrop-blur 玻璃叠加、以及流体动画系统。

### 设计目标

- 视觉体验全面升级，呈现高端、专业的液态玻璃美学
- 深色模式降低长时间数据操作的视觉疲劳
- 保持 Element Plus 组件 API 完全兼容，零破坏性改动
- 动画系统纯 CSS 实现，零额外 JS 依赖
- 尊重 `prefers-reduced-motion`，提供无障碍降级

---

## 2. 色彩体系

### 2.1 基底色（深空暗色）

| Token | 色值 | 用途 |
|-------|------|------|
| `--glass-bg-deep` | `#09090B` | 页面根背景 |
| `--glass-bg-space` | `#0F0B1A` | 背景渐变起点 |
| `--glass-bg-base` | `#14101F` | 面板/容器底色 |
| `--glass-bg-elevated` | `#1E1830` | 悬浮面板底色 |

### 2.2 玻璃叠加层

| Token | 色值 | blur | 用途 |
|-------|------|------|------|
| `--glass-surface-card` | `rgba(255,255,255,0.04)` | 16px | 卡片/面板 |
| `--glass-surface-sidebar` | `rgba(255,255,255,0.03)` | 24px | 侧边栏 |
| `--glass-surface-modal` | `rgba(255,255,255,0.06)` | 32px | 弹窗/Dialog |
| `--glass-border-default` | `rgba(255,255,255,0.08)` | - | 默认玻璃边框 |
| `--glass-border-accent` | `rgba(168,139,250,0.15)` | - | 强调边框 |

### 2.3 Iridescent 彩虹强调色

| Token | 色值 | 角色 |
|-------|------|------|
| `--color-primary` | `#A78BFA` | 主色调 (Violet) |
| `--color-accent-cyan` | `#22D3EE` | 高亮交互 (Cyan) |
| `--color-accent-pink` | `#F472B6` | 辅助强调 (Pink) |
| `--gradient-iridescent` | `violet → cyan → pink` | 主渐变 |

### 2.4 语义色

| Token | 色值 |
|-------|------|
| `--color-success` | `#4ADE80` |
| `--color-warning` | `#FBBF24` |
| `--color-danger` | `#F87171` |

### 2.5 文字色

| Token | 色值 | 用途 |
|-------|------|------|
| `--text-primary` | `#F1F5F9` | 正文/标题 |
| `--text-secondary` | `#94A3B8` | 辅助文字 |
| `--text-muted` | `#64748B` | 占位/禁用 |

---

## 3. 字体方案

| 角色 | 字体 | 字重 |
|------|------|------|
| 标题/卡片头部/导航 | Space Grotesk | 400, 500, 600, 700 |
| 正文/表格/表单 | Inter | 300, 400, 500, 600 |

Google Fonts 引入：
```css
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=Inter:wght@300;400;500;600&display=swap');
```

现有 Roboto 字体引用将被替换。

---

## 4. 布局架构

### 4.1 浮动玻璃布局

三段式布局（侧边栏 + Header + 内容区），所有面板与视口边缘保持 12px 间距，呈现"悬浮"感：

```
┌──────────────────────────────────────────────┐
│  ┌──────┐  ┌───────────────────────────┐     │
│  │      │  │  Glass Header (blur 20px) │     │
│  │Glass │  └───────────────────────────┘     │
│  │Side- │  ┌───────────────────────────┐     │
│  │bar   │  │  Stat Cards (blur 16px)  │     │
│  │blur  │  ├───────────────────────────┤     │
│  │24px  │  │  Content Panel           │     │
│  │      │  │  (blur 20px)             │     │
│  │      │  │  - Tree / Table / Chart  │     │
│  └──────┘  └───────────────────────────┘     │
│    12px spacing from all edges               │
│  Background: deep space + animated orbs      │
└──────────────────────────────────────────────┘
```

### 4.2 背景光斑系统

页面背景叠加 3 层径向渐变光斑，隐藏在内容之下，透过面板间隙和半透明玻璃可见：

- Violet 光斑：`radial-gradient(ellipse 80% 60% at 20% 30%, rgba(168,139,250,0.12), transparent)`
- Cyan 光斑：`radial-gradient(ellipse 60% 50% at 80% 70%, rgba(34,211,238,0.08), transparent)`
- Pink 光斑：`radial-gradient(ellipse 50% 40% at 50% 50%, rgba(244,114,182,0.06), transparent)`

光斑附加 CSS 变形动画（border-radius 关键帧，6秒周期）。

### 4.3 深度系统

| 层级 | blur 值 | z-index | 元素 |
|------|---------|---------|------|
| L0 | 0 | 0 | 背景光斑 |
| L1 | 16px | 1 | 统计卡片 |
| L2 | 20px | 1 | 内容面板 / Header |
| L3 | 24px | 2 | 侧边栏 |
| L4 | 32px | 1000+ | Dialog / 弹窗 |

### 4.4 顶部光线反射

每个玻璃面板顶部有一道微弱水平高光线（`::before` 伪元素）：
```css
background: linear-gradient(90deg, transparent, rgba(255,255,255,0.15), transparent);
height: 1px;
```

---

## 5. 组件风格

### 5.1 按钮 (ElButton)

| 变体 | 背景 | 边框 | 特效 |
|------|------|------|------|
| Primary | violet 渐变 | `rgba(168,139,250,0.4)` | 霓虹发光阴影 |
| Default | 白色 6% | `rgba(255,255,255,0.12)` | - |
| Danger | red 渐变 | `rgba(248,113,113,0.35)` | red 发光 |
| Ghost | 透明 | 透明 | hover 微亮 |

尺寸：sm (8px radius), default (10px), lg (12px)
圆角统一 10px，hover 时 glow 增强

### 5.2 输入框 (ElInput)

- 背景：`rgba(255,255,255,0.04)`
- 边框：`rgba(255,255,255,0.1)`
- 圆角：10px
- Focus：边框 `rgba(168,139,250,0.5)` + violet glow 环
- Placeholder：`#475569`

### 5.3 对话框 (ElDialog)

- 背景：`rgba(20,16,31,0.95)` + blur 32px
- 边框：`rgba(168,139,250,0.15)`
- 圆角：20px
- 阴影：多层深色阴影 + violet 外发光
- 遮罩：`rgba(0,0,0,0.6)` + blur

### 5.4 表格 (ElTable)

- 容器：玻璃面板 blur 16px
- 表头：大写 + 0.5px 字间距 + muted 色
- 行分隔：`rgba(255,255,255,0.04)` 底部边框
- Hover 行：`rgba(255,255,255,0.04)` 背景
- 业务域分隔行：violet 背景 `rgba(168,139,250,0.08)`

### 5.5 树形控件 (ElTree)

- hover：`rgba(255,255,255,0.04)` 背景
- 选中态：`rgba(168,139,250,0.12)` + violet 文字
- 展开箭头：violet 色

### 5.6 标签页 (ElTabs)

- 胶囊式切换，类似 iOS 分段控件
- 容器：`rgba(255,255,255,0.03)` + 4px padding + 12px 圆角
- 选中态：`rgba(168,139,250,0.18)` + violet 文字 + 内阴影

### 5.7 标签 (ElTag)

对应 iridescent 五色体系：
- Violet：智慧门诊 / 默认
- Cyan：智慧临床 / 信息
- Pink：患者服务 / 辅助
- Green：已发布 / 成功
- Amber：编辑中 / 警告

### 5.8 滚动条

- 宽度：5px
- 轨道：透明
- 滑块：`rgba(168,139,250,0.2)` + 10px 圆角
- hover：`rgba(168,139,250,0.35)`

---

## 6. 动画系统

### 6.1 缓动曲线

| 名称 | 函数 | 时长 | 场景 |
|------|------|------|------|
| Fluid Ease | `cubic-bezier(0.4,0,0.2,1)` | 150-200ms | 悬停/焦点 |
| Glass Morph | `cubic-bezier(0.34,1.56,0.64,1)` | 400-600ms | 面板展开/弹窗 |
| Iris Flow | `cubic-bezier(0.65,0,0.35,1)` | 600-1000ms | 颜色过渡/渐变 |
| Depth Float | `ease-out` + 50ms stagger | 300-500ms | 多层进入 |

### 6.2 关键动效

1. **光线扫描 (Light Sweep)**：按钮/卡片 hover 时高光从左到右扫过，3s 周期
2. **虹彩渐变流动 (Iridescent Shift)**：装饰分割线/进度条上的 violet→cyan→pink 渐变色位移动画
3. **光斑变形 (Morphing Blob)**：背景装饰光斑 CSS border-radius 关键帧变形，6s 周期
4. **呼吸发光 (Glow Pulse)**：主按钮和通知点的 scale+opacity 脉冲，2s 周期
5. **景深错开 (Depth Stagger)**：多层玻璃面板 50ms 间隔 + Y 轴位移差进入

### 6.3 性能约束

- 所有动画使用 `transform` 和 `opacity`，避免 layout thrashing
- 光线扫描仅 hover 时触发，非持续循环
- 虹彩渐变流动仅用于装饰元素，不在内容区
- backdrop-blur 叠加层数控制在 5-8 层
- 检测 `prefers-reduced-motion` 后降级为静态效果

---

## 7. 文件结构

### 7.1 新增文件

```
frontend/src/styles/
  liquid-glass-vars.css    # CSS 自定义属性（Design Token）
  liquid-glass-anim.css    # 动画 @keyframes
  liquid-glass-theme.css   # Element Plus 全局覆盖 + 组件主题
```

### 7.2 修改文件

| 文件 | 改动内容 |
|------|----------|
| `frontend/index.html` | 字体引用 Roboto → Inter + Space Grotesk |
| `frontend/src/main.js` | CSS 引入路径更新 |
| `frontend/src/layout/MainLayout.vue` | 背景光斑 + 浮动玻璃布局 + 景深动画 |
| `frontend/src/views/login/LoginView.vue` | 登录页全玻璃效果 |
| `frontend/src/views/dashboard/DataWorkbench.vue` | 工作台玻璃面板 |
| `frontend/src/views/system/*.vue` | 管理页面统一样式适配 |
| `frontend/src/components/TreePanel.vue` | 玻璃树容器 |
| `frontend/src/components/DataListTab.vue` | 玻璃表格样式 |
| `frontend/src/components/StatsTab.vue` | 统计卡片玻璃化 |

### 7.3 移除文件

```
frontend/src/styles/mui-theme.css    # 旧 MUI 主题
```

---

## 8. 实施步骤

1. **创建 CSS 变量文件** (`liquid-glass-vars.css`) — 定义所有 Design Token
2. **创建动画关键帧文件** (`liquid-glass-anim.css`) — 所有 @keyframes
3. **创建核心主题文件** (`liquid-glass-theme.css`) — Element Plus 全局覆盖
4. **更新入口文件** — index.html 字体 + main.js CSS 路径
5. **重构 MainLayout.vue** — 背景光斑 + 浮动玻璃布局
6. **逐页面适配** — LoginView → DataWorkbench → 管理页面
7. **组件级调整** — TreePanel / DataListTab / StatsTab
8. **最终检查** — 对比度 / 响应式 / reduced-motion / 清理残留

---

## 9. 前置检查清单

- [ ] 无 emoji 图标（使用 Heroicons/Lucide/Element Plus Icons）
- [ ] cursor-pointer 在全部可交互元素
- [ ] 悬停状态有平滑过渡 (150-300ms)
- [ ] 暗色模式文字对比度 ≥ 4.5:1
- [ ] 键盘导航可见焦点状态
- [ ] prefers-reduced-motion 被尊重
- [ ] 375px / 768px / 1024px / 1440px 响应式正常
- [ ] 无水平滚动条
