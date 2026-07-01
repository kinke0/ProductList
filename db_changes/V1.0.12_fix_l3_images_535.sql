-- ============================================================
-- V1.0.12 修复 5.3.5 住院医生站章节图片名称不一致
-- ============================================================
-- 问题根因：
--   从服务器下载图片后，物理文件去掉了版本号前缀（如 "5.1.4 " / "5.3.5 "），
--   但数据库 image_resource 表的 stored_name / path / url 仍保留旧前缀，
--   导致数据库记录指向的物理文件不存在（页面显示"图片缺失"）。
--
-- 影响范围：
--   version_id = 3（当前研发版本 1.2），5.3.5 住院医生站章节，共 7 条记录。
--
-- 修复策略：
--   修改数据库记录对齐物理文件（物理文件不动，因为物理文件是服务器下载的真实状态）。
--   每条记录同步更新 stored_name、path、url 三个字段；filename（显示名）不变。
--
-- 验证依据：
--   7 个目标物理文件已在
--   uploads/images/3/5. 智慧医疗/5.3 住院诊疗业务/5.3.5 住院医生站/ 目录下确认存在。
--
-- 执行方式：
--   sqlite3 superpower.db < db_changes/V1.0.12_fix_l3_images_535.sql
-- ============================================================

BEGIN TRANSACTION;

-- id=15063：去掉前缀 "5.1.4 "
UPDATE image_resource SET
  stored_name = '门诊医生站系统_会诊工作台-MDT会诊记录.png',
  path = './uploads/images/3/5. 智慧医疗/5.3 住院诊疗业务/5.3.5 住院医生站/门诊医生站系统_会诊工作台-MDT会诊记录.png',
  url = '/api/images/file/3/5. 智慧医疗/5.3 住院诊疗业务/5.3.5 住院医生站/门诊医生站系统_会诊工作台-MDT会诊记录.png'
WHERE id = 15063;

-- id=15064：去掉前缀 "5.1.4 "
UPDATE image_resource SET
  stored_name = '门诊医生站系统_我的会诊-申请我的会诊.png',
  path = './uploads/images/3/5. 智慧医疗/5.3 住院诊疗业务/5.3.5 住院医生站/门诊医生站系统_我的会诊-申请我的会诊.png',
  url = '/api/images/file/3/5. 智慧医疗/5.3 住院诊疗业务/5.3.5 住院医生站/门诊医生站系统_我的会诊-申请我的会诊.png'
WHERE id = 15064;

-- id=15065：去掉前缀 "5.1.4 "
UPDATE image_resource SET
  stored_name = '门诊医生站系统_我的会诊-我申请的会诊.png',
  path = './uploads/images/3/5. 智慧医疗/5.3 住院诊疗业务/5.3.5 住院医生站/门诊医生站系统_我的会诊-我申请的会诊.png',
  url = '/api/images/file/3/5. 智慧医疗/5.3 住院诊疗业务/5.3.5 住院医生站/门诊医生站系统_我的会诊-我申请的会诊.png'
WHERE id = 15065;

-- id=15066：去掉前缀 "5.1.4 "
UPDATE image_resource SET
  stored_name = '门诊医生站系统_我的会诊-会诊申请查询.png',
  path = './uploads/images/3/5. 智慧医疗/5.3 住院诊疗业务/5.3.5 住院医生站/门诊医生站系统_我的会诊-会诊申请查询.png',
  url = '/api/images/file/3/5. 智慧医疗/5.3 住院诊疗业务/5.3.5 住院医生站/门诊医生站系统_我的会诊-会诊申请查询.png'
WHERE id = 15066;

-- id=15067：去掉前缀 "5.1.4 "
UPDATE image_resource SET
  stored_name = '门诊医生站系统_我的会诊-会诊评价.png',
  path = './uploads/images/3/5. 智慧医疗/5.3 住院诊疗业务/5.3.5 住院医生站/门诊医生站系统_我的会诊-会诊评价.png',
  url = '/api/images/file/3/5. 智慧医疗/5.3 住院诊疗业务/5.3.5 住院医生站/门诊医生站系统_我的会诊-会诊评价.png'
WHERE id = 15067;

-- id=15068：去掉前缀 "5.3.5 "
UPDATE image_resource SET
  stored_name = '住院医生站_医生交接班-班次维护.png',
  path = './uploads/images/3/5. 智慧医疗/5.3 住院诊疗业务/5.3.5 住院医生站/住院医生站_医生交接班-班次维护.png',
  url = '/api/images/file/3/5. 智慧医疗/5.3 住院诊疗业务/5.3.5 住院医生站/住院医生站_医生交接班-班次维护.png'
WHERE id = 15068;

-- id=15069：去掉前缀 "5.3.5 "
UPDATE image_resource SET
  stored_name = '住院医生站_医生交接班-交班列表.png',
  path = './uploads/images/3/5. 智慧医疗/5.3 住院诊疗业务/5.3.5 住院医生站/住院医生站_医生交接班-交班列表.png',
  url = '/api/images/file/3/5. 智慧医疗/5.3 住院诊疗业务/5.3.5 住院医生站/住院医生站_医生交接班-交班列表.png'
WHERE id = 15069;

COMMIT;
