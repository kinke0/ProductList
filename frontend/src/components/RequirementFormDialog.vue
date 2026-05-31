<template>
  <el-dialog v-model="visible" :title="isEdit ? '编辑需求' : '提交需求'" width="720px" @close="onClose">
    <el-form :model="form" label-width="80px" size="small">
      <el-form-item label="标题" required>
        <el-input v-model="form.title" placeholder="请输入需求标题" />
      </el-form-item>
      <el-form-item label="所属模块" required>
        <el-cascader
          v-model="modulePath"
          :options="moduleOptions"
          :props="{ checkStrictly: true, value: 'value', label: 'label', children: 'children' }"
          :disabled="!!prefilledModule"
          placeholder="请选择产品模块"
          clearable
          style="width: 100%;"
          @change="onModuleChange"
        />
      </el-form-item>
      <el-form-item label="需求类型" required>
        <el-select v-model="form.type" placeholder="请选择需求类型" style="width: 100%;">
          <el-option label="功能增强" value="功能增强" />
          <el-option label="Bug修复" value="Bug修复" />
          <el-option label="新功能" value="新功能" />
          <el-option label="性能优化" value="性能优化" />
        </el-select>
      </el-form-item>
      <el-form-item label="优先级" required>
        <el-select v-model="form.priority" style="width: 100%;">
          <el-option label="高" value="高" />
          <el-option label="中" value="中" />
          <el-option label="低" value="低" />
        </el-select>
      </el-form-item>
      <el-form-item label="描述">
        <div class="feature-editor">
          <div class="feature-editor-toolbar">
            <button type="button" class="fe-btn" title="插入图片" @click="openImagePicker">🖼</button>
            <button type="button" class="fe-btn" title="截图" @click="handleScreenshot">✂</button>
          </div>
          <div class="feature-editor-body" contenteditable="true" ref="editorRef" @input="onEditorInput" @click="onEditorClick"></div>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>

  <ImagePicker v-model="showImagePicker" :fixed-category="'需求管理'" :default-category="pickerCategory" @select="insertImageFromPicker" />

  <div v-if="screenshotActive" class="screenshot-hint">
    <div class="screenshot-hint-text">截图模式：拖拽选择区域，Esc 取消</div>
  </div>

  <el-dialog v-model="imgPreviewVisible" title="图片预览" width="auto" top="2vh" :style="{ maxWidth: '90vw' }">
    <div style="display:flex;align-items:center;justify-content:center;">
      <img v-if="imgPreviewUrl" :src="imgPreviewUrl" style="max-width:85vw;max-height:78vh;object-fit:contain;" />
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { createRequirement, updateRequirement } from '../api/requirement'
import { uploadImage, updateImage, replaceImageFile } from '../api/image'
import { startScreenshot, openAnnotationEditor } from '../utils/screenshot'
import { PLATFORM_MODULES } from '../constants/platformModules'
import ImagePicker from './ImagePicker.vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: Boolean,
  editId: { type: Number, default: null },
  initialData: { type: Object, default: null },
  prefilledModule: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const isEdit = computed(() => !!props.editId)
const form = ref({ title: '', description: '', priority: '中', category: '', domain: '', type: '' })
const modulePath = ref([])
const moduleOptions = ref([])
const editorRef = ref(null)
const showImagePicker = ref(false)
const screenshotActive = ref(false)
let savedSelectionRange = null
const imgPreviewVisible = ref(false)
const imgPreviewUrl = ref('')
const restoringFromScreenshot = ref(false)

const pickerCategory = computed(() => {
  const parts = ['需求管理']
  if (form.value.category) parts.push(form.value.category)
  if (form.value.domain) parts.push(form.value.domain)
  return parts.join('/')
})

function loadModuleTree() {
  moduleOptions.value = PLATFORM_MODULES.map(m => ({
    value: m.value,
    label: m.label,
    children: m.children || []
  }))
}

function onModuleChange(val) {
  if (!val || val.length === 0) {
    form.value.category = ''
    form.value.domain = ''
  } else if (val.length === 1) {
    form.value.category = val[0]
    form.value.domain = ''
  } else {
    form.value.category = val[0]
    form.value.domain = val[1]
  }
}

