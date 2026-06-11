# SQL脚本执行页签实施计划

**日期：** 2026-06-10
**版本：** V1.0.6 beta

## 目标

在"非常规操作"页面新增"SQL脚本执行"页签，支持管理员手动输入SQL语句或上传.sql脚本文件并在SQLite数据库上执行，执行结果实时反馈。

## 功能要点

1. **SQL文本编辑**：提供多行文本编辑器，支持手动输入/粘贴SQL语句
2. **文件上传**：支持上传 .sql 脚本文件，读取内容到编辑器预览
3. **多语句执行**：按分号分隔多条SQL语句，逐条执行
4. **结果反馈**：显示每条语句的执行结果（成功/失败、影响行数、耗时）
5. **操作日志**：执行操作记录到 operation_log 表

## 涉及文件

| 文件 | 改动类型 |
|---|---|
| `MaintenanceController.java` | 新增接口 |
| `MaintenanceService.java` | 新增 SQL 执行方法 |
| `frontend/src/api/maintenance.js` | 新增 API 函数 |
| `frontend/src/views/system/SpecialOps.vue` | 新增页签 |
| `VERSION.md` | 追加变更记录 |

## 分步实施

### Step 1: 后端 - MaintenanceService 新增 SQL 执行方法

注入 `javax.sql.DataSource`，新增 `executeSql(String sql)` 方法：
- 按分号分隔多条SQL语句
- 逐条通过 JDBC Statement 执行
- 捕获每条语句的执行结果（成功/失败、消息、影响行数、耗时）
- 返回 `List<SqlExecutionResult>` DTO

需要在 `com.superpower.modules.system.dto` 下新建 `SqlExecutionResult.java` DTO 类。

### Step 2: 后端 - MaintenanceController 新增接口

新增 `POST /api/maintenance/execute-sql`：
- 接收 `{ "sql": "..." }` JSON body
- 调用 `maintenanceService.executeSql(sql)`
- 记录操作日志（module="非常规操作"，description="执行SQL脚本"）
- 返回执行结果列表

### Step 3: 前端 - maintenance.js 新增 API

新增 `executeSql(sql)` 函数，调用 `POST /maintenance/execute-sql`。

### Step 4: 前端 - SpecialOps.vue 新增页签

在现有4个 `el-tab-pane` 后新增第5个页签 "SQL脚本执行"：
- 文件上传按钮（accept=".sql"，读取文件内容填充到编辑器）
- SQL文本编辑器（`el-input` type="textarea"，等宽字体，多行）
- 执行按钮（二次确认弹窗后提交）
- 结果展示区域：表格列出每条语句的执行结果
- 重置按钮清空编辑器和结果
