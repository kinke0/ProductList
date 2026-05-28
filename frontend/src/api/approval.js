import request from '../utils/request'

export function approveEntry(entryId, action, comment) {
  return request.post(`/approval/${entryId}`, { action, comment })
}

export function getApprovalLogs(entryId) {
  return request.get(`/approval/${entryId}/logs`)
}
