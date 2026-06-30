# Bug修复：升级/降级节点后子节点层级标签未更新

## 目标
修复 `levelUp` 和 `levelDown` 操作后子节点的 `level` 字段未级联更新的问题。当前升级L4→L3后，子节点L5仍显示"功能"而非"模块"。

## 根因
后端 `DataEntryService.levelUp()` 和 `levelDown()` 方法只修改目标节点的 `level` 字段，没有递归更新所有后代节点的 `level`。前端 `levelLabel()` 函数根据 `level` 数值映射标签（3→产品, 4→模块, 5→功能），因此子节点 `level` 不变则标签不变。

## 涉及文件
- `src/main/java/com/superpower/modules/data/service/DataEntryService.java` — 修改 `levelUp` 和 `levelDown` 方法，增加子节点 level 级联更新逻辑
- `VERSION.md` — 在 V1.0.12 版本变更说明中追加修复说明

## 分步实施

### Step 1: 修改 `levelUp` 方法
在 `entryRepository.save(entry)` 之后，调用 `collectDescendantsList` 收集所有后代节点，批量将每个后代节点的 `level` 减 1 并保存。

```java
// 在 levelUp 方法中，syncImagesProductIdForBranch 之前插入：
List<DataEntry> descendants = collectDescendantsList(entry.getVersionId(), entry.getId());
for (DataEntry desc : descendants) {
    desc.setLevel(desc.getLevel() - 1);
    entryRepository.save(desc);
}
```

### Step 2: 修改 `levelDown` 方法
同理，在 `entryRepository.save(entry)` 之后，将每个后代节点的 `level` 加 1。

```java
// 在 levelDown 方法中，syncImagesProductIdForBranch 之前插入：
List<DataEntry> descendants = collectDescendantsList(entry.getVersionId(), entry.getId());
for (DataEntry desc : descendants) {
    desc.setLevel(desc.getLevel() + 1);
    entryRepository.save(desc);
}
```

### Step 3: 更新 VERSION.md
在 V1.0.12 的"产品清单"部分追加：
- 修复升级/降级节点后子节点层级标签未同步更新的问题：升级（L4→L3）或降级节点时，级联调整所有后代节点的 level 字段，确保标签显示正确

### Step 4: 验证
- 后端编译 `mvn compile`
- 前端构建 `npm run build`
- 重启前后端服务
- curl 验证端口和接口正常
