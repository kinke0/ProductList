<template>
  <div class="page gallery-page">
    <div class="page-header">
      <h3>图床管理</h3>
      <div class="header-actions">
        <el-button type="primary" size="small" :disabled="!selectedNode" @click="triggerUpload">
          <el-icon><Upload /></el-icon>上传图片
        </el-button>
        <el-button type="danger" size="small" :disabled="selectedIds.length === 0" @click="batchDelete">
          <el-icon><Delete /></el-icon>批量删除 ({{ selectedIds.length }})
        </el-button>
      </div>
      <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp" style="display:none" @change="handleFileUpload" />
    </div>
    <div class="gallery-body">
      <div class="gallery-sidebar">
        <div class="sidebar-header">目录</div>
        <el-tree
          :data="treeData"
          :props="{ children: 'children', label: 'label' }"
          node-key="label"
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
        <div v-if="filteredImages.length === 0" class="empty-tip">{{ currentImages.length > 0 ? '未找到匹配的图片' : '请选择左侧目录查看图片，或上传图片' }}</div>
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
                <el-button size="small" link @click="showReferences(img)">引用</el-button>
                <el-button size="small" link @click="replaceImage(img)">替换</el-button>
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
    <el-dialog v-model="refVisible" title="引用列表" width="60%">
      <el-table :data="refList" border size="small">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="colProductSystem" label="名称" />
        <el-table-column prop="colBizCategory" label="业务分类" width="120" />
        <el-table-column prop="colBizDomain" label="业务域" width="120" />
      </el-table>
      <div v-if="refList.length === 0" style="text-align:center;padding:20px;color:#999;">暂无引用</div>
    </el-dialog>
    <input ref="replaceFileInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp" style="display:none" @change="handleReplaceFile" />
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { getImages, uploadImage, deleteImage as deleteImageApi, getImageTree, getImageReferences, updateImage, batchDeleteImages } from '../../api/image'
import { getVersions } from '../../api/version'
import { Upload, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed } from 'vue'

const treeData = ref([])
const currentImages = ref([])
const selectedNode = ref(null)
const selectedCategory = ref(null)
const selectedDomain = ref(null)
const selectedProduct = ref(null)
const selectedIds = ref([])
const searchText = ref('')
const versionId = ref(null)
const previewVisible = ref(false)
const previewUrl = ref('')
const refVisible = ref(false)
const refList = ref([])
const fileInput = ref(null)
const editingImgId = ref(null)
const editingName = ref('')
const replacingImg = ref(null)
const replaceFileInput = ref(null)

async function loadVersion() {
  const res = await getVersions()
  const draft = res.data.find(v => v.status === 'draft')
  versionId.value = draft ? draft.id : (res.data[res.data.length - 1]?.id)
}

async function loadTree() {
  if (!versionId.value) return
  const res = await getImageTree(versionId.value)
  treeData.value = res.data || []
}

function onNodeClick(data, node) {
  const path = []
  let n = node
  while (n && n.data && n.data.label) { path.unshift(n.data.label); n = n.parent }
  if (path.length >= 3) {
    selectedCategory.value = path[0]
    selectedDomain.value = path[1]
    selectedProduct.value = path[2]
  } else if (path.length === 2) {
    selectedCategory.value = path[0]
    selectedDomain.value = path[1]
    selectedProduct.value = null
  } else {
    selectedCategory.value = path[0]
    selectedDomain.value = null
    selectedProduct.value = null
  }
  selectedNode.value = data
  loadImages()
}

async function loadImages() {
  const params = { versionId: versionId.value }
  if (selectedProduct.value) { params.category = selectedCategory.value; params.domain = selectedDomain.value; params.product = selectedProduct.value }
  else if (selectedDomain.value) { params.category = selectedCategory.value; params.domain = selectedDomain.value }
  else if (selectedCategory.value) { params.category = selectedCategory.value }
  const res = await getImages(params)
  currentImages.value = res.data || []
}

function triggerUpload() {
  fileInput.value.click()
}

