<template>
  <div class="page">
    <div class="page-header">
      <h3>{{ title }}</h3>
      <el-button size="small" type="primary" @click="openDialog()">新增</el-button>
    </div>
    <el-table :data="items" border stripe size="small">
      <el-table-column prop="value" label="名称" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" link @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showDialog" :title="isEdit ? '编辑' : '新增'" width="420px">
      <el-form :model="form" label-width="80px" size="small">
        <el-form-item label="名称">
          <el-input v-model="form.value" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getOptions, createOption, updateOption, deleteOption } from '../../api/option'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const type = route.meta.type || 'status'
const title = route.meta.title || '选项维护'

const items = ref([])
const showDialog = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const form = ref({ value: '' })

async function loadData() {
  const res = await getOptions(type)
  items.value = res.data || []
}

function openDialog(row) {
  if (row) { isEdit.value = true; editingId.value = row.id; form.value = { value: row.value } }
  else { isEdit.value = false; editingId.value = null; form.value = { value: '' } }
  showDialog.value = true
}

async function handleSave() {
  if (!form.value.value) { ElMessage.warning('请输入名称'); return }
  if (isEdit.value) await updateOption(editingId.value, form.value.value)
  else await createOption(type, form.value.value)
  ElMessage.success('保存成功')
  showDialog.value = false
  loadData()
}

async function handleDelete(id) {
  ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' }).then(async () => {
    await deleteOption(id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

onMounted(loadData)
</script>

<style scoped>
.page { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.page-header h3 { margin: 0; font-size: 16px; }
</style>
