# 实施计划：清单备注标签展示

## 目标

在清单名称列后显示橙色圆形感叹号备注标签，支持：
1. 有备注的节点直接显示标签，悬浮显示备注内容
2. 未展开时，子节点的备注冒泡到父节点显示
3. 展开后，父节点标签消失，备注标签显示在各自子节点上
4. 父节点悬浮时显示所有子节点备注，附带章节名称（仅多条时显示名称）

## 涉及文件

| 文件 | 改动类型 | 说明 |
|------|---------|------|
| `DataEntrySummaryDTO.java` | 修改 | 新增 `colRemark` 字段和 `fromEntity` 映射 |
| `DataListTab.vue` | 修改 | 新增备注标签渲染逻辑、`collectRemarks` 函数、悬浮提示、样式 |

## 分步实施

### 第 1 步：后端 — DTO 补充 `colRemark`

- `DataEntrySummaryDTO.java`：新增 `private String colRemark;` 字段
- `fromEntity` 方法新增：`dto.setColRemark(e.getColRemark());`

### 第 2 步：前端 — 备注收集函数

在 `getDescendantCount` 后新增：
- `collectRemarks(row)`：递归收集节点自身 + 未展开子节点的备注
- `getRemarks(row)`：调用 `collectRemarks` 并过滤空备注

### 第 3 步：前端 — 模板渲染

在 `<span class="product-name">` 后、`<span class="record-count">` 前插入：
- `el-tooltip` 包裹橙色感叹号圆形标签
- 多条备注时显示章节名称前缀，单条时不显示

### 第 4 步：前端 — 样式

```css
.remark-badge {
  display: inline-flex; align-items: center; justify-content: center;
  width: 16px; height: 16px; border-radius: 50%;
  background: #E6A23C; color: #fff; font-size: 11px; font-weight: bold;
  margin-left: 4px; flex-shrink: 0; cursor: pointer;
}
```

## 交互逻辑

| 场景 | 父节点标签 | 子节点标签 | 悬浮内容 |
|------|-----------|-----------|---------|
| 父节点自身有备注 | 显示 | — | 自身备注 |
| 子节点有备注，父节点折叠 | 父节点显示 | 不可见 | 所有子节点备注+名称 |
| 子节点有备注，父节点展开 | 不显示 | 各子节点显示 | 各自备注 |
| 父+子都有备注，父节点折叠 | 父节点显示 | 不可见 | 父+所有子节点备注 |

## 验证

1. `mvn compile` 后端编译通过
2. `npm run build` 前端构建通过
