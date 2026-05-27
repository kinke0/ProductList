# 左侧层级树独立化设计方案

**日期:** 2026-05-26
**项目:** superPowerTest

## 一、需求概述

左侧层级导航树展示"业务分类"和"业务域"二级结构，作为右侧数据清单的归类维度。分类体系本身具备版本管理机制，每个版本可有独立的分类/域结构。

数据来源：`docs/添翼产品清单.xlsx` 的"业务分类"和"业务域"列，作为初始版本的种子数据。

## 二、数据模型

### 2.1 新增表（带版本隔离）

**base_category:**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| version_id | BIGINT | 所属版本 |
| name | VARCHAR(200) | 业务分类名称 |
| sort_order | INTEGER | 排序 |

**base_domain:**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| version_id | BIGINT | 所属版本 |
| category_id | BIGINT | 所属分类ID |
| name | VARCHAR(200) | 业务域名称 |
| sort_order | INTEGER | 排序 |

### 2.2 数据来源

Excel 列：`业务分类`（第6列）、`业务域`（第7列）。导入逻辑：
- 提取唯一 `(业务分类, 业务域)` 组合
- 从名称中提取排序编号（如 "1. 数智底座-数据" → sort_order=1）
- 作为初始版本种子数据导入
- 创建新版本时，如需可从上一版本复制分类/域结构

## 三、后端变更

### 3.1 新增文件

| 文件 | 职责 |
|------|------|
| `BaseCategory.java` | 业务分类实体 |
| `BaseDomain.java` | 业务域实体 |
| `BaseCategoryRepository.java` | 按versionId查询 |
| `BaseDomainRepository.java` | 按versionId+categoryId查询 |
| `CategoryService.java` | 构建树、Excel导入、版本复制 |
| `CategoryController.java` | `GET /api/tree?versionId=X` |

### 3.2 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/tree?versionId=X` | 返回版本X的分类树 `[TreeNodeDTO]` |

### 3.3 TreeNodeDTO 复用

分类节点 level=1，域节点 level=2，id 使用 category/domain 的实际 ID。

## 四、前端变更

| 文件 | 变更 |
|------|------|
| `TreePanel.vue` | 恢复 `versionId` prop，调用 `GET /api/tree?versionId=X` |
| `DataWorkbench.vue` | 保持 `<TreePanel :version-id="selectedVersion.id">` 不变 |
| `api/data.js` | `getTree(versionId)` 改为调用新接口 |

## 五、影响范围

- 无破坏性变更：TreePanel 接口（prop/emit）不变
- 版本切换时树跟随更新
- 分类体系版本隔离，互不影响
