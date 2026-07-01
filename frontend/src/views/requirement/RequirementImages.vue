<template>
  <div class="page gallery-page">
    <div class="page-header">
      <h3>需求图片</h3>
      <div class="header-actions">
        <el-button type="primary" size="small" :disabled="!selectedNode" @click="triggerUpload">
          <el-icon><Upload /></el-icon>上传图片
        </el-button>
        <el-button type="danger" size="small" :disabled="selectedIds.length === 0" @click="batchDelete">
          <el-icon><Delete /></el-icon>批量删除 ({{ selectedIds.length }})
        </el-button>
      </div>
      <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple style="display:none" @change="handleFileUpload" />
    </div>
    <div class="gallery-body">
      <div class="gallery-sidebar">
        <div class="sidebar-header">系统功能模块</div>
        <el-tree
          :data="treeData"
          :props="{ children: 'children', label: 'label' }"
          node-key="key"
          highlight-current
          default-expand-all
          @node-click="onNodeClick"
        />
      </div>
      <div class="gallery-content">
        <div v-if="currentImages.length > 0" class="gallery-toolbar">
          <el-checkbox :model-value="isAllSelected" :indeterminate="isIndeterminate" @change="toggleSelectAll">全选</el-checkbox>
          <el-input v-model="searchText" placeholder="搜索图片名称..." size="small" clearable style="width:200px;margin-left:12px;" />
        </div>
        <div v-if="filteredImages.length === 0" class="empty-tip">{{ currentImages.length > 0 ? '未找到匹配的图片' : '请选择左侧模块查看图片，或上传图片' }}</div>
        <div v-else class="image-grid">
          <el-tooltip v-for="img in filteredImages" :key="img.id" :content="img.filename" placement="top" :show-after="300" :hide-after="0">
            <div class="image-card" :class="{ selected: selectedIds.includes(img.id) }">
              <el-checkbox class="img-checkbox" :model-value="selectedIds.includes(img.id)" @change="toggleSelect(img)" @click.stop />
              <div class="image-thumb" @click="previewImage(img)">
                <img :src="img.url" :alt="img.filename" />
              </div>
              <div class="image-info">
                <el-button size="small" type="primary" link @click.stop="startEditName(img)">{{ editingImgId === img.id ? '保存' : '编辑' }}</el-button>
                <template v-if="editingImgId === img.id">
                  <input class="image-name-edit" v-model="editingName" @keydown.enter="saveImgName(img)" @blur="saveImgName(img)" @click.stop />
                </template>
                <template v-else>
                  <span class="image-name">{{ img.filename }}</span>
                </template>
                <span class="image-size">{{ formatSize(img.size) }}</span>
              </div>
              <div class="image-actions" @click.stop>
                <el-button size="small" type="primary" link @click="copyUrl(img)">复制URL</el-button>
                <el-button size="small" type="warning" link @click="showRefs(img)">引用</el-button>
                <el-button size="small" type="danger" link @click="handleDelete(img)">删除</el-button>
              </div>
            </div>
          </el-tooltip>
        </div>
      </div>
    </div>
    <el-dialog v-model="previewVisible" title="图片预览" width="auto" top="2vh" :style="{ maxWidth: '90vw' }">
      <div style="display:flex;align-items:center;justify-content:center;">
        <img v-if="previewUrl" :src="previewUrl" style="max-width:85vw;max-height:78vh;object-fit:contain;" />
      </div>
    </el-dialog>
    <el-dialog v-model="refVisible" :title="'引用需求 - ' + refImgName" width="550px">
      <div v-if="refList.length === 0" style="text-align:center;padding:20px;color:#999;">暂无需求引用此图片</div>
      <el-table v-else :data="refList" size="small" border>
        <el-table-column prop="reqNo" label="需求编号" width="140" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="70" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getImages, uploadImage, deleteImage as deleteImageApi, updateImage, batchDeleteImages, getImageReqReferences } from '../../api/image'
