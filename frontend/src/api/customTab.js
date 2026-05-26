import request from '../utils/request'

export function getCustomTabs(versionId) {
  return request.get(`/custom-tab/${versionId}`)
}

export function createCustomTab(data) {
  return request.post('/custom-tab', data)
}

export function deleteCustomTab(id) {
  return request.delete(`/custom-tab/${id}`)
}

export function addEntriesToTab(tabId, entryIds) {
  return request.post(`/custom-tab/${tabId}/entries`, { entryIds })
}

export function removeEntryFromTab(tabId, entryId) {
  return request.delete(`/custom-tab/${tabId}/entries/${entryId}`)
}
