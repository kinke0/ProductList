# 方案全景图页签 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在产品清单功能的 Tab 页签中新增"方案全景图"页签，以卡片矩阵展示"可交付"状态的产品按 L1/L2/L3 层级排布的全景视图。

**Architecture:** 纯前端方案，新增 `PanoramaTab.vue` 组件，复用现有 `queryEntries` API 获取数据。前端过滤、分组、渲染。修改 `DataWorkbench.vue` 插入新 tab 并处理跳转交互。

**Tech Stack:** Vue 3 Composition API + Element Plus + 系统已有 CSS 变量 (`--si-*`)

---

## File Structure

| 文件 | 操作 | 职责 |
|------|------|------|
| `frontend/src/components/PanoramaTab.vue` | 新增 | 全景图组件：数据加载、分组过滤、卡片渲染、预览弹窗 |
| `frontend/src/views/dashboard/DataWorkbench.vue` | 修改 | 导入 PanoramaTab、插入 panorama tab、添加跳转处理方法 |
| `frontend/src/api/data.js` | 无变更 | 复用 `queryEntries` 和 `getCategoryTree` |

---

### Task 1: 创建 PanoramaTab.vue — 数据层与核心结构

**Files:**
- Create: `frontend/src/components/PanoramaTab.vue`

- [ ] **Step 1: 创建 PanoramaTab.vue 文件骨架**

