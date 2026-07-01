# 方案全景图页签设计文档

## 概述

在产品清单功能的 Tab 页签中，新增"方案全景图"页签，作为所有页签的第一个。该页签以卡片矩阵的形式，展示当前版本中功能状态包含"可交付"的产品（level=3）按 L1/L2/L3 层级排布的全景视图。

## 需求

### 核心需求

1. **新增页签**：在 DataWorkbench.vue 的 el-tabs 中，第一个位置插入"方案全景图"tab（name=`panorama`）
2. **两区布局**：上半部分"业务系统"（L1 章节前缀 5-9），下半部分"数智底座"（L1 章节前缀 1-4）
3. **数据过滤**：仅展示 col_状态 包含"可交付"的 level=3 产品数据
4. **层级排布**：L1=业务分类 > L2=业务域 > L3=产品，按全景图示意排列
5. **跟随版本**：随 DataWorkbench 版本选择器联动

### 交互需求

| 操作 | 行为 |
|------|------|
| 点击 L1 标题 | 跳转到"数据清单"页签，选中左侧树对应 L1 节点 |
| 点击 L2 标题 | 跳转到"数据清单"页签，选中左侧树对应 L2 节点 |
| 点击 L3 卡片 | 弹出预览弹窗（调用 `/api/data/{id}/preview`） |

### 显示规则

- L3 卡片仅显示产品纯中文名称（去掉编号前缀，如 "5.1.1 数据资源管理平台" → "数据资源管理平台"）
- L3 数量少于 3 个时采用纵向排列，3 个及以上采用横向排列（flex-wrap）
- 没有"可交付"产品的 L1/L2 分组不显示
- 整个区域无数据时显示"暂无可交付的产品"空状态提示

## 技术方案

### 方案选型

**方案 A（采用）：纯前端组件**

- 新增 `PanoramaTab.vue`，复用现有 API
- 无需新增后端接口
- 与 StatsTab.vue 模式一致

### 文件变更

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `frontend/src/components/PanoramaTab.vue` | 新增 | 全景图组件 |
| `frontend/src/views/dashboard/DataWorkbench.vue` | 修改 | 添加 panorama tab |

### 复用 API

| API | 用途 |
|-----|------|
| `GET /api/data/query/{versionId}` | 获取 level=3 产品数据 |
| `GET /api/tree?versionId=X` | 获取 L1/L2 树结构（可选，用于点击跳转时构建 selectedNode） |
| `GET /api/data/{id}/preview` | L3 预览 |

## 组件设计

### PanoramaTab.vue

**Props：**

| Prop | 类型 | 说明 |
|------|------|------|
| `versionId` | Number/String | 当前版本 ID |
| `selectedNode` | Object | 树选中节点 |

**Emits：**

| Event | 参数 | 说明 |
|-------|------|------|
| `navigate-to-list` | `{ categoryLabel, domainLabel }` | 通知父组件跳转到数据清单并选中树节点 |

### 内部状态

```javascript
const businessSystemGroups = ref([])  // 上半部数据（L1前缀5-9）
const digitalFoundationGroups = ref([])  // 下半部数据（L1前缀1-4）
const loading = ref(false)
const previewVisible = ref(false)
const previewHtml = ref('')
const previewEntryId = ref(null)
```

### 数据处理流程

```
1. queryEntries(versionId, {}) → 获取所有 level=3 数据
2. 过滤 colStatus 包含 "可交付"
3. 按 colBizCategory(L1) 分组 → 按 colBizDomain(L2) 子分组
4. 按 L1 name 数字前缀划分 businessSystem(5-9) / digitalFoundation(1-4)
5. L1/L2 内部按 sortOrder 排序
6. 产品名称去除编号前缀（正则：去除开头的 "数字.数字.数字 " 模式）
```

### 预览功能

在 PanoramaTab 内部实现简化版预览弹窗（el-dialog + iframe srcdoc），与 DataListTab 预览逻辑一致但不含编辑/审批操作。

## 配色方案

基于系统 SI 设计体系（主色 `#2563EB`，Tailwind Slate 蓝灰色调）：

| 元素 | 配色 | 用途 |
|------|------|------|
| 上半部区域标题 "业务系统" | `#2563EB` 背景，白色文字 | 主色标题 |
| 下半部区域标题 "数智底座" | `#0F172A` 背景，白色文字 | 深色标题 |
| L1 卡片 | `#FFFFFF` 背景，`#E2E8F0` 边框，`border-radius: 8px` | 标准卡片 |
| L1 标题文字 | `#0F172A`，font-weight: 600 | 主文字色 |
| L2 标签 | `rgba(37,99,235,0.08)` 背景，`#2563EB` 文字，`border-radius: 4px` | 域分隔行风格 |
| L3 产品卡片 | `#F8FAFC` 背景，hover `#F1F5F9`，`border-radius: 6px` | 浅色交互 |
| L3 卡片文字 | `#0F172A`，font-size: 13px | 标准文字 |
| 页面背景 | `#F8FAFC` | 全局背景 |

## 页面布局

```
┌──────────────────────────────────────────────────────────┐
│  ■ 业务系统（主色背景标题条）                               │
│  ┌──────────────────────┐ ┌──────────────────────┐      │
│  │ L1: 5. 智慧医疗       │ │ L1: 6. 智慧服务       │      │
│  │  ▪ 5.1 门诊诊疗业务   │ │  ▪ 6.1 xxx          │      │
│  │  ┌────┐ ┌────┐ ┌────┐│ │  ┌────┐ ┌────┐     │      │
│  │  │L3卡│ │L3卡│ │L3卡││ │  │L3卡│ │L3卡│     │      │
│  │  └────┘ └────┘ └────┘│ │  └────┘ └────┘     │      │
│  │  ▪ 5.2 急诊诊疗业务   │ │                      │      │
│  │  ┌────┐              │ │                      │      │
│  │  │L3卡│              │ │                      │      │
│  │  └────┘              │ │                      │      │
│  └──────────────────────┘ └──────────────────────┘      │
├──────────────────────────────────────────────────────────┤
│  ■ 数智底座（深色背景标题条）                               │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐    │
│  │ L1: 1.xxx    │ │ L1: 2.xxx    │ │ L1: 3.xxx    │    │
│  │  ▪ L2 ...    │ │  ▪ L2 ...    │ │  ▪ L2 ...    │    │
│  │  ┌────┐┌────┐│ │  ┌────┐┌────┐│ │  ┌────┐     │    │
│  │  │L3卡││L3卡││ │  │L3卡││L3卡││ │  │L3卡│     │    │
│  │  └────┘└────┘│ │  └────┘└────┘│ │  └────┘     │    │
│  └──────────────┘ └──────────────┘ └──────────────┘    │
└──────────────────────────────────────────────────────────┘
```

## DataWorkbench.vue 变更

1. 在 `<el-tabs>` 中第一个位置插入：
```html
<el-tab-pane label="方案全景图" name="panorama">
  <PanoramaTab
    :version-id="selectedVersion.id"
    :selected-node="selectedNode"
    @navigate-to-list="onNavigateToList"
  />
</el-tab-pane>
```

2. 导入 PanoramaTab 组件

3. 新增 `onNavigateToList` 方法：
```javascript
function onNavigateToList({ categoryLabel, domainLabel }) {
  // 构造 selectedNode
  selectedNode.value = { id: 'custom', categoryLabel, domainLabel }
  // 切换到数据清单 tab
  activeTab.value = 'list'
  // 触发数据刷新
  listRefreshTrigger.value = Date.now()
}
```

4. 在 `onTabClick` 中添加 panorama 刷新逻辑
