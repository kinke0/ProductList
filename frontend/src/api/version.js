import request from '../utils/request'

export function getAppVersion() {
  return request.get('/app-version')
}

export function getVersions() {
  return request.get('/versions')
}

export function getReleasedVersions() {
  return request.get('/versions/released')
}

export function createVersion() {
  return request.post('/versions')
}

export function getVersionProgress() {
  return request.get('/versions/progress')
}

export function deleteVersion(id) {
  return request.delete(`/versions/${id}`)
}

export function releaseVersion(id) {
  return request.post(`/versions/${id}/release`)
}

export function rollbackVersion(id) {
  return request.post(`/versions/${id}/rollback`)
}
