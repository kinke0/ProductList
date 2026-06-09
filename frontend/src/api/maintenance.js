import request from '../utils/request'

export function migrateImageAll() {
  return request.post('/maintenance/migrate-image')
}

export function migrateStep(step) {
  return request.post(`/maintenance/migrate-step/${step}`)
}

export function getMigrationStatus() {
  return request.get('/maintenance/migration-status')
}

export function resetMigration() {
  return request.post('/maintenance/migration-reset')
}
