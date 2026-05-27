import request from '../utils/request'

export function generateDocument(data) {
  return request.post('/documents/generate', data)
}

export function getDocRecords(versionId) {
  return request.get('/documents/records', { params: { versionId } })
}

export function getDocProgress(recordId) {
  return request.get(`/documents/records/${recordId}/progress`)
}

export function downloadDocument(recordId) {
  return request.get(`/documents/records/${recordId}/download`, {
    responseType: 'blob'
  })
}

export function previewDocument(recordId) {
  return request.get(`/documents/records/${recordId}/download`, {
    responseType: 'blob',
    params: { preview: '1' }
  })
}

export function deleteDocRecord(id) {
  return request.delete(`/documents/records/${id}`)
}

export function downloadTestWord() {
  return request.get('/documents/test-word', {
    responseType: 'blob'
  })
}