async function handleFileUpload(e) {
  const file = e.target.files[0]
  if (!file) return
  const defaultName = file.name.replace(/\.[^.]+$/, '')
  try {
    const { value } = await ElMessageBox.prompt('请输入图片名称', '上传图片', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: defaultName,
      inputPlaceholder: '请输入名称'
    })
    const displayName = value || defaultName
    await uploadImage(file, selectedCategory.value, selectedDomain.value, selectedProduct.value, versionId.value, displayName)
    ElMessage.success('上传成功')
    loadImages()
    loadTree()
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') {
      ElMessage.error(err?.response?.data?.message || '上传失败')
    }
  }
  e.target.value = ''
}

function previewImage(img) {
  previewUrl.value = img.url
  previewVisible.value = true
}

function copyUrl(img) {
  const url = window.location.origin + img.url
  navigator.clipboard.writeText(url).then(() => ElMessage.success('URL已复制'))
}

async function showReferences(img) {
  try {
    const res = await getImageReferences(img.id)
    refList.value = res.data || []
    refVisible.value = true
  } catch (e) {
    ElMessage.error('查询引用失败')
  }
}

async function handleDelete(img) {
  try {
    const refRes = await getImageReferences(img.id)
    const refs = refRes.data || []
    if (refs.length > 0) {
      const list = refs.map(r => `ID:${r.id} ${r.colBizCategory || ''} - ${r.colBizDomain || ''} - ${r.colProductSystem || ''}`).join('\n')
      ElMessageBox.alert(
        `图片"${img.filename}"正被以下 ${refs.length} 条记录引用，无法删除：\n${list}`,
        '图片被引用',
        { type: 'warning' }
      )
      return
    }
    await ElMessageBox.confirm(`确认删除图片"${img.filename}"？`, '提示', { type: 'warning' })
    await deleteImageApi(img.id)
    ElMessage.success('删除成功')
    loadImages()
    loadTree()
    selectedIds.value = selectedIds.value.filter(id => id !== img.id)
  } catch (e) { /* cancel */ }
}

const filteredImages = computed(() => {
  if (!searchText.value) return currentImages.value
  const keyword = searchText.value.toLowerCase()
  return currentImages.value.filter(img => (img.filename || '').toLowerCase().includes(keyword))
})
const isIndeterminate = computed(() => {
  const count = filteredImages.value.filter(img => selectedIds.value.includes(img.id)).length
  return count > 0 && count < filteredImages.value.length
})

function toggleSelect(img) {
  const idx = selectedIds.value.indexOf(img.id)
  if (idx >= 0) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(img.id)
}

function toggleSelectAll(checked) {
  if (checked) selectedIds.value = filteredImages.value.map(img => img.id)
  else selectedIds.value = []
}

