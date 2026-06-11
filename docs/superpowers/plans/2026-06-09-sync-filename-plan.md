# 图片文件名同步改造实施计划

**日期：** 2026-06-09
**版本：** V1.0.5 beta

## 目标

重命名图片时同步修改磁盘物理文件名，使 storedName = filename（显示名），一处修改全局同步。同时在非常规操作中提供一次性迁移入口，将现有数据对齐。

## Part 1: ImageResourceService.update() 同步改物理文件

### 文件: `ImageResourceService.java`

**update() 方法改造：**
当 filename 变化时：
1. 保存旧 url、旧 storedName
2. 生成新 storedName = sanitize(filename去扩展名) + 原扩展名
3. 同目录同名检查
4. Files.move 改名物理文件
5. 更新 DB：storedName、path、url
6. 调用 syncImageNameInReferences(旧url, 新url, 旧name, 新name)

**syncImageNameInReferences 增强：**
- 参数从 (imageUrl, oldName, newName) 改为 (oldUrl, newUrl, oldName, newName)
- 新增 data-url 同步（旧URL → 新URL）
- 新增 <img src="..."> 同步
- 保留现有 data-filename、title、alt、image-name 同步

## Part 2: 非常规操作 — 一次性迁移

### 后端
- `MaintenanceService.java`: 新增文件名同步迁移方法
  - 扫描所有 ImageResource，比较 storedName 与 sanitize(filename)+扩展名
  - 逐个改名物理文件 + 更新 DB + 更新 DataEntry 引用
- `MaintenanceController.java`: 新增3个接口
- 使用独立的 migrationStatus 对象，不与现有版本隔离迁移冲突

### 前端
- `maintenance.js`: 新增3个API
- `SpecialOps.vue`: 新增"同步图片文件名" tab

## Part 3: 撤回 DocumentService 改动

恢复 processDescriptionWithImages 原始代码（不再需要 #filename 兜底）

## 涉及文件

| 文件 | 改动类型 |
|---|---|
| ImageResourceService.java | 修改 |
| DocumentService.java | 撤回 |
| MaintenanceService.java | 新增方法 |
| MaintenanceController.java | 新增接口 |
| maintenance.js | 新增API |
| SpecialOps.vue | 新增tab |
| VERSION.md | 追加记录 |
