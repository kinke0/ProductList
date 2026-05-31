import request from '../utils/request'

export function getCategoryTree(versionId) {
  return request.get('/tree', { params: { versionId } })
}

export function createCategory(versionId, name) {
  return request.post('/category', { name }, { params: { versionId } })
}

export function updateCategory(id, name) {
  return request.put(`/category/${id}`, { name })
}

export function deleteCategory(id) {
  return request.delete(`/category/${id}`)
}

export function createDomain(versionId, categoryId, name) {
  return request.post('/domain', { name }, { params: { versionId, categoryId } })
}

export function updateDomain(id, name) {
  return request.put(`/domain/${id}`, { name })
}

export function deleteDomain(id) {
  return request.delete(`/domain/${id}`)
}

export function updateCategorySort(versionId, sortList) {
  return request.put('/category/sort', sortList, { params: { versionId } })
}
