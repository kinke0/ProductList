# 数据审批流实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为信息清单数据添加4状态审批流（待提交→待审核→审核通过/驳回），基于角色控制编辑权限，记录流转日志。

**Architecture:** 后端新增 ApprovalLog 实体 + 审批接口，User 实体增加 role 字段，DataEntry 复用 colStatus 字段。前端在列表操作列和编辑表单中添加审批按钮。

**Tech Stack:** Spring Boot 3.2 / JPA / SQLite / Vue 3 / Element Plus

---

### Task 1: User 实体添加 role 字段

**Files:**
- Modify: `src/main/java/com/superpower/modules/user/entity/User.java`
- Modify: `src/main/java/com/superpower/modules/user/dto/UserDTO.java`

- [ ] **Step 1: User 实体添加 role 字段**

在 `User.java` 中添加：
```java
@Column(length = 20)
private String role = "editor";
```

- [ ] **Step 2: UserDTO 添加 role 字段**

在 `UserDTO.java` 中添加 `role` 字段及 getter/setter。

- [ ] **Step 3: 编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add -A && git commit -m "feat: User实体添加role字段(editor/reviewer/admin)"
```

---

### Task 2: ApprovalLog 实体、Repository、Service

**Files:**
- Create: `src/main/java/com/superpower/modules/approval/entity/ApprovalLog.java`
- Create: `src/main/java/com/superpower/modules/approval/repository/ApprovalLogRepository.java`
- Create: `src/main/java/com/superpower/modules/approval/service/ApprovalService.java`

- [ ] **Step 1: 创建 ApprovalLog 实体**

```java
package com.superpower.modules.approval.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "approval_log")
public class ApprovalLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Column(name = "from_status", length = 20)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 20)
    private String toStatus;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "operator_name", length = 100)
    private String operatorName;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

- [ ] **Step 2: 创建 ApprovalLogRepository**

```java
package com.superpower.modules.approval.repository;

import com.superpower.modules.approval.entity.ApprovalLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApprovalLogRepository extends JpaRepository<ApprovalLog, Long> {
    List<ApprovalLog> findByEntryIdOrderByCreatedAtDesc(Long entryId);
}
```

- [ ] **Step 3: 创建 ApprovalService**

```java
package com.superpower.modules.approval.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.approval.entity.ApprovalLog;
import com.superpower.modules.approval.repository.ApprovalLogRepository;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import com.superpower.modules.version.service.VersionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ApprovalService {

    private static final String ST_PENDING = "待提交";
    private static final String ST_REVIEW = "待审核";
    private static final String ST_APPROVED = "审核通过";
    private static final String ST_REJECTED = "驳回";

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
        ST_PENDING, Set.of(ST_REVIEW),
        ST_REVIEW, Set.of(ST_APPROVED, ST_REJECTED),
        ST_APPROVED, Set.of(ST_REJECTED),
        ST_REJECTED, Set.of(ST_REVIEW)
    );

    private static final Map<String, Set<String>> ACTION_ROLES = Map.of(
        "submit", Set.of("editor", "admin"),
        "approve", Set.of("reviewer", "admin"),
        "reject", Set.of("reviewer", "admin")
    );

    private static final Map<String, String> ACTION_TARGET = Map.of(
        "submit", ST_REVIEW,
        "approve", ST_APPROVED,
        "reject", ST_REJECTED
    );

    private final DataEntryRepository entryRepository;
    private final ApprovalLogRepository logRepository;
    private final VersionService versionService;

    public ApprovalService(DataEntryRepository entryRepository,
                           ApprovalLogRepository logRepository,
                           VersionService versionService) {
        this.entryRepository = entryRepository;
        this.logRepository = logRepository;
        this.versionService = versionService;
    }

    @Transactional
    public void approve(Long entryId, String action, String userRole, Long userId, String userName, String comment) {
        if (!ACTION_ROLES.getOrDefault(action, Set.of()).contains(userRole)) {
            throw new BusinessException("无权执行此操作");
        }

        DataEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new BusinessException("数据不存在"));

        if (!versionService.isEditable(entry.getVersionId())) {
            throw new BusinessException("仅编辑中版本可变更审批状态");
        }

        String currentStatus = entry.getColStatus();
        if (currentStatus == null || currentStatus.isEmpty()) {
            currentStatus = ST_PENDING;
        }

        String targetStatus = ACTION_TARGET.get(action);
        Set<String> allowed = TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(targetStatus)) {
            throw new BusinessException("当前状态不允许此操作：" + currentStatus + " → " + targetStatus);
        }

        entry.setColStatus(targetStatus);
        entryRepository.save(entry);

        ApprovalLog log = new ApprovalLog();
        log.setEntryId(entryId);
        log.setFromStatus(currentStatus);
        log.setToStatus(targetStatus);
        log.setAction(action);
        log.setOperatorId(userId);
        log.setOperatorName(userName);
        log.setComment(comment);
        logRepository.save(log);
    }

    public List<ApprovalLog> getLogs(Long entryId) {
        return logRepository.findByEntryIdOrderByCreatedAtDesc(entryId);
    }

    public static boolean canEdit(String status, String role) {
        if ("admin".equals(role)) return true;
        if ("editor".equals(role)) return ST_PENDING.equals(status) || ST_REJECTED.equals(status);
        return false;
    }
}
```

