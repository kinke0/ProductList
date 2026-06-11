import request from '../utils/request'

export function getUserLogs(userId) {
  return request.get(`/operation-logs/user/${userId}`)
}

export function getAllLogs() {
  return request.get('/operation-logs')
}