import { PLATFORM_MODULES } from '../../constants/platformModules'
import { Upload, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const treeData = ref([])
const currentImages = ref([])
const selectedNode = ref(null)
const selectedCategory = ref('')
const selectedDomain = ref('')
const selectedIds = ref([])
const searchText = ref('')
const previewVisible = ref(false)
const previewUrl = ref('')
const fileInput = ref(null)
const editingImgId = ref(null)
const editingName = ref('')
const refVisible = ref(false)
const refList = ref([])
const refImgName = ref('')

const filteredImages = computed(() => {
  if (!searchText.value) return currentImages.value
  const kw = searchText.value.toLowerCase()
  return currentImages.value.filter(img => (img.filename || '').toLowerCase().includes(kw))
})

const isAllSelected = computed(() => currentImages.value.length > 0 && selectedIds.value.length === currentImages.value.length)
const isIndeterminate = computed(() => selectedIds.value.length > 0 && selectedIds.value.length < currentImages.value.length)

async function loadTree() {
  const imgRes = await getImages({ category: '需求管理' })
  const allImages = imgRes.data || []

  const imageCountMap = {}
  allImages.forEach(img => {
    const key = img.domain || ''
    if (key) imageCountMap[key] = (imageCountMap[key] || 0) + 1
  })

  treeData.value = PLATFORM_MODULES.map(mod => {
    let catCount = 0
    const children = (mod.children || []).map(sub => {
      const cnt = imageCountMap[sub.value] || 0
      catCount += cnt
      return {
        key: mod.value + '/' + sub.value,
        label: sub.value + (cnt > 0 ? ` (${cnt})` : ''),
        _level: 2,
        _category: mod.value,
        _domain: sub.value,
        _count: cnt
      }
    })
    if (children.length === 0) {
      catCount = imageCountMap[mod.value] || 0
    }
    return {
      key: mod.value,
      label: mod.value + (catCount > 0 ? ` (${catCount})` : ''),
      _level: 1,
      _category: mod.value,
      _domain: '',
      _count: catCount,
      children
    }
  })
}

function onNodeClick(data) {
  selectedNode.value = data
  selectedCategory.value = data._category || ''
  selectedDomain.value = data._domain || ''
  loadImages()
}

async function loadImages() {
  const params = { category: '需求管理' }
  if (selectedDomain.value) {
    params.domain = selectedDomain.value
  }
  const res = await getImages(params)
  currentImages.value = res.data || []
}

function triggerUpload() { fileInput.value.click() }

async function handleFileUpload(e) {
  const files = Array.from(e.target.files || [])
  if (files.length === 0) return
  const uploadDomain = selectedDomain.value || ''
  if (files.length === 1) {
    const file = files[0]
    const defaultName = file.name.replace(/\.[^.]+$/, '')
    try {
      const { value } = await ElMessageBox.prompt('请输入图片名称', '上传图片', {
        confirmButtonText: '确定', cancelButtonText: '取消',
        inputValue: defaultName, inputPlaceholder: '请输入名称'
      })
      await uploadImage(file, '需求管理', uploadDomain, null, null, value || defaultName)
      ElMessage.success('上传成功')
      loadImages()
      loadTree()
    } catch (err) {
      if (err !== 'cancel' && err !== 'close') ElMessage.error(err?.response?.data?.message || '上传失败')
    }
  } else {
    try {
      let success = 0, failed = 0
      for (const file of files) {
        const displayName = file.name.replace(/\.[^.]+$/, '')
        try {
          await uploadImage(file, '需求管理', uploadDomain, null, null, displayName)
          success++
        } catch { failed++ }
      }
      if (success > 0) ElMessage.success(`成功上传 ${success} 张图片${failed > 0 ? `，${failed} 张失败` : ''}`)
      else ElMessage.error('全部上传失败')
      loadImages()
      loadTree()
    } catch { ElMessage.error('批量上传失败') }
  }
  e.target.value = ''
}

function toggleSelect(img) {
  const idx = selectedIds.value.indexOf(img.id)
  if (idx >= 0) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(img.id)
}

function toggleSelectAll(val) {
  selectedIds.value = val ? currentImages.value.map(i => i.id) : []
}

function previewImage(img) {
  previewUrl.value = img.url
  previewVisible.value = true
}

function copyUrl(img) {
  navigator.clipboard.writeText(img.url).then(() => ElMessage.success('已复制')).catch(() => ElMessage.error('复制失败'))
}

async function showRefs(img) {
  refImgName.value = img.filename
  refVisible.value = true
  try {
    const res = await getImageReqReferences(img.id)
    refList.value = res.data || []
  } catch {
    refList.value = []
  }
}

function handleDelete(img) {
  ElMessageBox.confirm(`确认删除 "${img.filename}"？`, '提示', { type: 'warning' }).then(async () => {
    try {
      const refs = await getImageReqReferences(img.id)
      if (refs.data && refs.data.length > 0) {
        const names = refs.data.map(r => r.reqNo).join(', ')
        ElMessage.warning(`该图片被以下需求引用，无法删除：${names}`)
        return
      }
    } catch {}
    await deleteImageApi(img.id)
    ElMessage.success('已删除')
    loadImages()
    loadTree()
  }).catch(() => {})
}

async function batchDelete() {
  ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 张图片？`, '提示', { type: 'warning' }).then(async () => {
    await batchDeleteImages(selectedIds.value)
    ElMessage.success('已删除')
    selectedIds.value = []
    loadImages()
    loadTree()
  }).catch(() => {})
}

function startEditName(img) {
  if (editingImgId.value === img.id) {
    saveImgName(img)
  } else {
    editingImgId.value = img.id
    editingName.value = img.filename
  }
}

async function saveImgName(img) {
  if (editingImgId.value !== img.id) return
  const newName = editingName.value.trim()
  if (newName && newName !== img.filename) {
    await updateImage(img.id, { filename: newName })
    img.filename = newName
  }
  editingImgId.value = null
}

function formatSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1024 / 1024).toFixed(1) + 'MB'
}

onMounted(async () => {
  await loadTree()
})
</script>

<style scoped>
.page { padding: 20px 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--si-border); }
.page-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: var(--si-text-primary); }
.header-actions { display: flex; gap: 8px; }

.gallery-body { display: flex; gap: 16px; min-height: calc(100vh - 180px); }
.gallery-sidebar {
  width: 220px; flex-shrink: 0; background: #fff;
  border: 1px solid var(--si-border); border-radius: 8px; overflow: hidden;
}
.sidebar-header { padding: 10px 12px; font-size: 13px; font-weight: 600; color: var(--si-text-secondary); border-bottom: 1px solid var(--si-border); }
.gallery-content { flex: 1; min-width: 0; }

.gallery-toolbar { display: flex; align-items: center; margin-bottom: 12px; }
.empty-tip { text-align: center; padding: 60px 20px; color: #909399; font-size: 14px; }
.image-grid { display: flex; flex-wrap: wrap; gap: 12px; }
.image-card {
  width: 180px; border: 1px solid var(--si-border); border-radius: 8px;
  overflow: hidden; background: #fff; transition: box-shadow 0.2s; position: relative;
}
.image-card.selected { border-color: #2563EB; box-shadow: 0 0 0 2px rgba(37,99,235,0.2); }
.image-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.img-checkbox { position: absolute; top: 6px; left: 6px; z-index: 1; }
.image-thumb { height: 140px; overflow: hidden; cursor: pointer; display: flex; align-items: center; justify-content: center; background: #f5f5f5; }
.image-thumb img { max-width: 100%; max-height: 100%; object-fit: contain; }
.image-info { display: flex; padding: 6px 8px; justify-content: space-between; align-items: center; }
.image-name { font-size: 12px; color: var(--si-text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; min-width: 0; }
.image-size { font-size: 11px; color: var(--si-text-muted); flex-shrink: 0; margin-left: 4px; }
.image-name-edit { font-size: 12px; border: 1px solid #dcdfe6; border-radius: 3px; padding: 1px 4px; width: 80px; outline: none; }
.image-actions { display: flex; gap: 4px; justify-content: center; padding: 4px 6px 6px; border-top: 1px solid #e2e8f0; }

:deep(.el-tree) { background: transparent; }
:deep(.el-tree-node__content) { height: 32px; font-size: 13px; }
</style>
