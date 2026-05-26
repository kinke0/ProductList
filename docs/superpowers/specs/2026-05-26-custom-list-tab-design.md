# 自定义清单Tab功能设计方案

**日期:** 2026-05-26
**项目:** superPowerTest

## 一、需求概述

- 数据清单Tab右侧新增"添加清单"按钮，用户可自定义名称创建新Tab
- 自定义清单Tab拥有与数据清单完全相同的功能（过滤、编辑、删除、树形表格、版本划分等），初始数据为空
- 数据清单Tab的"生成文档"按钮替换为"插入待生成清单"，弹窗选择目标自定义清单后插入勾选的条目
- 数据持久化存储（后端数据库）

## 二、数据模型

### 2.1 新增表

**custom_tab:**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(100) | 清单名称（同版本下唯一） |
| version_id | BIGINT | 所属版本ID |
| user_id | BIGINT | 创建人ID |
| created_at | TIMESTAMP | 创建时间 |

**custom_tab_entry:**
| 字段 | 类型 | 说明 |
|------|------|------|
| custom_tab_id | BIGINT | 清单ID（外键） |
| entry_id | BIGINT | 条目ID（外键，关联 data_entry） |

联合主键: (custom_tab_id, entry_id)

### 2.2 数据关系

- 自定义清单与 DataEntry 是引用关系，编辑操作直接作用于原始 DataEntry 表
- 自定义清单按 version_id 隔离，切换版本后看到的清单列表不同
- 删除清单时级联删除关联条目

## 三、架构设计

### 3.1 后端新增

| 层 | 文件 | 职责 |
|---|---|---|
| Entity | `CustomTab.java` | 自定义清单实体 |
| Entity | `CustomTabEntry.java` | 清单-条目关联实体 |
| Repository | `CustomTabRepository.java` | 清单CRUD，按versionId查询 |
| Repository | `CustomTabEntryRepository.java` | 关联关系CRUD，批量插入/删除，按entryId查询所属清单 |
| Service | `CustomTabService.java` | 创建/删除/重命名清单，添加/移除条目，名称重复检测 |
| Controller | `CustomTabController.java` | REST API |

### 3.2 后端修改

| 文件 | 变更 |
|------|------|
| `DataEntryRepository.java` | `queryEntries` 方法新增可选参数 `customTabId`，有值时JOIN custom_tab_entry过滤 |
| `DataEntryService.java` | `query` 方法签名新增 `customTabId` 参数并透传至 repository |
| `DataEntryController.java` | `/query/{versionId}` 新增可选参数 `customTabId` |

### 3.3 REST API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/custom-tab/{versionId}` | 获取版本下所有自定义清单 |
| POST | `/api/custom-tab` | 创建自定义清单 `{name, versionId}` |
| DELETE | `/api/custom-tab/{id}` | 删除自定义清单 |
| POST | `/api/custom-tab/{id}/entries` | 批量添加条目 `{entryIds: [1,2,3]}` |
| DELETE | `/api/custom-tab/{id}/entries/{entryId}` | 移除单个条目 |

### 3.4 前端新增

| 文件 | 说明 |
|------|------|
| `frontend/src/api/customTab.js` | API 封装：getCustomTabs, createCustomTab, deleteCustomTab, addEntriesToTab, removeEntryFromTab |

### 3.5 前端修改

| 文件 | 变更 |
|------|------|
| `api/data.js` | `queryEntries` 新增可选参数 `customTabId` |
| `DataListTab.vue` | "生成文档"按钮改为"插入待生成清单"；emit 改为 `insertToList(entryIds)`；接收可选 prop `customTabId`，非空时查询附带 `customTabId` |
| `DataWorkbench.vue` | Tab栏动态渲染（统计视图 + 数据清单 + N个自定义清单）；"添加清单"按钮 + 创建对话框；自定义Tab渲染DataListTab传入`customTabId`和`versionId`；处理`insertToList`弹出选择目标清单对话框 |

## 四、数据流

### 4.1 创建自定义清单
```
用户点击"添加清单" → 弹窗输入名称 → POST /api/custom-tab → 刷新Tab列表 → 切换到新Tab
```

### 4.2 插入条目到自定义清单
```
数据清单Tab勾选条目 → 点击"插入待生成清单" → 弹窗选择目标清单 → POST /api/custom-tab/{id}/entries → toast成功
```

### 4.3 自定义清单Tab查询
```
切换到自定义清单Tab → DataListTab.queryEntries(customTabId=X) → 后端JOIN过滤 → 返回该清单下的条目树
```

## 五、错误处理

| 场景 | 处理方式 |
|------|----------|
| 创建清单时名称空 | 前端校验，toast"请输入清单名称" |
| 名称重复（同版本） | 后端检测返回400，前端toast"清单名称已存在" |
| 插入时未勾选条目 | 前端校验，toast"请先勾选条目" |
| 插入时无可选清单 | toast"请先创建自定义清单" |
| 删除清单 | ElMessageBox.confirm二次确认 |
| 后端异常 | 统一异常拦截，ElMessage.error |

## 六、测试策略

- **后端单元测试**：`CustomTabServiceTest` 覆盖创建/删除清单、添加/移除条目、版本隔离、名称重复检测
- **后端集成**：`DataEntryServiceTest` 增加带 `customTabId` 的查询测试
- **前端构建**：`npm run build` 无报错
- **人工验收**：创建清单 → 插入条目 → 在自定义清单中编辑/过滤/删除 → 生成文档

## 七、影响范围

- 无破坏性变更
- 数据清单Tab原有"生成文档"按钮移除（生成文档仍在DataWorkbench的文档生成对话框使用）
- 自定义清单中编辑条目会影响原数据清单中的同名条目（因为操作的是同一DataEntry）
