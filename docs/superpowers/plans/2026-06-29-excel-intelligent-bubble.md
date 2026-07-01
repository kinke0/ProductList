# Excel导出增加智能化冒泡显示

## Context

用户需求：Excel导出时需要支持智能化标签的冒泡显示逻辑。如果L5被标注了智能化但Excel只导出到L4级,那么L4这一行应该显示"智能化"标记. 这与前端清单中的AI标签冒泡逻辑一致.

## CodeGraph 分析结果

- `generateExcel` (DocumentService.java:1425) — 入口方法， 定义headers数组、列宽、调用writeExcelRow
- `writeExcelRow` (DocumentService.java:1558) — 写入每行数据, 当前只检查target自身的colIntelligent
- `getIntelligentChildren` (前端DataListTab.vue:2262) — 递归收集智能化子节点, 倅checkExpanded参数控制冒泡逻辑
- `DataEntry.colIntelligent` (DataEntry.java:147) — 字段已存在, 值为 `'1'` 或空字符串
- `DataEntry.children` (前端树结构) — 后端实体无此字段, 需要构建树结构来支持冒泡

## 关键问题分析

当前 `writeExcelRow` 方法只根据 `target.getColIntelligent()` 判断是否显示"是", 无法冒泡.

后端 `DataEntry` 实体没有 `children` 字段, 无法直接递归. 需要通过 `entryMap` 构建父子关系.

## 修改文件

- `src/main/java/com/superpower/modules/document/service/DocumentService.java`

## 实施步骤

### Step 1: 在 writeExcelRow 中增加冒泡逻辑

在 `writeExcelRow` 方法中, 当 `target.colIntelligent` 不是 `"1"` 时, 检查是否有智能化子节点:

```java
// 当前逻辑
Cell c5 = row.createCell(5);
c5.setCellValue("1".equals(target.getColIntelligent()) ? "是" : "");
c5.setCellStyle(centerStyle);

// 新增冒泡逻辑
// 当自身不是智能化时, 检查是否有智能化子节点
String intelligentValue = "1".equals(target.getColIntelligent()) ? "是" : "";
if ("".equals(intelligentValue) && hasIntelligentChildren(target, entryMap, entryIds)) {
    intelligentValue = "是";
}
c5.setCellValue(intelligentValue);
```

### Step 2: 新增 hasIntelligentChildren 辅助方法

类似前端 `getIntelligentChildren` 的递归方法, 但不需要检查展开状态(因为Excel导出不涉及展开/折叠):

```java
private boolean hasIntelligentChildren(DataEntry entry, Map<Long, DataEntry> entryMap, Set<Long> entryIds) {
    // 检查entry的所有子节点(entryMap中parentId等于entry.id的)
    for (DataEntry child : entryMap.values()) {
        if (child.getParentId() != null && child.getParentId().equals(entry.getId()) {
            if ("1".equals(child.getColIntelligent())) {
                return true;
            }
            // 递归检查子节点的子节点
            if (hasIntelligentChildren(child, entryMap, entryIds)) {
                return true;
            }
        }
    }
    return false;
}
```

**注意**: 这个方法遍历 entryMap.values() 效率较低(O(n)), 但Excel导出数据量通常不大, 可以接受.

### Step 3: 更新 VERSION.md
