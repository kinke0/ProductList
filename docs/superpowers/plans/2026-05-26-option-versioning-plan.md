# 选项数据版本化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development

**Goal:** 将 sys_option（解决方案/应用角色/功能状态）版本化，创建新版本时镜像复制，已发布版本锁定不可修改。

**Architecture:** DataOption 实体新增 versionId，所有查询/创建追加版本过滤，DataVersionService.createVersion 复制选项数据。

**Tech Stack:** Spring Boot 3, Spring Data JPA, Vue 3

---

### Task 1: DataOption 实体和 Repository 版本化

**Files:**
- Modify: `src/main/java/com/superpower/modules/option/entity/DataOption.java`
- Modify: `src/main/java/com/superpower/modules/option/repository/DataOptionRepository.java`

**Step 1:** 读取现有文件，了解当前结构

**Step 2:** 修改 DataOption 实体，添加 versionId:
```java
@Column(name = "version_id")
private Long versionId;
```

**Step 3:** 修改 DataOptionRepository，查询方法加 versionId:
```java
List<DataOption> findByTypeAndVersionIdOrderBySortOrder(String type, Long versionId);
List<DataOption> findByVersionId(Long versionId);
void deleteByVersionId(Long versionId);
```

**Step 4:** Commit

---

### Task 2: DataOptionService 版本化

**Files:**
- Modify: `src/main/java/com/superpower/modules/option/service/DataOptionService.java`

读取文件后修改。新建一个版本时复制选项的方法 `copyOptions`，以及在 create/update 时加读保护。

**Step 1:** 注入 DataVersionRepository，添加 `ensureVersionEditable`

**Step 2:** 修改所有方法带 versionId 参数

**Step 3:** 添加 `copyOptions(sourceVersionId, targetVersionId)` 方法

**Step 4:** Commit

---

### Task 3: DataOptionController 版本化

**Files:**
- Modify: `src/main/java/com/superpower/modules/option/controller/DataOptionController.java`

**Step 1:** 所有接口 path 改为 `/options/{versionId}/{type}`

**Step 2:** Commit

---

### Task 4: DataVersionService 复制选项

**Files:**
- Modify: `src/main/java/com/superpower/modules/version/service/DataVersionService.java`

**Step 1:** 注入 DataOptionService，在 createVersion 末尾调用 `copyOptions`

**Step 2:** Commit

---

### Task 5: 前端 Option API 和页面适配

**Files:**
- Modify: `frontend/src/api/option.js`
- Modify: `frontend/src/views/system/OptionManage.vue`
- Modify: `frontend/src/views/system/BaseDataManage.vue` (loadVersion 提供 versionId)

**Step 1:** 修改 frontend API，`getOptions(versionId, type)` 等

**Step 2:** OptionManage.vue 从 route meta 或版本信息获取 versionId

**Step 3:** BaseDataManage.vue 提供 versionId 给 OptionManage（通过 store 或 props）

---

### Task 6: 种子数据迁移和编译验证

**Steps:**
1. 为现有 sys_option 记录设置 version_id = 1
2. mvn compile + npm run build
3. 重启验证
