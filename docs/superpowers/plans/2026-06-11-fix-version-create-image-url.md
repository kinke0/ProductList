# 创建版本功能修复 — 图片URL替换 + data-id映射 + BaseProduct双重复制消除

## 目标
1. 修复创建版本时图片URL替换链断裂问题（只替换sourceVersionId，遗漏更早版本号的URL）
2. 修复创建版本时HTML中`data-id`未映射到新image_resource ID的问题
3. 消除BaseProduct被CategoryService和ProductService双重复制的问题
4. 修复现有v6数据中残留的旧版本URL

## 涉及文件

| 文件 | 改动点 |
|------|--------|
| `DataVersionService.java` | 步骤5建立imgIdMap；步骤7改为替换所有旧版本号URL + data-id映射 + productId映射 |
| `CategoryService.java` | copyFromVersion不再复制BaseProduct |
| `ProductService.java` | copyFromVersion返回productIdMap |
| `db_changes/V1.0.7_fix_v6_image_url.sql` | 修复v6残留旧URL数据 |

## 分步实施

### 步骤 1：CategoryService.copyFromVersion — 不再复制BaseProduct
当前 CategoryService 在复制 BaseDomain 后会遍历并复制 BaseProduct（第266-274行），但 ProductService.copyFromVersion 也会复制 BaseProduct（含正确的 l1Id/l2Id 映射）。删除 CategoryService 中的 BaseProduct 复制逻辑。

### 步骤 2：ProductService.copyFromVersion — 返回productIdMap
当前 ProductService 复制 BaseProduct 时没有记录旧→新 ID 映射。新增 productIdMap 并返回。

### 步骤 3：DataVersionService — 步骤5建立imgIdMap
在步骤5复制 image_resource 时，建立 `oldImgId → newImgId` 的映射（HashMap<Long, Long>），供步骤7使用。

### 步骤 4：DataVersionService — 步骤7全面修复
#### 4a：URL替换改为替换所有旧版本号
- 查询所有版本ID列表（除targetVersionId外）
- 对每个旧版本号做 URL 前缀替换：`/api/images/file/{oldVersionId}/` → `/api/images/file/{targetVersionId}/`

#### 4b：data-id映射
- 在步骤7中，对 col_功能说明 中的 `data-id="xxx"` 做替换，将旧 image_resource ID 替换为新 ID
- 使用正则替换：`data-id="旧ID"` → `data-id="新ID"`

#### 4c：productId映射
- 从 ProductService 返回的 productIdMap 中获取映射
- 遍历新 DataEntry 时更新 product_id 字段

### 步骤 5：SQL脚本修复现有v6数据
- 修复 v6 中残留的 `/api/images/file/3/` → `/api/images/file/6/`
- 修复 v6 中残留的 data-id 映射

## 验证步骤
1. mvn compile 后端编译无错误
2. 重启后端服务
3. 创建新版本验证图片URL正确替换
4. 检查新版本data-id映射是否正确