watch(() => props.modelValue, (val) => {
  if (val) {
    if (restoringFromScreenshot.value) {
      restoringFromScreenshot.value = false
      return
    }
    loadModuleTree()
    if (props.initialData) {
      form.value = {
        title: props.initialData.title || '',
        description: props.initialData.description || '',
        priority: props.initialData.priority || '中',
        category: props.initialData.category || '',
        domain: props.initialData.domain || '',
        type: props.initialData.type || ''
      }
    } else {
      form.value = { title: '', description: '', priority: '中', category: '', domain: '', type: '' }
    }
    if (props.prefilledModule) {
      form.value.category = props.prefilledModule.category || ''
      form.value.domain = props.prefilledModule.domain || ''
      if (props.prefilledModule.domain) {
        modulePath.value = [props.prefilledModule.category, props.prefilledModule.domain]
      } else if (props.prefilledModule.category) {
        modulePath.value = [props.prefilledModule.category]
      }
    } else {
      modulePath.value = form.value.domain
        ? [form.value.category, form.value.domain]
        : (form.value.category ? [form.value.category] : [])
    }
    nextTick(() => {
      if (editorRef.value) {
        editorRef.value.innerHTML = form.value.description || ''
      }
    })
  }
})

function onEditorInput() {
  if (editorRef.value) {
    form.value.description = editorRef.value.innerHTML
  }
}

function onEditorClick(e) {
  const card = e.target.closest('.image-card')
  if (!card || !card.closest('.feature-editor-body')) return
  e.preventDefault()
  e.stopPropagation()
  const actionBtn = e.target.closest('[data-action]')
  if (!actionBtn) return
  const action = actionBtn.getAttribute('data-action')
  const url = card.getAttribute('data-url')

  if (action === 'preview' && url) {
    imgPreviewUrl.value = url
    imgPreviewVisible.value = true
  } else if (action === 'delete') {
    card.remove()
    form.value.description = editorRef.value?.innerHTML || ''
  } else if (action === 'rename') {
    const nameEl = card.querySelector('.image-name')
    if (nameEl && !nameEl.querySelector('input')) {
      const oldName = nameEl.textContent
      const input = document.createElement('input')
      input.type = 'text'
      input.value = oldName
      input.style.cssText = 'width:60px;font-size:12px;border:1px solid #409eff;border-radius:3px;padding:0 3px;outline:none;height:18px;line-height:18px;vertical-align:middle;box-sizing:border-box;'
      nameEl.textContent = ''
      nameEl.appendChild(input)
      input.focus()
      input.select()
      const imgId = card.getAttribute('data-id')
      actionBtn.textContent = '✓'
      actionBtn.setAttribute('data-action', 'save-name')
      const doSave = () => {
        const newName = input.value.trim() || oldName
        card.setAttribute('data-filename', newName)
        card.title = newName
        nameEl.textContent = newName
        actionBtn.textContent = '改名'
        actionBtn.setAttribute('data-action', 'rename')
        form.value.description = editorRef.value?.innerHTML || ''
        if (imgId) {
          updateImage(Number(imgId), { filename: newName }).catch(() => {})
        }
      }
      let blurTimeout = null
      input.addEventListener('blur', () => {
        clearTimeout(blurTimeout)
        blurTimeout = setTimeout(doSave, 150)
      })
      input.addEventListener('keydown', (ev) => {
        if (ev.key === 'Enter') { ev.preventDefault(); clearTimeout(blurTimeout); doSave() }
      })
    }
  } else if (action === 'save-name') {
    const nameEl = card.querySelector('.image-name')
    const input = nameEl?.querySelector('input')
    if (input) {
      input.dispatchEvent(new Event('blur'))
    }
  } else if (action === 'edit') {
    handleEditImage(card, url)
  }
}

