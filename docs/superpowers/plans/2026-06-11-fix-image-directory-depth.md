# 图片目录层级修复计划

## 问题
图床物理目录被创建到了超过 L3 级的深度。正确结构应为 `category/domain/product(L3)/file`（3级），
实际为 `category/domain/product(L4~L6)/file`（4~6级）。

根因：上传图片时 `imagePickerProduct` 取的是当前编辑行的 `col_产品系统`（可能是 L4/L5/L6/L7），
而不是向上追溯到 level=3 祖先的 `col_产品系统`。

另有 24 条 migration 图片用了 `1.1.3.*` 编码（data_entry 中是 `1.1.1.*`），3 条用了错误的业务域。

## 修复范围
- image_resource: v3/v4/v5 各 148 条 product 需修正 = 444 条
- data_entry: v3/v4/v5 中引用旧 URL 的条目需修正
- 物理文件: 移动到 L3 目录，清理旧目录

## 实施步骤

### Step 1: 修复代码（防止新增错误数据）

#### 1.1 前端 DataListTab.vue - imagePickerProduct
当前逻辑：向上查找 level=3 祖先，找不到时回退到 `editForm.colProductSystem`（可能是 L4+）。
修复：找不到时继续通过 parentRow 向上查找，最终回退到空字符串而非当前行值。

#### 1.2 后端 ImageResourceService.java - upload() 方法
增加 product 校验：查询 data_entry 中 level=3 的 col_产品系统 列表，
如果传入的 product 不在列表中，通过 parent_id 递归查找 L3 祖先。

#### 1.3 后端 DataEntryService.java - buildSubPath 的 product 来源
检查编辑功能说明时图片插入的 product 参数来源，确保始终传 L3 值。

### Step 2: SQL 脚本修复历史数据

映射策略：
- 121 条：通过 data_entry parent_id 递归链精确匹配
- 24 条 `1.1.3.*`：编码 1.1.3 → 1.1.1 替换后匹配 L3
- 3 条 `5.3.4.*`：直接映射到 `5.3.4 住院药房系统`，domain 修正

修复内容：
1. 更新 image_resource 的 product/url/path 字段
2. 移动物理文件到 L3 目录
3. 更新 data_entry 的 col_功能说明 中的图片 URL
4. 清理旧物理目录

### 涉及文件
- `frontend/src/components/DataListTab.vue` - imagePickerProduct 计算属性
- `src/.../image/service/ImageResourceService.java` - upload() 增加 L3 校验
- `src/.../data/service/DataEntryService.java` - buildSubPath 检查
- `db_changes/V1.0.7_fix_image_directory_depth.sql` - SQL 修复脚本
- `VERSION.md` - 变更说明
