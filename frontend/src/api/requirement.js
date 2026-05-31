import request from '../utils/request'

export function getRequirements(params) {
  return request.get('/requirements', { params })
}

export function getMyRequirements() {
  return request.get('/requirements/my')
}

export function getRequirementDetail(id) {
  return request.get(`/requirements/${id}`)
}

export function getRequirementStats(params) {
  return request.get('/requirements/stats', { params })
}

export function getModuleStats(params) {
  return request.get('/requirements/stats-by-module', { params })
}

export function getTypeStats(params) {
  return request.get('/requirements/stats-by-type', { params })
}

export function createRequirement(data) {
  return request.post('/requirements', data)
}

export function updateRequirement(id, data) {
  return request.put(`/requirements/${id}`, data)
}

export function confirmRequirement(id, data) {
  return request.put(`/requirements/${id}/confirm`, data)
}

export function developRequirement(id, data) {
  return request.put(`/requirements/${id}/develop`, data)
}

export function readyRequirement(id, data) {
  return request.put(`/requirements/${id}/ready`, data)
}

export function releaseRequirement(id, data) {
  return request.put(`/requirements/${id}/release`, data)
}

export function rejectRequirement(id, data) {
  return request.put(`/requirements/${id}/reject`, data)
}

export function cancelRequirement(id) {
  return request.put(`/requirements/${id}/cancel`)
}

export function deleteRequirement(id) {
  return request.delete(`/requirements/${id}`)
}
