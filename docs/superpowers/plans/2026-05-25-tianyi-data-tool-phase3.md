# 添翼产品清单管理工具 — 实现计划（Phase 3：文档生成）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**目标：** 实现高级用户的文档生成功能，支持将选中数据导出为招标参数（Word/Excel）和功能说明（Word/Excel）。

**架构：** 后端 Apache POI 生成文档，前端提供生成对话框和下载。

**Tech Stack:** Apache POI 5.2.x (Word/Excel)

---

### Task 1: Maven 添加 POI 依赖

**Files:**
- Modify: `pom.xml`

添加 Apache POI 依赖，支持 docx 和 xlsx 生成。

### Task 2: 文档生成 Service

**Files:**
- Create: `src/main/java/com/superpower/modules/document/service/DocumentService.java`
- Create: `src/main/java/com/superpower/modules/document/controller/DocumentController.java`
- Create: `src/main/java/com/superpower/modules/document/dto/DocGenerateRequest.java`

实现文档生成逻辑：
- 招标参数 Word：每条产品/系统生成一页，包含产品名称、招标参数说明、功能说明、版本划分等
- 招标参数 Excel：表格导出选中数据的核心字段
- 功能说明 Word：每条产品/系统一页，侧重功能说明字段
- 功能说明 Excel：表格导出功能说明相关字段

### Task 3: 前端文档生成对话框

**Files:**
- Modify: `frontend/src/views/dashboard/DataWorkbench.vue`
- Modify: `frontend/src/components/DataListTab.vue`

在数据工作台添加"生成文档"功能，弹窗选择文档类型和格式，勾选数据后生成下载。
