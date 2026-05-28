import request from '../utils/request'

export function getCustomTabs(versionId) {
  return request.get(`/custom-tab/${versionId}`)
}

export function createCustomTab(data) {
  return request.post('/custom-tab', data)
}

export function createCustomTabWithFilter(data) {
  return request.post('/custom-tab/create-with-filter', data)
}

export function deleteCustomTab(id) {
  return request.delete(`/custom-tab/${id}`)
}

export function renameCustomTab(id, name) {
  return request.put(`/custom-tab/${id}`, { name })
}

export function addEntriesToTab(tabId, entryIds) {
  return request.post(`/custom-tab/${tabId}/entries`, { entryIds })
}

export function removeEntryFromTab(tabId, entryId) {
  return request.delete(`/custom-tab/${tabId}/entries/${entryId}`)
}

export function updateCustomTabSort(tabId, sortList) {
  return request.put(`/custom-tab/${tabId}/sort`, sortList)
}
