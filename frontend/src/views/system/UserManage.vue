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
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openEdit(row)">编辑</el-button>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUsers, createUser, updateUser, deleteUser } from '../../api/user'
import { getRoles } from '../../api/role'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref([])
const roles = ref([])
const dialogVisible = ref(false)
const isNew = ref(false)

const form = ref({ username: '', nickname: '', roleId: null, status: 1 })

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

onMounted(() => { loadUsers(); loadRoles() })
</script>

<style scoped>
.page { padding: 20px 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--si-border); }
.page-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: var(--si-text-primary); }
:deep(.el-table) { border-radius: var(--si-radius-md); }
:deep(.el-table th.el-table__cell) { background: var(--si-bg-hover); color: var(--si-text-secondary); font-weight: 600; }
</style>
