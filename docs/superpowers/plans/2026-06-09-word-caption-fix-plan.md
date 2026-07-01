# Word文档图片标题显示名修复计划

**日期：** 2026-06-09
**版本：** V1.0.5 beta

## 问题

用户重命名图片显示名后（如"数仓规划"→"数仓规划1-1"→"数仓规划"），Word文档中图片下方的标题仍显示旧的物理文件名，而非最新的显示名。

## 根因

`DocumentService.processDescriptionWithImages` 在处理图片URL时，将 `#显示名` 部分剥离，只传裸URL给 `insertSingleImage`/`insertImageGrid`，导致 `extractFilenameFromUrl` 从路径中提取物理文件名（storedName）而非显示名。

## 修复方案

修改 `processDescriptionWithImages`：
- 下载图片仍用去掉 `#` 的 URL
- 传给 `insertSingleImage`/`insertImageGrid` 的 URL 保留 `#显示名` 部分
- `downloadAndProcessImage` 和 `readLocalImage` 内部已有逻辑忽略 `#` 后内容

## 涉及文件

- `src/main/java/com/superpower/modules/document/service/DocumentService.java`
