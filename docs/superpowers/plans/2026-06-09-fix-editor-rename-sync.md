# 修复：编辑器中图片改名后丢失 + 双向同步完善

## 问题
自定义编辑器中对图片改名后，图片立即丢失（显示不出来）。

## 根因分析
编辑器改名 `doSave()` 流程：
1. 修改 DOM 的 `data-filename`/`title`/`image-name`（✅）
2. **没有更新 `data-url` 和 `img src`**（❌ 旧 URL 仍在 DOM 中）
3. `editForm.colFeatureDesc = innerHTML`（此时 data-url/img src 还是旧 URL）
4. `updateImage(id, { filename })` → 后端改名物理文件 + 更新 image_resource URL + syncImageNameInReferences 更新 DataEntry
5. 用户点"保存" → `updateEntry(id, editForm)` → **用含旧 URL 的 editForm 覆盖 DataEntry**
6. 后端 `syncImageCardFilenames` 用旧 URL 查 image_resource → 查不到（URL 已改） → 不更新
7. 最终 DataEntry 存的是旧 URL → 物理 URL 已改 → 404 丢失

## 修复方案（4 个修改点）

### 1. DataListTab.vue `doSave()` — 前端改名同步 URL
- `editForm.colFeatureDesc` 的赋值移到 `updateImage` 的 `.then()` 里面
- `.then()` 中用返回的新 URL 更新 DOM 的 `data-url` 和 `img src`
- 确保 editForm 包含正确的新 URL

### 2. DataListTab.vue `onDialogChange()` — 关闭自动保存
- 编辑器关闭时自动调用保存，防止"改名了但没保存"导致数据不一致
- 后端 image_resource 和物理文件已改名，但 DataEntry 没保存 → 数据不一致

### 3. RequirementFormDialog.vue `doSave()` — 同 DataListTab 逻辑

### 4. 后端 `DataEntryService.syncImageCardFilenames()` — 兜底保障
- 改用 `data-id` 替代 URL 匹配（和 syncImageNameInReferences 一致）
- `findAllById(ids)` 替代 `findAll()`（性能优化 + 避免 SQLITE_BUSY）
- 替换 `data-url`/`img src`（URL 层面同步，不只是 filename）

### 涉及文件
- `frontend/src/components/DataListTab.vue`
- `frontend/src/components/RequirementFormDialog.vue`
- `src/main/java/com/superpower/modules/data/service/DataEntryService.java`

### 验证
1. 编辑器中图片改名 → 图片不丢失 → DataEntry 引用 URL 正确
2. 图床中图片改名 → 编辑器打开后图片正常
3. 编辑器改名 → 直接关闭（不点保存） → 重新打开 → 图片正常
