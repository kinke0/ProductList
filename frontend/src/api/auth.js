import request from '../utils/request'

export function login(username, password) {
  return request.post('/auth/login', { username, password })
}

export function getCurrentUser() {
  return request.get('/auth/me')
}

export function register(username, password, nickname) {
  return request.post('/auth/register', { username, password, nickname })
}

export function changePassword(oldPassword, newPassword) {
  return request.put('/auth/password', { oldPassword, newPassword })
}
