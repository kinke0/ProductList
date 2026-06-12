import request from '../utils/request'

const PNG_IEND = [0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, 0xAE, 0x42, 0x60, 0x82]
const PNG_HEADER = [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]

async function validateImageFile(file) {
  if (!file.name.toLowerCase().endsWith('.png')) return
  const buf = await file.slice(-12).arrayBuffer()
  const tail = new Uint8Array(buf)
  if (tail.length < 12) {
    throw new Error(`图片 "${file.name}" 文件过小，可能已损坏`)
  }
  const isPng = file.slice(0, 8).arrayBuffer().then(b => {
    const h = new Uint8Array(b)
    return PNG_HEADER.every((v, i) => h[i] === v)
  })
  if (!await isPng) return
  const hasIend = PNG_IEND.every((v, i) => tail[i] === v)
  if (!hasIend) {
    throw new Error(`图片 "${file.name}" 文件不完整（缺少结束标记），请重新导出后上传`)
  }
}

export function uploadImage(file, category, domain, product, versionId, filename) {
  return validateImageFile(file).then(() => {
    const formData = new FormData()
    formData.append('file', file)
    if (category) formData.append('category', category)
    if (domain) formData.append('domain', domain)
    if (product) formData.append('product', product)
    if (versionId) formData.append('versionId', versionId)
    if (filename) formData.append('filename', filename)
    return request.post('/images/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 30000
    })
  })
}

export function getImageTree(versionId) {
  return request.get('/images/tree', { params: { versionId } })
}

export function getImages(params) {
  return request.get('/images', { params })
}

export function deleteImage(id) {
  return request.delete(`/images/${id}`)
}

export function updateImage(id, data) {
  return request.put(`/images/${id}`, data)
}

export function replaceImageFile(id, file) {
  return validateImageFile(file).then(() => {
    const formData = new FormData()
    formData.append('file', file)
    return request.put(`/images/${id}/file`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 30000
    })
  })
}

export function getImageReferences(id) {
  return request.get(`/images/${id}/references`)
}

export function getAllImageReferences(id) {
  return request.get(`/images/${id}/all-references`)
}

export function getImageReqReferences(id) {
  return request.get(`/images/${id}/req-references`)
}

export function migrateImages(ids) {
  return request.post('/images/migrate-external-images', ids)
}

export function getMigrationProgress(taskId) {
  return request.get(`/images/migrate-task/${taskId}`)
}

export function batchDeleteImages(ids) {
  return request.post('/images/batch-delete', ids)
}

export function getBatchReferences(ids) {
  return request.post('/images/batch-references', ids)
}