import request from '../utils/request'

export function getTree(versionId) {
  return request.get(`/data/tree/${versionId}`)
}

export function getCategoryTree(versionId) {
  return request.get(`/tree?versionId=${versionId}`)
}

export function getChildren(versionId, parentId) {
  return request.get(`/data/children/${versionId}/${parentId}`)
}

export function getEntry(id) {
  return request.get(`/data/${id}`)
}

export function queryEntries(versionId, params) {
  return request.get(`/data/query/${versionId}`, { params })
}

export function createEntry(data) {
  return request.post('/data', data)
}

export function updateEntry(id, data) {
  return request.put(`/data/${id}`, data)
}

export function deleteEntry(id) {
  return request.delete(`/data/${id}`)
}

export function updateSort(sortList) {
  return request.put('/data/sort', sortList)
}

export function reorderAll(versionId) {
  return request.put(`/data/reorder/${versionId}`)
}

export function dedupEntries(versionId) {
  return request.delete(`/data/dedup/${versionId}`)
}

export function dedupDeepEntries(versionId) {
  return request.delete(`/data/dedup-deep/${versionId}`)
}

export function levelUp(id) {
  return request.put(`/data/${id}/level-up`)
}

export function levelDown(id) {
  return request.put(`/data/${id}/level-down`)
}

export function moveToParent(id, newParentId) {
  return request.put(`/data/${id}/move-to-parent`, { newParentId })
}

export function moveToSibling(id, targetId) {
  return request.put(`/data/${id}/move-to-sibling`, { targetId })
}

export function importExcel(file, versionId) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('versionId', versionId)
  return request.post('/data/import-excel', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export function batchDelete(versionId, ids) {
  return request.post(`/data/batch-delete?versionId=${versionId}`, ids)
}

export function getDomainTree(versionId, domainId, categoryId) {
  const params = { domainId }
  if (categoryId) params.categoryId = categoryId
  return request.get(`/data/domain-tree/${versionId}`, { params })
}

export function getSubTree(versionId, parentId) {
  return request.get(`/data/sub-tree/${versionId}/${parentId}`)
}

export function batchUpdateCategory(versionId, entryIds, categoryId, domainId, parentId = null) {
  const body = { versionId, entryIds, categoryId, domainId }
  if (parentId !== null) body.parentId = parentId
  return request.put('/data/batch-category', body)
}

export function updateCategorySort(versionId, sortList) {
  return request.put(`/category/sort?versionId=${versionId}`, sortList)
}

export function fixDataHierarchy(versionId) {
  return request.put(`/data/fix-hierarchy/${versionId}`)
}

export function previewEntry(id) {
  return request.get(`/data/${id}/preview`, { responseType: 'text' })
}

export function renumberEntries(items) {
  return request.put('/data/renumber', { items })
}

export function copyEntries(sourceIds, targetId, mode) {
  return request.post('/data/copy', { sourceIds, targetId, mode })
}

export function moveEntries(sourceIds, targetId, mode) {
  return request.put('/data/move', { sourceIds, targetId, mode })
}
