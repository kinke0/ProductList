<template>
  <div class="page gallery-page">
    <div class="page-header">
      <h3>图床管理</h3>
      <el-button type="primary" size="small" :disabled="!selectedNode" @click="triggerUpload">
        <el-icon><Upload /></el-icon>上传图片
      </el-button>
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
        <div v-if="currentImages.length === 0" class="empty-tip">请选择左侧目录查看图片，或上传图片</div>
        <div v-else class="image-grid">
          <div v-for="img in currentImages" :key="img.id" class="image-card">
            <div class="image-thumb" @click="previewImage(img)">
              <img :src="img.url" :alt="img.filename" />
            </div>
            <div class="image-info">
              <span class="image-name" :title="img.filename">{{ img.filename }}</span>
              <span class="image-size">{{ formatSize(img.size) }}</span>
            </div>
            <div class="image-actions">
              <el-button size="small" type="primary" link @click="copyUrl(img)">复制URL</el-button>
              <el-button size="small" link @click="showReferences(img)">引用</el-button>
              <el-button size="small" type="danger" link @click="handleDelete(img)">删除</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
    <el-dialog v-model="previewVisible" title="图片预览" width="60%">
      <img v-if="previewUrl" :src="previewUrl" style="width:100%;" />
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getImages, uploadImage, deleteImage as deleteImageApi, getImageTree, getImageReferences } from '../../api/image'
import { getVersions } from '../../api/version'
import { Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const treeData = ref([])
const currentImages = ref([])
const selectedNode = ref(null)
const selectedCategory = ref(null)
const selectedDomain = ref(null)
const selectedProduct = ref(null)
const versionId = ref(null)
const previewVisible = ref(false)
const previewUrl = ref('')
const refVisible = ref(false)
const refList = ref([])
const fileInput = ref(null)

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

function onNodeClick(data) {
  const level = data.children ? (data.children[0]?.children ? 1 : 2) : 3
  if (level === 1) { selectedCategory.value = data.label; selectedDomain.value = null; selectedProduct.value = null }
  else if (level === 2) { selectedDomain.value = data.label }
  else { selectedProduct.value = data.label }
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
  try {
    await uploadImage(file, selectedCategory.value, selectedDomain.value, selectedProduct.value, versionId.value)
    ElMessage.success('上传成功')
    loadImages()
    loadTree()
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || '上传失败')
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
    await ElMessageBox.confirm(`确认删除图片"${img.filename}"？`, '提示', { type: 'warning' })
    await deleteImageApi(img.id)
    ElMessage.success('删除成功')
    loadImages()
    loadTree()
  } catch (e) { /* cancel */ }
}

function formatSize(bytes) {
  if (!bytes) return '0B'
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1024 / 1024).toFixed(1) + 'MB'
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
.gallery-body { display: flex; flex: 1; min-height: 0; gap: 16px; }
.gallery-sidebar { width: 240px; flex-shrink: 0; background: var(--si-bg-card); border: 1px solid var(--si-border); border-radius: var(--si-radius-lg); padding: 12px; overflow-y: auto; }
.sidebar-header { font-size: 13px; font-weight: 600; color: var(--si-text-secondary); margin-bottom: 8px; padding-bottom: 8px; border-bottom: 1px solid var(--si-border-light); }
.gallery-content { flex: 1; overflow-y: auto; background: var(--si-bg-card); border: 1px solid var(--si-border); border-radius: var(--si-radius-lg); padding: 16px; }
.empty-tip { text-align: center; padding: 60px 20px; color: var(--si-text-muted); font-size: 14px; }
.image-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px; }
.image-card { border: 1px solid var(--si-border); border-radius: var(--si-radius-md); overflow: hidden; background: #fff; transition: box-shadow 0.2s; }
.image-card:hover { box-shadow: var(--si-shadow-md); }
.image-thumb { height: 140px; overflow: hidden; cursor: pointer; display: flex; align-items: center; justify-content: center; background: #f5f5f5; }
.image-thumb img { max-width: 100%; max-height: 100%; object-fit: contain; }
.image-info { padding: 6px 8px; display: flex; justify-content: space-between; align-items: center; }
.image-name { font-size: 12px; color: var(--si-text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 120px; }
.image-size { font-size: 11px; color: var(--si-text-muted); }
.image-actions { padding: 4px 8px 6px; display: flex; gap: 4px; justify-content: center; border-top: 1px solid var(--si-border-light); }
</style>