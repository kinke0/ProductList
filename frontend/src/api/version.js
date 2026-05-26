import request from '../utils/request'

export function getVersions() {
  return request.get('/versions')
}

export function getReleasedVersions() {
  return request.get('/versions/released')
}

export function createVersion() {
  return request.post('/versions')
}

export function releaseVersion(id) {
  return request.post(`/versions/${id}/release`)
}
