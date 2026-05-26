import request from '../utils/request'

export function getOptions(type) {
  return request.get(`/options/${type}`)
}

export function createOption(type, value) {
  return request.post(`/options/${type}`, { value })
}

export function updateOption(id, value) {
  return request.put(`/options/${id}`, { value })
}

export function deleteOption(id) {
  return request.delete(`/options/${id}`)
}
