<template>
  <div class="page">
    <div class="page-header">
      <h3>用户管理</h3>
      <el-button type="primary" size="small" @click="openNew">新增用户</el-button>
    </div>
    <el-table :data="users" border stripe size="small">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="nickname" label="姓名" width="120" />
      <el-table-column prop="roleName" label="角色" width="120" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.status === 1" type="success" size="small">启用</el-tag>
          <el-tag v-else type="danger" size="small">禁用</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="在线" width="70" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.online" type="success" size="small" effect="dark">在线</el-tag>
          <el-tag v-else type="info" size="small">离线</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最后登录" width="170">
        <template #default="{ row }">
          <span v-if="row.lastLoginAt">{{ formatTime(row.lastLoginAt) }}</span>
          <span v-else style="color:#999">从未登录</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="180">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="info" link @click="openLog(row)">操作日志</el-button>
          <el-button size="small" type="danger" link @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isNew ? '新增用户' : '编辑用户'" width="450px">
      <el-form :model="form" label-width="80px" size="small">
        <el-form-item label="用户名" v-if="isNew">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleId" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" v-if="!isNew">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="logVisible" :title="logUserName + ' 的操作日志'" width="70%" top="5vh">
      <el-table :data="logs" border stripe size="small" v-loading="logLoading" max-height="500">
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="action" label="操作" width="90">
          <template #default="{ row }">
            <el-tag :type="actionTagType(row.action)" size="small">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column prop="targetType" label="对象类型" width="90" />
        <el-table-column prop="targetId" label="对象ID" width="80" />
        <el-table-column prop="ip" label="IP" width="130" />
      </el-table>
      <div v-if="logs.length === 0 && !logLoading" style="text-align:center;padding:30px;color:#999;">暂无操作日志</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUsers, createUser, updateUser, deleteUser } from '../../api/user'
import { getRoles } from '../../api/role'
import { getUserLogs } from '../../api/operationLog'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref([])
const roles = ref([])
const dialogVisible = ref(false)
const isNew = ref(false)
const form = ref({ username: '', nickname: '', roleId: null, status: 1 })
const logVisible = ref(false)
const logUserName = ref('')
const logs = ref([])
const logLoading = ref(false)

function formatTime(val) {
  if (!val) return ''
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function actionTagType(action) {
  if (action === 'CREATE' || action === 'UPLOAD') return 'success'
  if (action === 'DELETE') return 'danger'
  if (action === 'UPDATE') return 'warning'
  if (action === 'LOGIN') return 'primary'
  return 'info'
}

async function loadUsers() {
  const res = await getUsers()
  users.value = res.data || []
}

async function loadRoles() {
  const res = await getRoles()
  roles.value = res.data || []
}

function openNew() {
  isNew.value = true
  form.value = { username: '', nickname: '', roleId: null, status: 1 }
  dialogVisible.value = true
}

function openEdit(row) {
  isNew.value = false
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleSave() {
  if (isNew.value) {
    await createUser(form.value)
    ElMessage.success('创建成功')
  } else {
    await updateUser(form.value.id, form.value)
    ElMessage.success('保存成功')
  }
  dialogVisible.value = false
  loadUsers()
}

async function handleDelete(id) {
  ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' }).then(async () => {
    await deleteUser(id)
    ElMessage.success('删除成功')
    loadUsers()
  }).catch(() => {})
}

async function openLog(row) {
  logUserName.value = row.nickname || row.username
  logs.value = []
  logVisible.value = true
  logLoading.value = true
  try {
    const res = await getUserLogs(row.id)
    logs.value = res.data || []
  } finally {
    logLoading.value = false
  }
}

onMounted(() => { loadUsers(); loadRoles() })
</script>

<style scoped>
.page { padding: 20px 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--si-border); }
.page-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: var(--si-text-primary); }
:deep(.el-table) { border-radius: var(--si-radius-md); }
:deep(.el-table th.el-table__cell) { background: var(--si-bg-hover); color: var(--si-text-secondary); font-weight: 600; }
</style>
