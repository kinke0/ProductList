# 修复 findSiblings 对 L3 节点排序逻辑导致"无法改变显示顺序"的bug

## 目标
修复"1.3.2 公立医院绩效考核系统"等L3节点无法改变显示顺序的问题。

## 根因
1. **数据不一致**：batchUpdateCategory 将8个条目的 domainId 从135改为134，但 parentId 未更新（仍指向6282，domainId=135的L2分隔行）
2. **findSiblings 逻辑**：L3条目用 domainId 查找兄弟，混合了不同 parentId 的条目，导致排序混乱
3. **前端渲染**：按 parentId 分组，6362 在 parentId=6281 下唯一，视觉上是第一个也是唯一一个

## 涉及文件
- `src/main/java/com/superpower/modules/data/service/DataEntryService.java` — findSiblings 方法（line 1289-1314）
- `VERSION.md` — 变更说明

## 实施步骤

### 1. 修改 findSiblings 方法

当前逻辑（line 1289-1314）：
```java
private List<DataEntry> findSiblings(DataEntry entry) {
    List<DataEntry> siblings;
    if (entry.getLevel() != null && entry.getLevel() == 3) {
        if (entry.getDomainId() != null) {
            siblings = entryRepository.findL3ByDomainId(entry.getVersionId(), entry.getDomainId());
        } else {
            siblings = entryRepository.findRootEntries(entry.getVersionId());
        }
    } else {
        Long parentId = entry.getParentId();
        if (parentId != null) {
            return entryRepository.findByVersionIdAndParentIdOrderBySortOrder(entry.getVersionId(), parentId);
        }
        if (entry.getDomainId() != null) {
            siblings = entryRepository.findRootEntriesByDomainId(entry.getVersionId(), entry.getDomainId());
        } else {
            siblings = entryRepository.findRootEntries(entry.getVersionId());
        }
    }
    siblings.sort(Comparator
            .comparing((DataEntry e) -> e.getParentId(), Comparator.nullsLast(Long::compareTo))
            .thenComparingInt(e -> e.getSortOrder() != null ? e.getSortOrder() : 0));
    return siblings;
}
```

改为：
```java
private List<DataEntry> findSiblings(DataEntry entry) {
    if (entry.getLevel() != null && entry.getLevel() >= 3) {
        // L3+ 条目：优先基于 parentId 找同一父节点下的兄弟（与前端渲染一致）
        Long parentId = entry.getParentId();
        if (parentId != null) {
            return entryRepository.findByVersionIdAndParentIdOrderBySortOrder(entry.getVersionId(), parentId);
        }
        // parentId 为 null 的 L3 条目：基于 domainId fallback
        if (entry.getDomainId() != null) {
            List<DataEntry> siblings = entryRepository.findL3ByDomainId(entry.getVersionId(), entry.getDomainId());
            siblings.sort(Comparator.comparingInt(e -> e.getSortOrder() != null ? e.getSortOrder() : 0));
            return siblings;
        }
        return entryRepository.findRootEntries(entry.getVersionId());
    }
    // L4+ 非 L3 条目：基于 parentId 查找（原逻辑保留）
    Long parentId = entry.getParentId();
    if (parentId != null) {
        return entryRepository.findByVersionIdAndParentIdOrderBySortOrder(entry.getVersionId(), parentId);
    }
    // parentId 为 null 的 fallback：基于 domainId
    if (entry.getDomainId() != null) {
        List<DataEntry> siblings = entryRepository.findRootEntriesByDomainId(entry.getVersionId(), entry.getDomainId());
        siblings.sort(Comparator.comparingInt(e -> e.getSortOrder() != null ? e.getSortOrder() : 0));
        return siblings;
    }
    return entryRepository.findRootEntries(entry.getVersionId());
}
```

关键改动：
- L3 条目从 `findL3ByDomainId` 改为优先用 `findByVersionIdAndParentIdOrderBySortOrder`
- 去掉 `siblings.sort(...)` 中混合 parentId 的排序（不再需要，因为同一 parentId 下已经有 sortOrder 排序）
- parentId 为 null 时才 fallback 到 domainId

### 2. 修复数据库中 parentId/domainId 不一致的条目

使用 SQL 直接修复 SQLite 数据库中的不一致数据：
```sql
-- 查找不一致的条目
SELECT e.id, e.parent_id, e.domain_id, p.domain_id as parent_domain_id
FROM data_entry e
JOIN data_entry p ON e.parent_id = p.id
WHERE e.level = 3 AND e.domain_id IS NOT NULL AND e.parent_id IS NOT NULL
AND e.domain_id != p.domain_id;

-- 修复：将 parentId 更新为正确的 L2 分隔行
UPDATE data_entry e
SET parent_id = (
    SELECT l2.id FROM data_entry l2
    WHERE l2.version_id = e.version_id AND l2.level = 2 AND l2.domain_id = e.domain_id
    LIMIT 1
)
WHERE e.level = 3 AND e.domain_id IS NOT NULL
AND e.parent_id IS NOT NULL
AND e.domain_id != (SELECT p.domain_id FROM data_entry p WHERE p.id = e.parent_id);
```

### 3. 更新 VERSION.md

在当前版本变更说明中追加：
- 修复L3条目上下移操作提示"已经是第一个/最后一个"的bug：findSiblings改为基于parentId查找兄弟节点，与前端树结构渲染一致

## 验证
1. 后端 mvn compile ✅
2. 前端 npm run build ✅
3. 服务重启 + curl 验证端口可达
4. curl 调用关键业务接口检查无500错误
5. 测试 id=6362 的 moveUp/moveDown → 正常响应
6. 检查数据修复后 parentId 与 domainId 一致