- [ ] **Step 4: 编译验证**

Run: `mvn compile -q`

- [ ] **Step 5: 提交**

```bash
git add -A && git commit -m "feat: ApprovalLog实体、Repository、ApprovalService"
```

---

### Task 3: 审批 Controller 接口

**Files:**
- Create: `src/main/java/com/superpower/modules/approval/controller/ApprovalController.java`

- [ ] **Step 1: 创建 ApprovalController**

```java
package com.superpower.modules.approval.controller;

import com.superpower.common.Result;
import com.superpower.modules.approval.entity.ApprovalLog;
import com.superpower.modules.approval.service.ApprovalService;
import com.superpower.modules.user.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/approval")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping("/{entryId}")
    public Result<Void> approve(@PathVariable Long entryId,
                                @RequestBody Map<String, String> body,
                                Authentication auth) {
        String action = body.get("action");
        String comment = body.get("comment");
        User user = (User) auth.getPrincipal();
        approvalService.approve(entryId, action, user.getRole(), user.getId(), user.getUsername(), comment);
        return Result.success();
    }

    @GetMapping("/{entryId}/logs")
    public Result<List<ApprovalLog>> getLogs(@PathVariable Long entryId) {
        return Result.success(approvalService.getLogs(entryId));
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -q`

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "feat: ApprovalController审批接口"
```

---

### Task 4: 前端审批 API 和用户角色传递

**Files:**
- Create: `frontend/src/api/approval.js`
- Modify: `frontend/src/components/DataListTab.vue` — 引入用户角色、审批按钮
- Modify: `frontend/src/views/dashboard/DataWorkbench.vue` — 传递用户角色

- [ ] **Step 1: 创建审批 API**

```javascript
// frontend/src/api/approval.js
import request from '../utils/request'

export function approveEntry(entryId, action, comment) {
  return request.post(`/approval/${entryId}`, { action, comment })
}

export function getApprovalLogs(entryId) {
  return request.get(`/approval/${entryId}/logs`)
}
```

- [ ] **Step 2: DataListTab 接收 userRole prop**

在 DataListTab props 中添加：
```javascript
userRole: { type: String, default: 'editor' }
```

DataWorkbench 中传递 userRole：
```html
<DataListTab ... :user-role="currentUserRole" />
```

DataWorkbench 中从 auth store 获取用户角色：
```javascript
import { useAuthStore } from '../stores/auth'
const authStore = useAuthStore()
const currentUserRole = computed(() => authStore.user?.role || 'editor')
```

- [ ] **Step 3: 编译验证**

Run: `cd frontend && npx vite build --logLevel silent`

- [ ] **Step 4: 提交**

```bash
git add -A && git commit -m "feat: 前端审批API和用户角色传递"
```

---

### Task 5: 列表操作列添加审批按钮

**Files:**
- Modify: `frontend/src/components/DataListTab.vue` — 操作列添加审批按钮

- [ ] **Step 1: 添加审批按钮到操作列**

在操作列（vcol-ops div）中，根据状态和角色动态显示提交/通过/驳回按钮：

```html
<template v-if="!row._isSeparator">
  <span v-if="canSubmit(row)" class="op-btn op-add" @click="handleApprove(row, 'submit')">提交</span>
  <span v-if="canApprove(row)" class="op-btn op-add" @click="handleApprove(row, 'approve')">通过</span>
  <span v-if="canReject(row)" class="op-btn op-del" @click="handleReject(row)">驳回</span>
  <span v-if="canEditRow(row)" class="op-btn op-edit" @click="editRow(row)">编辑</span>
  <span v-if="canEditRow(row) && !props.customTabId" class="op-btn op-add" @click="addChildRow(row)">添加</span>
  <span v-if="props.customTabId && canEditRow(row)" class="op-btn op-del" @click="emit('removeFromList', collectSelfAndDescendants(row))">移除</span>
  <span v-else-if="canEditRow(row)" class="op-btn op-del" @click="deleteRow(row)">删除</span>
