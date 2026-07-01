# Excel导出智能化冒泡显示

## Context

用户反馈：清单中的AI标签采用冒泡方式显示（L5标注了智能化时，折叠状态下L4也会显示AI标签），但Excel导出中的"智能化"列只读取当前输出行的 `colIntelligent` 字段，没有冒泡逻辑。

例如：L5标注了智能化，但Excel只生成到L4级，那L4行的"智能化"列应该显示"是"而非空。

## CodeGraph 分析结果

- `generateExcel` (DocumentService.java:1425) — 入口方法，遍历 L3(roots) → L4(l4List)，L5级节点虽然在 entries 中但不会被单独输出为行
- `writeExcelRow` (DocumentService.java:1558) — 写入每行数据，当前智能化列只读取 `target.getColIntelligent()`
- `childrenByParent` (第1473-1480行) — 以 parentId 为 key 收集所有子节点（包括 L5），但当前只取 `l4List = childrenByParent.getOrDefault(l3.getId())` 来获取 L4，没有递归获取 L5

## 修改文件

- `src/main/java/com/superpower/modules/document/service/DocumentService.java`

## 实施步骤

### Step 1: 新增 `hasIntelligentDescendant` 辅助方法

在 `DocumentService` 中新增递归方法，检查一个条目及其所有后代是否有智能化标记：

```java
private boolean hasIntelligentDescendant(DataEntry entry, Map<Long, List<DataEntry>> childrenByParent) {
    // 自身是智能化
    if ("1".equals(entry.getColIntelligent())) return true;
    // 递归检查子节点
    List<DataEntry> children = childrenByParent.getOrDefault(entry.getId(), new ArrayList<>());
    for (DataEntry child : children) {
        if (hasIntelligentDescendant(child, childrenByParent)) return true;
    }
    return false;
}
```

### Step 2: 修改 writeExcelRow 方法，增加冒泡参数

给 `writeExcelRow` 新增 `boolean intelligentBubble` 参数，当冒泡为 true 时显示"是"：

```java
// 修改前
c5.setCellValue("1".equals(target.getColIntelligent()) ? "是" : "");

// 修改后
c5.setCellValue(("1".equals(target.getColIntelligent()) || intelligentBubble) ? "是" : "");
```

### Step 3: 修改 generateExcel 中的调用点

在遍历 L4 时，调用 `hasIntelligentDescendant` 检查 L4 是否有智能化后代（L5等），传递冒泡标记给 `writeExcelRow`。

**关键逻辑**：冒泡只在 L4 有子节点（L5）但子节点不会单独成行时生效。

- 第1491行：L3 单独成行时，检查 L3 自身及其所有后代的智能化 → 传 `hasIntelligentDescendant(l3, childrenByParent)`
- 第1496行：L4 成行时，检查 L4 自身及其后代的智能化 → 传 `hasIntelligentDescendant(l4, childrenByParent)`

但需要注意：如果 L3 本身标注了智能化，当前逻辑已经是 `target.getColIntelligent()` 会读取到。所以冒泡的意义是：当 L4 行输出时，其子节点（L5）有智能化但 L5 不单独成行。

### Step 4: 更新 VERSION.md

### Step 5: 验证
1. 后端编译 `mvn compile -Dmaven.test.skip=true`
2. 前端构建 `npm run build`
3. 重启前后端服务，curl验证端口正常
4. curl 调用 `/api/versions` 检查无500错误
5. 在系统中选择包含L5智能化产品的条目，生成Excel文档，验证L4行的"智能化"列显示"是"
