<template>
  <el-dialog v-model="visible" title="选择图片" width="70%" top="5vh" append-to-body @close="emit('close')">
    <div class="picker-body">
      <div class="picker-content">
        <div class="picker-toolbar">
          <el-button size="small" type="primary" plain @click="triggerUpload"><el-icon><Upload /></el-icon>本地上传</el-button>
          <el-input v-model="searchText" placeholder="搜索图片名称..." size="small" clearable style="width:180px;margin-left:8px;" />
          <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple style="display:none" @change="handleFileUpload" />
        </div>
        <div class="picker-scroll">
          <div v-if="loading" style="text-align:center;padding:40px;">加载中...</div>
          <div v-else-if="filteredImages.length === 0" class="empty-tip">暂无图片，可点击上方按钮上传</div>
          <div v-else class="image-grid">
            <div v-for="img in filteredImages" :key="img.id" class="image-card" :class="{ selected: selectedId === img.id }" @click="selectImage(img)">
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
import { ref, computed, watch } from 'vue'
import { getImages, uploadImage } from '../api/image'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: Boolean,
  defaultCategory: { type: String, default: null },
  defaultDomain: { type: String, default: null },
  defaultProduct: { type: String, default: null },
  fixedCategory: { type: String, default: null },
  versionId: { type: Number, default: null }
})
const emit = defineEmits(['update:modelValue', 'select', 'close'])

const visible = ref(false)
const images = ref([])
const selectedId = ref(null)
const selectedImage = ref(null)
const loading = ref(false)
const curCategory = ref(null)
const curDomain = ref(null)
const curProduct = ref(null)
const fileInput = ref(null)
const searchText = ref('')

const filteredImages = computed(() => {
  if (!searchText.value) return images.value
  const kw = searchText.value.toLowerCase()
  return images.value.filter(img => (img.filename || '').toLowerCase().includes(kw))
})

watch(() => props.modelValue, async (v) => {
  visible.value = v
  if (v) {
    selectedId.value = null
    selectedImage.value = null
    searchText.value = ''
    curCategory.value = props.fixedCategory || props.defaultCategory
    curDomain.value = props.defaultDomain
    curProduct.value = props.defaultProduct
    await loadImages()
  }
})
watch(visible, (v) => { emit('update:modelValue', v) })

async function loadImages() {
  loading.value = true
  try {
    const params = { includeReferenced: false }
    if (curCategory.value) params.category = curCategory.value
    if (curDomain.value) params.domain = curDomain.value
    if (props.versionId) params.versionId = props.versionId
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
      await uploadImage(file, uploadCategory, uploadDomain, uploadProduct || null, props.versionId, displayName)
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
        await uploadImage(file, uploadCategory, uploadDomain, uploadProduct || null, props.versionId, displayName)
        success++
      } catch {
        failed++
      }
    }
    if (success > 0) ElMessage.success(`成功上传 ${success} 张图片${failed > 0 ? `，${failed} 张失败` : ''}`)
    else ElMessage.error('全部上传失败')
  }
  await loadImages()
  e.target.value = ''
}
</script>

<style scoped>
.picker-body { display: flex; height: 400px; }
.picker-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.picker-toolbar { flex-shrink: 0; padding: 0 0 8px 0; display: flex; align-items: center; gap: 8px; }
.picker-scroll { flex: 1; overflow-y: auto; }
.empty-tip { text-align: center; padding: 40px; color: #999; }
.image-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(100px, 1fr)); gap: 8px; }
.image-card { border: 2px solid transparent; border-radius: 6px; overflow: hidden; cursor: pointer; transition: border-color 0.2s; }
.image-card.selected { border-color: var(--el-color-primary); }
.image-thumb { height: 80px; display: flex; align-items: center; justify-content: center; background: #f5f5f5; }
.image-thumb img { max-width: 100%; max-height: 100%; object-fit: contain; }
.image-name { font-size: 11px; color: #666; text-align: center; padding: 2px 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