</template>
```

添加辅助函数和审批处理函数：

```javascript
function canEditRow(row) {
  return ApprovalService_canEdit(row.colStatus, props.userRole)
}
function canSubmit(row) {
  const s = row.colStatus
  return ['editor', 'admin'].includes(props.userRole) && (s === '待提交' || s === '驳回')
}
function canApprove(row) {
  return ['reviewer', 'admin'].includes(props.userRole) && row.colStatus === '待审核'
}
function canReject(row) {
  return ['reviewer', 'admin'].includes(props.userRole) && (row.colStatus === '待审核' || row.colStatus === '审核通过')
}

async function handleApprove(row, action) {
  await approveEntry(row.id, action)
  ElMessage.success('操作成功')
  handleQuery(true)
}
async function handleReject(row) {
  const { value } = await ElMessageBox.prompt('请输入驳回原因（非必填）', '驳回', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPlaceholder: '请输入驳回原因，可不填',
    inputValidator: () => true
  }).catch(() => ({ value: null }))
  if (value === null) return
  await approveEntry(row.id, 'reject', value || '')
  ElMessage.success('已驳回')
  handleQuery(true)
}
```

- [ ] **Step 2: 修改状态标签颜色**

将 statusTagType 函数改为基于审批状态：

```javascript
function statusTagType(status) {
  switch (status) {
    case '待提交': return 'primary'
    case '待审核': return 'warning'
    case '审核通过': return 'success'
    case '驳回': return 'danger'
    default: return 'info'
  }
}
```

- [ ] **Step 3: 编译验证**

- [ ] **Step 4: 提交**

```bash
git add -A && git commit -m "feat: 列表操作列添加审批按钮和状态颜色"
```

---

### Task 6: 编辑表单添加审批按钮

**Files:**
- Modify: `frontend/src/components/DataListTab.vue` — 编辑弹窗底部添加审批按钮

- [ ] **Step 1: 在编辑弹窗 footer 添加审批按钮**

在 `el-dialog` 的 footer slot 中，在"取消"和"保存"按钮之前添加审批按钮：

```html
<template #footer>
  <span v-if="!isNew && canSubmit(editingRow)" style="margin-right: auto;">
    <el-button type="primary" @click="handleApprove(editingRow, 'submit')">提交</el-button>
  </span>
  <span v-if="!isNew && canApprove(editingRow)" style="margin-right: auto;">
    <el-button type="success" @click="handleApprove(editingRow, 'approve')">通过</el-button>
    <el-button type="danger" @click="handleReject(editingRow)">驳回</el-button>
  </span>
  <span v-if="!isNew && canReject(editingRow) && editingRow?.colStatus === '审核通过'" style="margin-right: auto;">
    <el-button type="danger" @click="handleReject(editingRow)">驳回</el-button>
  </span>
  <el-button @click="showEditDialog = false">取消</el-button>
  <el-button type="primary" @click="saveEdit">保存</el-button>
</template>
```

注意：`editingRow` 需要在编辑时记录当前行引用。

- [ ] **Step 2: 编译验证**

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "feat: 编辑表单添加审批按钮"
```

---

### Task 7: 新建数据默认状态为"待提交"

**Files:**
- Modify: `frontend/src/components/DataListTab.vue` — 新建时设置默认状态
- Modify: `src/main/java/com/superpower/modules/data/entity/DataEntry.java` — 默认值

- [ ] **Step 1: DataEntry 实体 colStatus 默认值**

```java
@Column(name = "col_状态", length = 100)
private String colStatus = "待提交";
```

- [ ] **Step 2: 前端新建表单默认状态**

在 `initEditForm` 或 `openNewDialog` 中设置 `editForm.colStatus = '待提交'`。

- [ ] **Step 3: 编译验证**

- [ ] **Step 4: 提交**

```bash
git add -A && git commit -m "feat: 新建数据默认状态为待提交"
```

---

### Task 8: 集成测试与最终验证

- [ ] **Step 1: 重启应用**

```bash
lsof -ti:8080 | xargs kill -9 2>/dev/null
mvn compile -q && mvn spring-boot:run -q &
```

- [ ] **Step 2: 验证场景**

1. 以编辑员登录 → 新建数据 → 状态默认为"待提交" → 可编辑 → 点击提交 → 状态变为"待审核" → 不可编辑
2. 以审核员登录 → 看到"待审核"数据 → 点击通过 → 状态变为"审核通过" → 点击驳回 → 状态变为"驳回"
3. 以编辑员登录 → 看到"驳回"数据 → 可编辑 → 重新提交
4. 查看审批日志接口返回正确

- [ ] **Step 3: 备份提交**

```bash
git add -A && git commit -m "feat: 审批流功能完成"
```
