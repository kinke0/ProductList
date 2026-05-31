<template>
  <el-dialog v-model="visible" title="选择图片" width="70%" top="5vh" append-to-body @close="emit('close')">
    <div class="picker-body">
      <div v-if="treeData.length > 0" class="picker-sidebar">
        <el-tree
          ref="treeRef"
          :data="treeData"
          :props="{ children: 'children', label: 'label' }"
          node-key="key"
          highlight-current
          default-expand-all
          @node-click="onNodeClick"
        />
      </div>
      <div class="picker-content">
        <div class="picker-toolbar">
          <el-button size="small" type="primary" plain @click="triggerUpload"><el-icon><Upload /></el-icon>本地上传</el-button>
          <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple style="display:none" @change="handleFileUpload" />
        </div>
        <div class="picker-scroll">
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
import { getImages, uploadImage } from '../api/image'
import { PLATFORM_MODULES } from '../constants/platformModules'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: Boolean,
  defaultCategory: { type: String, default: null },
  defaultDomain: { type: String, default: null },
  defaultProduct: { type: String, default: null },
  fixedCategory: { type: String, default: null }
})
const emit = defineEmits(['update:modelValue', 'select', 'close'])

const visible = ref(false)
const treeData = ref([])
const images = ref([])
const selectedId = ref(null)
const selectedImage = ref(null)
const loading = ref(false)
const curCategory = ref(null)
const curDomain = ref(null)
const curProduct = ref(null)
const fileInput = ref(null)
const treeRef = ref(null)

watch(() => props.modelValue, async (v) => {
  visible.value = v
  if (v) {
    selectedId.value = null
    selectedImage.value = null
    await loadTree()
    await loadInitialImages()
  }
})
watch(visible, (v) => { emit('update:modelValue', v) })

async function loadTree() {
  if (props.fixedCategory) {
    const cat = props.fixedCategory
    const imgRes = await getImages({ category: cat })
    const allImages = imgRes.data || []
    const imageCountMap = {}
    allImages.forEach(img => {
      const key = img.domain || ''
      imageCountMap[key] = (imageCountMap[key] || 0) + 1
    })
    const mod = PLATFORM_MODULES.find(m => m.value === cat)
    const children = (mod?.children || []).map(sub => ({
      key: cat + '/' + sub.value,
      label: sub.value + (imageCountMap[sub.value] ? ` (${imageCountMap[sub.value]})` : ''),
      _category: cat,
      _domain: sub.value
    }))
    let totalCount = allImages.length
    treeData.value = [{
      key: cat,
      label: cat + (totalCount > 0 ? ` (${totalCount})` : ''),
      _category: cat,
      _domain: '',
      children: children.length > 0 ? children : undefined
    }]
    curCategory.value = cat
    curDomain.value = null
    curProduct.value = null
  } else {
    treeData.value = []
    curCategory.value = props.defaultCategory
    curDomain.value = null
    curProduct.value = null
  }
}

async function loadInitialImages() {
  loading.value = true
  try {
    if (props.fixedCategory) {
      const params = { category: props.fixedCategory }
      if (curDomain.value) params.domain = curDomain.value
      const res = await getImages(params)
      images.value = res.data || []
    } else {
      images.value = []
    }
  } finally {
    loading.value = false
  }
}

function onNodeClick(data) {
  curCategory.value = data._category || props.fixedCategory
  curDomain.value = data._domain || null
  curProduct.value = null
  loadFilteredImages()
}

async function loadFilteredImages() {
  loading.value = true
  try {
    const params = {}
    if (curCategory.value) params.category = curCategory.value
    if (curDomain.value) params.domain = curDomain.value
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
  const files = Array.from(e.target.files || [])
  if (files.length === 0) return
  const uploadCategory = curCategory.value || props.fixedCategory || props.defaultCategory
  const uploadDomain = curDomain.value || props.defaultDomain || ''
  const uploadProduct = curProduct.value || props.defaultProduct || ''
  if (files.length === 1) {
    const file = files[0]
    const defaultName = file.name.replace(/\.[^.]+$/, '')
    try {
      const { value } = await ElMessageBox.prompt('请输入图片名称', '上传图片', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: defaultName,
        inputPlaceholder: '请输入名称'
      })
      const displayName = value || defaultName
      await uploadImage(file, uploadCategory, uploadDomain, uploadProduct || null, null, displayName)
      ElMessage.success('上传成功')
    } catch (err) {
      if (err !== 'cancel' && err !== 'close') {
        ElMessage.error(err?.response?.data?.message || '上传失败')
      }
      e.target.value = ''
      return
    }
  } else {
    let success = 0
    let failed = 0
    for (const file of files) {
      const displayName = file.name.replace(/\.[^.]+$/, '')
      try {
        await uploadImage(file, uploadCategory, uploadDomain, uploadProduct || null, null, displayName)
        success++
      } catch {
        failed++
      }
    }
    if (success > 0) ElMessage.success(`成功上传 ${success} 张图片${failed > 0 ? `，${failed} 张失败` : ''}`)
    else ElMessage.error('全部上传失败')
  }
  await loadTree()
  if (uploadProduct && treeRef.value) {
    treeRef.value.setCurrentKey(uploadProduct)
  }
  await loadFilteredImages()
  e.target.value = ''
}
</script>

<style scoped>
.picker-body { display: flex; gap: 16px; height: 400px; }
.picker-sidebar { width: 220px; flex-shrink: 0; overflow-y: auto; border: 1px solid var(--si-border); border-radius: 8px; padding: 8px; }
.picker-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.picker-toolbar { flex-shrink: 0; padding: 0 0 8px 0; }
.picker-scroll { flex: 1; overflow-y: auto; }
.empty-tip { text-align: center; padding: 40px; color: #999; }
.image-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(100px, 1fr)); gap: 8px; }
.image-card { border: 2px solid transparent; border-radius: 6px; overflow: hidden; cursor: pointer; transition: border-color 0.2s; }
.image-card.selected { border-color: var(--el-color-primary); }
.image-thumb { height: 80px; display: flex; align-items: center; justify-content: center; background: #f5f5f5; }
.image-thumb img { max-width: 100%; max-height: 100%; object-fit: contain; }
.image-name { font-size: 11px; color: #666; text-align: center; padding: 2px 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
