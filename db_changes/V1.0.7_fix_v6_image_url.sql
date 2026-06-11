-- V1.0.7 修复 v6 中残留的旧版本图片 URL
-- 问题：1.1.1.1.3.5 模板管理（id=18420）的 col_功能说明 中图片 URL 仍指向 v3

-- 修复 data_entry 中残留的旧版本图片 URL（v6 中的数据应全部指向 v6）
UPDATE data_entry
SET col_功能说明 = REPLACE(col_功能说明, '/api/images/file/3/', '/api/images/file/6/')
WHERE version_id = 6
  AND col_功能说明 LIKE '%/api/images/file/3/%';

UPDATE data_entry
SET col_功能说明 = REPLACE(col_功能说明, '/api/images/file/4/', '/api/images/file/6/')
WHERE version_id = 6
  AND col_功能说明 LIKE '%/api/images/file/4/%';

UPDATE data_entry
SET col_功能说明 = REPLACE(col_功能说明, '/api/images/file/5/', '/api/images/file/6/')
WHERE version_id = 6
  AND col_功能说明 LIKE '%/api/images/file/5/%';

UPDATE data_entry
SET col_控标点截图1 = REPLACE(col_控标点截图1, '/api/images/file/3/', '/api/images/file/6/')
WHERE version_id = 6
  AND col_控标点截图1 LIKE '%/api/images/file/3/%';

UPDATE data_entry
SET col_控标点截图2 = REPLACE(col_控标点截图2, '/api/images/file/3/', '/api/images/file/6/')
WHERE version_id = 6
  AND col_控标点截图2 LIKE '%/api/images/file/3/%';

UPDATE data_entry
SET col_控标点截图3 = REPLACE(col_控标点截图3, '/api/images/file/3/', '/api/images/file/6/')
WHERE version_id = 6
  AND col_控标点截图3 LIKE '%/api/images/file/3/%';
