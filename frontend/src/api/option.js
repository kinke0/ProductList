import request from '../utils/request'

export function getOptions(versionId, type) {
  return request.get(`/options/${versionId}/${type}`)
}

export function createOption(versionId, type, value) {
  return request.post(`/options/${versionId}/${type}`, { value })
}

export function updateOption(id, value) {
  return request.put(`/options/${id}`, { value })
}

export function deleteOption(id) {
  return request.delete(`/options/${id}`)
}
