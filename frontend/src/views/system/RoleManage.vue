<template>
  <div class="page">
    <div class="page-header">
      <h3>权限套餐管理</h3>
      <el-button type="primary" size="small" @click="openNew">新增套餐</el-button>
    </div>
    <el-table :data="roles" border stripe size="small">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="名称" width="150" />
      <el-table-column prop="code" label="编码" width="150" />
      <el-table-column prop="description" label="描述" />
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增权限套餐" width="450px">
      <el-form :model="form" label-width="80px" size="small">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="编码">
          <el-input v-model="form.code" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
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
import { getRoles, createRole } from '../../api/role'
import { ElMessage } from 'element-plus'

const roles = ref([])
const dialogVisible = ref(false)
const form = ref({ name: '', code: '', description: '' })

async function loadRoles() {
  const res = await getRoles()
  roles.value = res.data || []
}

function openNew() {
  form.value = { name: '', code: '', description: '' }
  dialogVisible.value = true
}

async function handleSave() {
  await createRole(form.value)
  ElMessage.success('创建成功')
  dialogVisible.value = false
  loadRoles()
}

onMounted(loadRoles)
</script>

<style scoped>
.page { padding: 20px 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--si-border); }
.page-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: var(--si-text-primary); }
:deep(.el-table) { border-radius: var(--si-radius-md); }
:deep(.el-table th.el-table__cell) { background: var(--si-bg-hover); color: var(--si-text-secondary); font-weight: 600; }
</style>