async function batchDelete() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 张图片？`, '批量删除', { type: 'warning' })
  } catch { return }
  const blocked = []
  const toDelete = []
  for (const id of selectedIds.value) {
    try {
      const res = await getImageReferences(id)
      const refs = res.data || []
      if (refs.length > 0) {
        const img = currentImages.value.find(i => i.id === id)
        blocked.push({ id, name: img?.filename, count: refs.length })
      } else {
        toDelete.push(id)
      }
    } catch { toDelete.push(id) }
  }
  if (toDelete.length === 0) {
    ElMessage.warning('所有选中图片均被引用，无法删除')
    return
  }
  if (blocked.length > 0) {
    const names = blocked.map(r => `"${r.name}" (${r.count}条引用)`).join('\n')
    try {
      await ElMessageBox.confirm(
        `${blocked.length} 张图片被引用无法删除：\n${names}\n\n确认删除其余 ${toDelete.length} 张图片？`,
        '部分图片被引用', { type: 'warning' }
      )
    } catch { return }
  }
  try {
    await batchDeleteImages(toDelete)
    ElMessage.success(`已删除 ${toDelete.length} 张图片`)
    selectedIds.value = []
    loadImages()
    loadTree()
  } catch {}
}

function formatSize(bytes) {
  if (!bytes) return '0B'
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1024 / 1024).toFixed(1) + 'MB'
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
  editingImgId.value = null
  if (newName && newName !== img.filename) {
    try {
      await updateImage(img.id, { filename: newName })
      img.filename = newName
      ElMessage.success('名称已更新')
    } catch (e) {
      ElMessage.error(e?.response?.data?.message || '更新失败')
    }
  }
}

function replaceImage(img) {
  replacingImg.value = img
  replaceFileInput.value.click()
}

async function handleReplaceFile(e) {
  const file = e.target.files[0]
  if (!file || !replacingImg.value) return
  const img = replacingImg.value
  try {
    const { value } = await ElMessageBox.prompt('请输入图片名称', '替换图片', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: img.filename,
      inputPlaceholder: '请输入名称'
    })
    const displayName = value || img.filename
    await uploadImage(file, selectedCategory.value, selectedDomain.value, selectedProduct.value, versionId.value, displayName)
    await deleteImageApi(img.id)
    ElMessage.success('替换成功')
    loadImages()
    loadTree()
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') {
      ElMessage.error(err?.response?.data?.message || '替换失败')
    }
  }
  replacingImg.value = null
  e.target.value = ''
}

onMounted(async () => {
  await loadVersion()
  await loadTree()
})
</script>

<style scoped>
.gallery-page { padding: 20px 24px; height: 100%; display: flex; flex-direction: column; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--si-border); }
.page-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: var(--si-text-primary); }
.header-actions { display: flex; gap: 8px; }
.gallery-body { display: flex; flex: 1; min-height: 0; gap: 16px; }
.gallery-sidebar { width: 240px; flex-shrink: 0; background: var(--si-bg-card); border: 1px solid var(--si-border); border-radius: var(--si-radius-lg); padding: 12px; overflow-y: auto; }
.sidebar-header { font-size: 13px; font-weight: 600; color: var(--si-text-secondary); margin-bottom: 8px; padding-bottom: 8px; border-bottom: 1px solid var(--si-border-light); }
.gallery-content { flex: 1; overflow-y: auto; background: var(--si-bg-card); border: 1px solid var(--si-border); border-radius: var(--si-radius-lg); padding: 16px; }
.empty-tip { text-align: center; padding: 60px 20px; color: var(--si-text-muted); font-size: 14px; }
.image-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px; }
.image-card { border: 1px solid var(--si-border); border-radius: var(--si-radius-md); overflow: hidden; background: #fff; transition: box-shadow 0.2s; position: relative; }
.image-card:hover { box-shadow: var(--si-shadow-md); }
.image-card.selected { border-color: #409eff; box-shadow: 0 0 0 2px rgba(64,158,255,0.2); }
.img-checkbox { position: absolute; top: 4px; left: 4px; z-index: 2; background: rgba(255,255,255,0.9); border-radius: 3px; padding: 2px; }
.gallery-toolbar { display: flex; align-items: center; padding-bottom: 12px; margin-bottom: 8px; border-bottom: 1px solid var(--si-border-light); }
.image-thumb { height: 140px; overflow: hidden; cursor: pointer; display: flex; align-items: center; justify-content: center; background: #f5f5f5; }
.image-thumb img { max-width: 100%; max-height: 100%; object-fit: contain; }
.image-info { padding: 6px 8px; display: flex; align-items: center; overflow: hidden; }
.image-name { font-size: 12px; color: var(--si-text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; min-width: 0; }
.image-size { font-size: 11px; color: var(--si-text-muted); flex-shrink: 0; margin-left: 4px; }
.image-actions { padding: 4px 8px 6px; display: flex; flex-wrap: wrap; gap: 2px; justify-content: center; border-top: 1px solid #e2e8f0; }

.image-name-edit { font-size: 12px; width: 70px; border: 1px solid #409eff; border-radius: 3px; padding: 0 3px; outline: none; height: 20px; line-height: 20px; }
</style>