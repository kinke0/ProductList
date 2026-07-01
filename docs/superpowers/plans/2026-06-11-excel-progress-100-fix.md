# Excel 生成进度 100% 修复

## 目标
修复 Excel 生成完成后 `processedEntries != totalEntries` 导致进度条不到 100% 的问题。

## 问题分析
1. `generateExcel()` 循环内的 `updateGenRecordProgress(recordId, rowIdx-1, excelTotal)` 受节流机制影响，最后几次进度更新可能被跳过
2. `updateGenRecordSuccess()` 只设置 `status=completed`，没有将 `processedEntries` 设为 `totalEntries`
3. 导致前端进度条卡在非 100% 状态，虽然任务已完成

## 涉及文件
- `src/main/java/com/superpower/modules/document/service/DocumentService.java`

## 改动说明

### 1. `generateExcel()` 末尾添加最终进度调用
在 `return out.toByteArray()` 之前：
```java
if (recordId != null) {
    updateGenRecordProgress(recordId, excelTotal, excelTotal);
}
```
确保循环结束后进度标记为 100%。

### 2. `updateGenRecordSuccess()` 兜底设置 processedEntries
```java
record.setProcessedEntries(record.getTotalEntries());
```
即使进度回调有竞态，完成回调也会兜底设为 100%。

## 验证
- `mvn compile` 编译通过
- curl 测试 Excel 生成，完成后 `processedEntries == totalEntries`
