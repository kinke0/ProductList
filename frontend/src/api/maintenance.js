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

export function syncFilenames() {
  return request.post('/maintenance/sync-filenames')
}

export function getFilenameSyncStatus() {
  return request.get('/maintenance/sync-filenames-status')
}

export function resetFilenameSync() {
  return request.post('/maintenance/sync-filenames-reset')
}

export function fixImageCardIds() {
  return request.post('/maintenance/fix-image-card-ids')
}

export function getFixIdStatus() {
  return request.get('/maintenance/fix-image-card-ids-status')
}

export function resetFixId() {
  return request.post('/maintenance/fix-image-card-ids-reset')
}
