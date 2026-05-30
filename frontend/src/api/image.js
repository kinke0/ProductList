import request from '../utils/request'

export function uploadImage(file, category, domain, product, versionId, filename) {
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

export function getImageReferences(id) {
  return request.get(`/images/${id}/references`)
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