```vue
<template>
  <div class="panorama-container">
    <div v-if="loading" class="panorama-loading">
      <el-icon class="is-loading" style="font-size:32px;color:#2563EB;"><Loading /></el-icon>
      <span style="margin-top:8px;color:#94A3B8;">加载中...</span>
    </div>
    <template v-else>
      <div v-if="businessSystemGroups.length === 0 && digitalFoundationGroups.length === 0" class="panorama-empty">
        <el-empty description="暂无可交付的产品" />
      </div>
      <template v-else>
        <div v-if="businessSystemGroups.length > 0" class="panorama-section">
          <div class="section-header section-header--primary">业务系统</div>
          <div class="section-body">
            <div class="l1-grid">
              <div v-for="l1 in businessSystemGroups" :key="l1.name" class="l1-card">
                <div class="l1-title" @click="onL1Click(l1.name)">{{ l1.name }}</div>
                <div v-for="l2 in l1.children" :key="l2.name" class="l2-group">
                  <div class="l2-label" @click="onL2Click(l1.name, l2.name)">{{ l2.name }}</div>
                  <div :class="['l3-grid', l2.items.length < 3 ? 'l3-grid--vertical' : '']">
                    <div
                      v-for="item in l2.items"
                      :key="item.id"
                      class="l3-card"
                      @click="onL3Click(item)"
                    >
                      {{ item.displayName }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-if="digitalFoundationGroups.length > 0" class="panorama-section">
          <div class="section-header section-header--dark">数智底座</div>
          <div class="section-body">
            <div class="l1-grid">
              <div v-for="l1 in digitalFoundationGroups" :key="l1.name" class="l1-card">
                <div class="l1-title" @click="onL1Click(l1.name)">{{ l1.name }}</div>
                <div v-for="l2 in l1.children" :key="l2.name" class="l2-group">
                  <div class="l2-label" @click="onL2Click(l1.name, l2.name)">{{ l2.name }}</div>
                  <div :class="['l3-grid', l2.items.length < 3 ? 'l3-grid--vertical' : '']">
                    <div
                      v-for="item in l2.items"
                      :key="item.id"
                      class="l3-card"
                      @click="onL3Click(item)"
                    >
                      {{ item.displayName }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </template>

    <el-dialog v-model="previewVisible" title="预览" width="80%" top="5vh" :close-on-click-modal="true">
      <div style="position:relative;">
        <iframe :srcdoc="previewHtml" style="width:100%;height:70vh;border:1px solid #e2e8f0;border-radius:4px;" />
        <div v-if="previewLoading" style="position:absolute;top:0;left:0;right:0;bottom:0;background:rgba(255,255,255,0.85);display:flex;flex-direction:column;align-items:center;justify-content:center;z-index:10;">
          <el-icon class="is-loading" style="font-size:48px;color:#409eff;"><Loading /></el-icon>
          <span style="margin-top:12px;color:#666;font-size:14px;">加载预览中...</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { queryEntries, getCategoryTree } from '../../api/data'

const props = defineProps({
  versionId: [Number, String],
  selectedNode: Object
})

const emit = defineEmits(['navigate-to-list'])

const loading = ref(false)
const businessSystemGroups = ref([])
const digitalFoundationGroups = ref([])
const previewVisible = ref(false)
const previewHtml = ref('')
const previewLoading = ref(false)

function stripPrefix(name) {
  if (!name) return ''
  return name.replace(/^[\d.]+\s*/, '')
}

function extractPrefix(name) {
  if (!name) return 0
  const m = name.match(/^(\d+)/)
  return m ? parseInt(m[1]) : 0
}

function groupData(entries) {
  const l1Map = new Map()
  for (const entry of entries) {
    const l1Name = entry.colBizCategory || '未分类'
    const l2Name = entry.colBizDomain || '未分组'
    if (!l1Map.has(l1Name)) {
      l1Map.set(l1Name, new Map())
    }
    const l2Map = l1Map.get(l1Name)
    if (!l2Map.has(l2Name)) {
      l2Map.set(l2Name, [])
    }
    l2Map.get(l2Name).push({
      id: entry.id,
      name: entry.colProductSystem || '',
      displayName: stripPrefix(entry.colProductSystem || '')
    })
  }
  const groups = []
  for (const [l1Name, l2Map] of l1Map) {
    const children = []
    for (const [l2Name, items] of l2Map) {
      children.push({ name: l2Name, items })
    }
    groups.push({ name: l1Name, children })
  }
  groups.sort((a, b) => extractPrefix(a.name) - extractPrefix(b.name))
  return groups
}

async function loadData() {
  if (!props.versionId) return
  loading.value = true
  try {
    const res = await queryEntries(props.versionId, {})
    const allEntries = (res.data || []).filter(e => e.level === 3 && e.colStatus && e.colStatus.includes('可交付'))
    const business = allEntries.filter(e => {
      const prefix = extractPrefix(e.colBizCategory || '')
      return prefix >= 5 && prefix <= 9
    })
    const foundation = allEntries.filter(e => {
      const prefix = extractPrefix(e.colBizCategory || '')
      return prefix >= 1 && prefix <= 4
    })
    businessSystemGroups.value = groupData(business)
    digitalFoundationGroups.value = groupData(foundation)
  } catch (e) {
    console.error('加载全景图数据失败:', e)
    businessSystemGroups.value = []
    digitalFoundationGroups.value = []
  } finally {
    loading.value = false
  }
}

function onL1Click(l1Name) {
  emit('navigate-to-list', { categoryLabel: l1Name, domainLabel: '' })
}

function onL2Click(l1Name, l2Name) {
  emit('navigate-to-list', { categoryLabel: l1Name, domainLabel: l2Name })
}

async function onL3Click(item) {
  previewLoading.value = true
  previewVisible.value = true
  previewHtml.value = ''
  try {
    const token = localStorage.getItem('token')
    const resp = await fetch(`/api/data/${item.id}/preview`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (resp.ok) {
      previewHtml.value = await resp.text()
    } else {
      previewHtml.value = '<p>加载预览失败</p>'
    }
  } catch {
    previewHtml.value = '<p>加载预览失败</p>'
  } finally {
    previewLoading.value = false
  }
}

onMounted(() => { loadData() })

watch(() => props.versionId, () => { loadData() })
</script>

<style scoped>
.panorama-container {
  padding: 16px;
  height: 100%;
  overflow-y: auto;
  background: #F8FAFC;
}
.panorama-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
}
.panorama-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
}
.panorama-section {
  margin-bottom: 20px;
}
.section-header {
  padding: 10px 16px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  border-radius: 8px 8px 0 0;
  letter-spacing: 1px;
}
.section-header--primary {
  background: #2563EB;
}
.section-header--dark {
  background: #0F172A;
}
.section-body {
  background: #fff;
  border: 1px solid #E2E8F0;
  border-top: none;
  border-radius: 0 0 8px 8px;
  padding: 16px;
}
.l1-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.l1-card {
  flex: 1;
  min-width: 280px;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 12px;
}
.l1-title {
  font-size: 14px;
  font-weight: 600;
  color: #0F172A;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 2px solid #2563EB;
  cursor: pointer;
}
.l1-title:hover {
  color: #2563EB;
}
.l2-group {
  margin-bottom: 10px;
}
.l2-label {
  display: inline-block;
  background: rgba(37, 99, 235, 0.08);
  color: #2563EB;
  font-size: 12px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 6px;
  cursor: pointer;
}
.l2-label:hover {
  background: rgba(37, 99, 235, 0.15);
}
.l3-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.l3-grid--vertical {
  flex-direction: column;
}
.l3-card {
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 13px;
  color: #0F172A;
  cursor: pointer;
  transition: all 0.15s;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.l3-card:hover {
  background: #F1F5F9;
  border-color: #2563EB;
  color: #2563EB;
}
</style>
```

- [ ] **Step 2: 验证文件已创建**

Run: `ls -la frontend/src/components/PanoramaTab.vue`
Expected: 文件存在

---

### Task 2: 修改 DataWorkbench.vue — 集成全景图 Tab

