# 图床 + 富文本编辑器 设计文档

## 概述

为系统新增图床功能模块，并替换功能说明字段为富文本编辑器，支持插入和管理图片。

## 一、图床模块

### 1.1 数据库

新增 `image_resource` 表：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | INTEGER PK AUTOINCREMENT | 主键 |
| filename | VARCHAR(255) | 原始文件名 |
| stored_name | VARCHAR(255) | 存储文件名(UUID.ext) |
| path | VARCHAR(500) | 相对路径 如 `数智底座-数据/大数据平台/xxx.jpg` |
| category | VARCHAR(200) | L1业务分类 |
| domain | VARCHAR(200) | L2业务域 |
| product | VARCHAR(500) | L3产品系统 |
| url | VARCHAR(500) | 访问URL `/api/images/file/{stored_name}` |
| size | INTEGER | 文件大小(字节) |
| mime_type | VARCHAR(50) | MIME类型 |
| uploaded_by | VARCHAR(100) | 上传者用户名 |
| created_at | DATETIME | 上传时间 |
| version_id | INTEGER | 所属版本ID |

### 1.2 后端API

#### 上传图片
- `POST /api/images/upload` — multipart/form-data
  - 参数: file(图片), category(L1), domain(L2), product(L3), versionId
  - 限制: 单张5MB, 支持 jpg/png/gif/webp
  - 返回: `{id, url, filename, size}`

#### 目录结构
- `GET /api/images/tree?versionId=X` — 获取L1→L2→L3树形目录（复用数据清单的分类数据，仅显示有图片的目录）

#### 图片列表
- `GET /api/images?category=&domain=&product=&versionId=` — 按目录查询图片列表

#### 删除图片
- `DELETE /api/images/{id}` — 删除图片(仅管理员)

#### 引用追踪
- `GET /api/images/{id}/references` — 查询图片被哪些功能说明引用（搜索 col_功能说明 中包含该图片URL的记录）

#### 图片访问
- `GET /api/images/file/{storedName}` — 静态资源访问图片文件

### 1.3 文件存储

- 存储目录: `./uploads/images/`
- 目录结构: `{category}/{domain}/{product}/{uuid}.{ext}`
- 配置: `app.image-storage-path=./uploads/images`

### 1.4 权限

- 图床菜单: 仅管理员(ADMIN)可见
- 上传接口: 所有登录用户可用
- 删除: 仅管理员
- 图片访问: 无需认证(静态资源)

## 二、图床管理页面

### 2.1 页面布局

- 路由: `/image-gallery`
- 菜单位置: 主菜单"产品清单"下方，新增"图床管理"菜单项
- 权限: 仅管理员可见

### 2.2 页面结构

- 左侧(30%): L1→L2→L3 三级目录树
  - 数据来源: 复用数据清单的 colBizCategory/colBizDomain/colProductSystem 层级
  - 仅显示包含图片的目录节点
  - 支持展开/折叠
- 右侧(70%): 图片列表区域
  - 网格缩略图展示(每张显示缩略图、文件名、大小、上传时间)
  - 操作: 上传、删除、复制URL、查看引用

### 2.3 上传交互

- 点击目录树节点后，右侧显示该目录下的图片
- 上传时自动归属当前选中的L1/L2/L3目录
- 支持拖拽上传和点击选择

## 三、富文本编辑器

### 3.1 组件选择

使用 `@vueup/vue-quill` (Vue3版Quill)，轻量(~130KB)，与项目风格一致。

### 3.2 替换范围

- DataListTab.vue 中编辑表单的"功能说明"字段
- 原组件: `<el-input type="textarea" :rows="10" />`
- 替换为: `<QuillEditor v-model="editForm.colFeatureDesc" />`

### 3.3 图片上传handler

- 点击编辑器工具栏的"插入图片"按钮时:
  1. 弹出图床选择器(复用图床管理页面的核心组件)
  2. 或选择从本地上传
  3. 上传后获得URL，插入 `<img src="url">` 到编辑器

### 3.4 数据兼容

- 存储格式: HTML
- 现有纯文本内容在Quill中作为纯段落渲染，完全兼容
- 读取时直接展示HTML内容

### 3.5 列表展示

- 列表中"功能说明"列仍显示纯文本摘要(去除HTML标签)
- 完整富文本只在编辑弹窗中展示

## 四、安全配置

### 4.1 Spring Security

- `/api/images/file/**` — 放行，无需认证
- `/api/images/upload` — 需登录
- `/api/images/**` 其他 — 需登录
- 删除操作 — 在Controller层校验管理员角色

### 4.2 文件校验

- 后端校验文件类型(jpg/png/gif/webp)
- 校验文件大小(5MB上限)
- 文件名UUID化，防止路径遍历

## 五、实施计划

1. 后端: ImageResource实体 + Repository + Service + Controller + Security配置
2. 前端: 图床管理页面(路由/菜单/组件)
3. 前端: 图床选择器组件(供富文本编辑器调用)
4. 前端: 集成vue-quill替换功能说明textarea
5. 前端: 列表中功能说明显示HTML摘要