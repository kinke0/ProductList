import request from '../utils/request'

export function getRoles() {
  return request.get('/roles')
}

export function createRole(data) {
  return request.post('/roles', data)
}