async function handleEditImage(card, url) {
  if (!url) return
  const oldId = card.getAttribute('data-id')
  try {
    const resp = await fetch(url)
    if (!resp.ok) { ElMessage.error('无法获取原图'); return }
    const blob = await resp.blob()
    if (!blob.type || !blob.type.startsWith('image/')) { ElMessage.error('图片数据无效'); return }
    const dataUrl = await new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(reader.result)
      reader.onerror = reject
      reader.readAsDataURL(blob)
    })
    const annotated = await openAnnotationEditor(dataUrl)
    if (!annotated) return
    const newBlob = await (await fetch(annotated)).blob()
    const file = new File([newBlob], `edited_${Date.now()}.png`, { type: 'image/png' })
    if (oldId) {
      const res = await replaceImageFile(Number(oldId), file)
      const imgEl = card.querySelector('.image-thumb img')
      if (imgEl) imgEl.src = res.data.url + '?t=' + Date.now()
      form.value.description = editorRef.value?.innerHTML || ''
      ElMessage.success('图片已更新')
    } else {
      const uploadDomain = form.value.domain || ''
      const res = await uploadImage(file, '需求管理', uploadDomain, null, null, `编辑_${Date.now()}`)
      const newUrl = res.data.url
      card.setAttribute('data-url', newUrl)
      card.setAttribute('data-id', res.data.id || '')
      const imgEl = card.querySelector('.image-thumb img')
      if (imgEl) imgEl.src = newUrl
      form.value.description = editorRef.value?.innerHTML || ''
      ElMessage.success('图片已更新')
    }
  } catch (e) {
    ElMessage.error('编辑图片失败')
  }
}

function onClose() {
  if (restoringFromScreenshot.value) return
  if (editorRef.value) {
    editorRef.value.innerHTML = ''
  }
  modulePath.value = []
}

function openImagePicker() {
  const sel = window.getSelection()
  if (sel.rangeCount > 0) {
    savedSelectionRange = sel.getRangeAt(0).cloneRange()
  }
  showImagePicker.value = true
}

function insertImageFromPicker(img) {
  if (!editorRef.value || !img.url) return
  const name = img.filename || '图片'
  const card = document.createElement('span')
  card.className = 'image-card'
  card.setAttribute('contenteditable', 'false')
  card.setAttribute('data-url', img.url)
  card.setAttribute('data-filename', name)
  card.setAttribute('data-id', img.id || '')
  card.title = name
  const sizeStr = img.size ? formatSize(img.size) : ''
  card.innerHTML = `<span class="image-thumb"><img src="${img.url}" alt="${name}" /></span><span class="image-info"><button type="button" class="image-action-btn image-edit-name-btn" data-action="rename">改名</button><span class="image-name">${name}</span><span class="image-size">${sizeStr}</span></span><span class="image-actions"><button type="button" class="image-action-btn" data-action="preview">预览</button><button type="button" class="image-action-btn" data-action="edit">编辑</button><button type="button" class="image-action-btn image-action-danger" data-action="delete">删除</button></span>`
  const after = document.createElement('br')
  editorRef.value.focus()
  if (savedSelectionRange) {
    const sel = window.getSelection()
    sel.removeAllRanges()
    sel.addRange(savedSelectionRange)
    savedSelectionRange = null
  }
  const sel = window.getSelection()
  if (sel.rangeCount) {
    const range = sel.getRangeAt(0)
    range.deleteContents()
    range.insertNode(after)
    range.insertNode(card)
    range.setStartAfter(after)
    range.collapse(true)
  } else {
    editorRef.value.appendChild(card)
    editorRef.value.appendChild(after)
  }
  form.value.description = editorRef.value.innerHTML
}

async function handleScreenshot() {
  restoringFromScreenshot.value = true
  visible.value = false
  screenshotActive.value = true
  await nextTick()
  await new Promise(r => setTimeout(r, 300))

  const result = await startScreenshot()
  if (!result) {
    screenshotActive.value = false
    visible.value = true
    return
  }

  const annotated = await openAnnotationEditor(result.dataUrl)
  screenshotActive.value = false
  visible.value = true

  if (!annotated) return

  try {
    const blob = await (await fetch(annotated)).blob()
    const file = new File([blob], `screenshot_${Date.now()}.png`, { type: 'image/png' })
    const uploadDomain = form.value.domain || ''
    const res = await uploadImage(file, '需求管理', uploadDomain, null, null, `截图_${Date.now()}`)
    insertImageFromPicker({ url: res.data.url, filename: res.data.filename, id: res.data.id, size: res.data.size })
    ElMessage.success('截图已插入')
  } catch (e) {
    ElMessage.error('截图上传失败')
  }
}

