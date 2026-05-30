<template>
  <el-dialog v-model="visible" title="预览" width="80%" top="5vh" :close-on-click-modal="true" @close="onClose">
    <template #header>
      <div style="display:flex;align-items:center;justify-content:space-between;width:100%;">
        <span>预览</span>
        <el-button type="primary" size="small" :loading="downloadLoading" @click="downloadPreview">下载Word</el-button>
      </div>
    </template>
    <div style="position:relative;">
      <iframe ref="frameRef" :srcdoc="html" @load="onFrameLoad" style="width:100%;height:70vh;border:1px solid #e2e8f0;border-radius:4px;" />
      <div v-if="downloadLoading" style="position:absolute;top:0;left:0;right:0;bottom:0;background:rgba(255,255,255,0.85);display:flex;flex-direction:column;align-items:center;justify-content:center;z-index:10;">
        <el-icon class="is-loading" style="font-size:48px;color:#409eff;"><Loading /></el-icon>
        <span style="margin-top:12px;color:#666;font-size:14px;">正在生成文档...</span>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  entryId: { type: Number, default: null },
  modelValue: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(false)
const html = ref('')
const downloadLoading = ref(false)
const frameRef = ref(null)
let pendingScrollTop = 0
let pendingHighlightId = null
let messageHandler = null

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.entryId) {
    loadPreview()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

watch(() => props.entryId, (newId) => {
  if (visible.value && newId) {
    loadPreview()
  }
})

onMounted(() => {
  messageHandler = (e) => {
    if (!visible.value) return
    emit('preview-message', e.data)
  }
  window.addEventListener('message', messageHandler)
})

onUnmounted(() => {
  if (messageHandler) window.removeEventListener('message', messageHandler)
})

async function loadPreview(timestamp) {
  html.value = ''
  try {
    const token = localStorage.getItem('token')
    const url = `/api/data/${props.entryId}/preview` + (timestamp ? `?_t=${timestamp}` : '')
    const resp = await fetch(url, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (resp.ok) {
      html.value = await resp.text()
    } else {
      html.value = '<p>加载预览失败</p>'
    }
  } catch {
    html.value = '<p>加载预览失败</p>'
  }
}

async function downloadPreview() {
  if (!props.entryId || downloadLoading.value) return
  downloadLoading.value = true
  try {
    const token = localStorage.getItem('token')
    const resp = await fetch(`/api/data/${props.entryId}/preview-download`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (!resp.ok) {
      ElMessage.error('生成失败')
      return
    }
    const blob = await resp.blob()
    const disposition = resp.headers.get('Content-Disposition')
    let filename = '预览文档.docx'
    if (disposition) {
      const match = disposition.match(/filename\*?=(?:UTF-8'')?(.+)/i)
      if (match) filename = decodeURIComponent(match[1].replace(/['"]/g, ''))
    }
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = filename; a.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载失败')
  } finally {
    downloadLoading.value = false
  }
}

function onFrameLoad() {
  try {
    var doc = frameRef.value?.contentWindow?.document
    if (!doc?.querySelector('.content')) return
  } catch { return }
  if (pendingScrollTop > 0) {
    try { frameRef.value?.contentWindow?.document?.querySelector?.('.content')?.scrollTo(0, pendingScrollTop) } catch {}
    pendingScrollTop = 0
  }
  if (pendingHighlightId) {
    frameRef.value?.contentWindow?.postMessage({ action: 'highlightEntry', entryId: pendingHighlightId }, '*')
    pendingHighlightId = null
  }
}

function onClose() {
  html.value = ''
}

function reload(highlightEntryId) {
  if (!props.entryId) return
  try { pendingScrollTop = frameRef.value?.contentWindow?.document?.querySelector?.('.content')?.scrollTop || 0 } catch {}
  pendingHighlightId = highlightEntryId || null
  loadPreview(Date.now())
}

function notifyUpdate(data) {
  if (!frameRef.value?.contentWindow) return
  frameRef.value.contentWindow.postMessage(data, '*')
}

defineExpose({ reload, notifyUpdate })
</script>