**Files:**
- Modify: `frontend/src/views/dashboard/DataWorkbench.vue`

需要在以下位置进行修改：
1. 第 237-246 行：添加 PanoramaTab 导入
2. 第 66-101 行：在 el-tabs 中第一个位置插入 panorama tab
3. 第 493-501 行：onTabClick 添加 panorama 刷新
4. 添加 onNavigateToList 方法
5. 第 372-377 行：版本切换时默认跳转到 panorama tab

- [ ] **Step 1: 在 import 区域添加 PanoramaTab 导入**

在 `DataWorkbench.vue` 第 241 行（`import DataListTab` 之后）添加：

```javascript
import PanoramaTab from '../../components/PanoramaTab.vue'
```

即修改第 239-241 行，从：

```
import TreePanel from '../../components/TreePanel.vue'
import StatsTab from '../../components/StatsTab.vue'
import DataListTab from '../../components/DataListTab.vue'
```

改为：

```
import TreePanel from '../../components/TreePanel.vue'
import StatsTab from '../../components/StatsTab.vue'
import DataListTab from '../../components/DataListTab.vue'
import PanoramaTab from '../../components/PanoramaTab.vue'
```

- [ ] **Step 2: 在 el-tabs 中第一个位置插入 panorama tab**

在 `DataWorkbench.vue` 第 66-67 行之间（`<el-tabs ...>` 开始标签之后，`<el-tab-pane label="统计视图"` 之前）插入：

```html
            <el-tab-pane label="方案全景图" name="panorama">
              <PanoramaTab
                :version-id="selectedVersion.id"
                :selected-node="selectedNode"
                @navigate-to-list="onNavigateToList"
              />
            </el-tab-pane>
```

- [ ] **Step 3: 添加 panoramaRefreshTrigger 响应式变量**

在第 274 行（`const listRefreshTrigger = ref(0)` 之后）添加：

```javascript
const panoramaRefreshTrigger = ref(0)
```

- [ ] **Step 4: 修改 onTabClick 添加 panorama 分支**

修改第 493-501 行的 `onTabClick` 函数，在 `stats` 分支之后、`list` 分支之前添加 panorama 分支：

从：

```javascript
 function onTabClick(tab) {
  if (tab.paneName === 'stats') {
    statsRefreshTrigger.value = Date.now()
  } else if (tab.paneName === 'list') {
    listRefreshTrigger.value = Date.now()
  } else if (tab.paneName?.startsWith('custom-')) {
    customTabRefresh.value++
  }
 }
```

改为：

```javascript
 function onTabClick(tab) {
  if (tab.paneName === 'panorama') {
    panoramaRefreshTrigger.value = Date.now()
  } else if (tab.paneName === 'stats') {
    statsRefreshTrigger.value = Date.now()
  } else if (tab.paneName === 'list') {
    listRefreshTrigger.value = Date.now()
  } else if (tab.paneName?.startsWith('custom-')) {
    customTabRefresh.value++
  }
 }
```

- [ ] **Step 5: 添加 onNavigateToList 方法**

在 `onTreeSelect` 函数（第 390-392 行）之后添加：

```javascript
function onNavigateToList({ categoryLabel, domainLabel }) {
  selectedNode.value = { id: 'custom', categoryLabel, domainLabel }
  activeTab.value = 'list'
  listRefreshTrigger.value = Date.now()
}
```

- [ ] **Step 6: 修改版本切换默认 tab 为 panorama**

修改第 375 行，从 `activeTab.value = 'stats'` 改为 `activeTab.value = 'panorama'`：

从：

```javascript
    activeTab.value = 'stats'
```

改为：

```javascript
    activeTab.value = 'panorama'
```

- [ ] **Step 7: 验证前端构建**

Run: `cd frontend && npm run build`
Expected: 构建成功，无错误

---

### Task 3: 验证功能完整性

- [ ] **Step 1: 启动后端服务（如未运行）**

Run: `curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/versions`
Expected: 200 或 401（说明服务在运行）

如果服务未运行，使用 `mvn spring-boot:run` 启动后端。

- [ ] **Step 2: 启动前端开发服务（如未运行）**

Run: `curl -s -o /dev/null -w "%{http_code}" http://localhost:5173`
Expected: 200

如果未运行，使用 `cd frontend && npm run dev` 启动。

- [ ] **Step 3: 检查全景图 tab 显示**

确认"方案全景图" tab 显示为第一个 tab，点击后能看到业务系统和数智底座两个区域，卡片布局正确。

- [ ] **Step 4: 提交代码**

```bash
git add frontend/src/components/PanoramaTab.vue frontend/src/views/dashboard/DataWorkbench.vue
git commit -m "feat: 添加方案全景图页签"
```
