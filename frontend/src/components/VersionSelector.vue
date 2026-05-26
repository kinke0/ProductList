<template>
  <el-dialog :model-value="visible" @update:model-value="$emit('update:visible', $event)" title="选择版本" width="500px" :close-on-click-modal="false">
    <el-table :data="versions" highlight-current-row @current-change="onSelect">
      <el-table-column prop="versionNo" label="版本号" width="100" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'draft'" type="warning" size="small">编辑中</el-tag>
          <el-tag v-else type="success" size="small">已发布</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布日期">
        <template #default="{ row }">
          {{ row.releasedAt ? row.releasedAt.substring(0, 10) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="创建日期">
        <template #default="{ row }">
          {{ row.createdAt ? row.createdAt.substring(0, 10) : '-' }}
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :disabled="!selected" @click="confirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getVersions } from '../api/version'

const props = defineProps({ visible: Boolean })
const emit = defineEmits(['update:visible', 'select'])

const versions = ref([])
const selected = ref(null)

watch(() => props.visible, async (val) => {
  if (val) {
    const res = await getVersions()
    versions.value = res.data
  }
})

function onSelect(row) {
  selected.value = row
}

function confirm() {
  if (selected.value) {
    emit('select', selected.value)
  }
}
</script>
