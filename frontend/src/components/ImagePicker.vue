<template>
  <el-dialog v-model="visible" title="选择图片" width="70%" top="5vh" @close="emit('close')">
    <div class="picker-body">
      <div class="picker-sidebar">
        <el-tree
          ref="treeRef"
          :data="treeData"
          :props="{ children: 'children', label: 'label' }"
          node-key="label"
          highlight-current
          default-expand-all
          @node-click="onNodeClick"
        />
      </div>
      <div class="picker-content">
        <div v-if="loading" style="text-align:center;padding:40px;">加载中...</div>
        <div v-else-if="images.length === 0" class="empty-tip">请选择目录或上传图片</div>
        <div v-else class="image-grid">
          <div v-for="img in images" :key="img.id" class="image-card" :class="{ selected: selectedId === img.id }" @click="selectImage(img)">
            <div class="image-thumb">
              <img :src="img.url" :alt="img.filename" />
            </div>
            <div class="image-name">{{ img.filename }}</div>
          </div>
        </div>
        <div style="margin-top:12px;text-align:center;">
          <el-button size="small" @click="triggerUpload">本地上传</el-button>
          <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp" style="display:none" @change="handleFileUpload" />
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="!selectedImage" @click="confirmSelect">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getImages, uploadImage, getImageTree } from '../api/image'
import { getVersions } from '../api/version'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  modelValue: Boolean,
  defaultCategory: { type: String, default: null },
  defaultDomain: { type: String, default: null },
  defaultProduct: { type: String, default: null }
})
const emit = defineEmits(['update:modelValue', 'select', 'close'])

const visible = ref(false)
const treeData = ref([])
const images = ref([])
const selectedId = ref(null)
const selectedImage = ref(null)
const loading = ref(false)
const versionId = ref(null)
const curCategory = ref(null)
const curDomain = ref(null)
const curProduct = ref(null)
const fileInput = ref(null)
const treeRef = ref(null)

watch(() => props.modelValue, async (v) => {
  visible.value = v
  if (v) {
    await loadVersion()
    await loadTree()
  }
})
watch(visible, (v) => { emit('update:modelValue', v) })

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
    curCategory.value = path[0]
    curDomain.value = path[1]
    curProduct.value = path[2]
  } else if (path.length === 2) {
    curCategory.value = path[0]
    curDomain.value = path[1]
    curProduct.value = null
  } else {
    curCategory.value = path[0]
    curDomain.value = null
    curProduct.value = null
  }
  loadImages()
}

async function loadImages() {
  loading.value = true
  try {
    const params = { versionId: versionId.value }
    if (curProduct.value) { params.category = curCategory.value; params.domain = curDomain.value; params.product = curProduct.value }
    else if (curDomain.value) { params.category = curCategory.value; params.domain = curDomain.value }
    else if (curCategory.value) { params.category = curCategory.value }
    const res = await getImages(params)
    images.value = res.data || []
  } finally {
    loading.value = false
  }
}

function selectImage(img) {
  selectedId.value = img.id
  selectedImage.value = img
}

async function confirmSelect() {
  if (selectedImage.value) {
    emit('select', {
      ...selectedImage.value,
      _pendingCategory: props.defaultCategory || null,
      _pendingDomain: props.defaultDomain || null,
      _pendingProduct: props.defaultProduct || null
    })
    visible.value = false
  }
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
    const uploadCategory = curCategory.value || props.defaultCategory
    const uploadDomain = curDomain.value || props.defaultDomain
    const uploadProduct = curProduct.value || props.defaultProduct
    await uploadImage(file, uploadCategory, uploadDomain, uploadProduct, versionId.value, displayName)
    ElMessage.success('上传成功')
    await loadTree()
    curCategory.value = uploadCategory
    curDomain.value = uploadDomain
    curProduct.value = uploadProduct
    if (uploadProduct && treeRef.value) {
      treeRef.value.setCurrentKey(uploadProduct)
    }
    await loadImages()
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') {
      ElMessage.error(err?.response?.data?.message || '上传失败')
    }
  }
  e.target.value = ''
}
</script>

<style scoped>
.picker-body { display: flex; gap: 16px; height: 400px; }
.picker-sidebar { width: 220px; flex-shrink: 0; overflow-y: auto; border: 1px solid var(--si-border); border-radius: 8px; padding: 8px; }
.picker-content { flex: 1; overflow-y: auto; }
.empty-tip { text-align: center; padding: 40px; color: #999; }
.image-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(100px, 1fr)); gap: 8px; }
.image-card { border: 2px solid transparent; border-radius: 6px; overflow: hidden; cursor: pointer; transition: border-color 0.2s; }
.image-card.selected { border-color: var(--el-color-primary); }
.image-thumb { height: 80px; display: flex; align-items: center; justify-content: center; background: #f5f5f5; }
.image-thumb img { max-width: 100%; max-height: 100%; object-fit: contain; }
.image-name { font-size: 11px; color: #666; text-align: center; padding: 2px 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>