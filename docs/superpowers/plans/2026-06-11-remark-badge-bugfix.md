# 实施计划：备注标签 Bug 修复

## 问题 1：悬浮提示中未明确显示章节名

根因：模板中 `v-if="getRemarks(row).length > 1"` 条件导致只有多条备注时才显示名称。
修复：去掉条件判断，始终显示章节名称前缀。

## 问题 2：折叠 L3 后 L3 无备注标记

根因：`collectRemarks` 递归子节点时检查 `!expandedNodeIds.has(child.id)`，当 L3 折叠但 L4 仍在 expandedNodeIds 中时，递归到 L4 就停止了，无法继续到 L5 收集备注。
修复：增加 `deep` 参数，折叠节点递归后代时传入 `deep=true`，跳过中间节点的展开状态检查，始终深入到所有后代。

## 涉及文件

- `DataListTab.vue`：模板去掉 `v-if` 条件；`collectRemarks` 增加 `deep` 参数
- `docs/superpowers/plans/`：更新计划文件
- `VERSION.md`：更新变更说明