async function handleSave() {
  if (!form.value.title) { ElMessage.warning('请输入标题'); return }
  if (!form.value.category) { ElMessage.warning('请选择所属模块'); return }
  if (!form.value.type) { ElMessage.warning('请选择需求类型'); return }
  onEditorInput()
  if (isEdit.value) {
    await updateRequirement(props.editId, form.value)
  } else {
    await createRequirement(form.value)
  }
  ElMessage.success(isEdit.value ? '更新成功' : '提交成功')
  visible.value = false
  emit('saved')
}

function formatSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1024 / 1024).toFixed(1) + 'MB'
}
</script>

<style scoped>
.feature-editor { width: 100%; }
.feature-editor-toolbar {
  display: flex; align-items: center; gap: 4px; padding: 8px 12px;
  border: 1px solid #dcdfe6; border-bottom: none; border-radius: 8px 8px 0 0;
  background: #fafafa;
}
.fe-btn {
  display: inline-flex; align-items: center; justify-content: center;
  width: 28px; height: 28px; border: none; background: transparent;
  border-radius: 4px; cursor: pointer; color: #444; padding: 0;
  font-size: 14px; transition: background 0.2s, color 0.2s;
}
.fe-btn:hover { background: #e8e8e8; color: #1a1a1a; }
.feature-editor-body {
  min-height: 200px; max-height: 400px; overflow-y: auto;
  border: 1px solid #dcdfe6; border-radius: 0 0 8px 8px; padding: 12px 15px;
  font-size: 14px; line-height: 1.6; outline: none; white-space: pre-wrap;
  word-wrap: break-word; background: #fff;
}
.feature-editor-body:empty::before {
  content: '请输入需求描述（支持插入图片和截图）...'; color: #c0c4cc; pointer-events: none;
}
.feature-editor :deep(.image-card) {
  display: inline-block; width: 180px; vertical-align: top;
  border: 1px solid #e2e8f0; border-radius: 6px;
  overflow: hidden; background: #fff; margin: 4px 8px 4px 0;
  user-select: none; transition: box-shadow 0.2s;
}
.feature-editor :deep(.image-card:hover) { box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.feature-editor :deep(.image-thumb) {
  height: 140px; overflow: hidden; cursor: pointer;
  display: flex; align-items: center; justify-content: center; background: #f5f5f5;
}
.feature-editor :deep(.image-thumb img) { max-width: 100%; max-height: 100%; object-fit: contain; }
.feature-editor :deep(.image-info) { display: flex; padding: 6px 8px; justify-content: space-between; align-items: center; }
.feature-editor :deep(.image-name) {
  font-size: 12px; color: #334155; overflow: hidden;
  text-overflow: ellipsis; white-space: nowrap; flex: 1; min-width: 0;
}
.feature-editor :deep(.image-size) { font-size: 11px; color: #94A3B8; flex-shrink: 0; margin-left: 4px; }
.feature-editor :deep(.image-edit-name-btn) {
  flex-shrink: 0; margin-right: 2px; min-width: auto; padding: 0 3px; font-size: 10px;
  line-height: 1.2; height: 18px; border: none; background: none; cursor: pointer; color: #409eff;
}
.feature-editor :deep(.image-actions) {
  display: flex; flex-wrap: wrap; gap: 2px; justify-content: center;
  padding: 4px 6px 6px; border-top: 1px solid #e2e8f0;
}
.feature-editor :deep(.image-action-btn) {
  font-size: 12px; border: none; background: none; cursor: pointer; padding: 2px 6px;
  color: #409eff; border-radius: 3px; line-height: 1.4;
}
.feature-editor :deep(.image-action-btn:hover) { background: #ecf5ff; }
.feature-editor :deep(.image-action-danger) { color: #f56c6c; }
.feature-editor :deep(.image-action-danger:hover) { background: #fef0f0; }

.screenshot-hint { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; z-index: 99997; pointer-events: none; }
.screenshot-hint-text {
  position: absolute; top: 20px; left: 50%; transform: translateX(-50%);
  background: rgba(0,0,0,0.7); color: #fff; padding: 8px 20px; border-radius: 6px; font-size: 14px;
}
</style>
