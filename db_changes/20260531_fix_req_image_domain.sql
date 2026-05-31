-- 修正需求图片旧数据的domain混乱值
-- 原因：之前上传参数层级错位，domain被错误赋值为'未分类'/'需求管理'(L1值)，product错位为'需求清单'(L2值)
-- 修正1：domain为L1错误值且product为空的，domain清空
-- 修正2：domain/product同时错位的(编辑/截图流程)，domain修正为L2值，product清空
-- 修正3：URL和path字段同步修正
-- 修正4：文件系统同步移动

-- 1. 修正domain和product字段
UPDATE image_resource
SET domain = '', product = ''
WHERE category = '需求管理'
  AND (domain = '未分类' OR domain = '需求管理')
  AND (product IS NULL OR product = '');

UPDATE image_resource
SET domain = '需求清单', product = ''
WHERE category = '需求管理'
  AND domain = '需求管理' AND product = '需求清单';

-- 2. 修正url: 删除旧的错误中间层级目录
UPDATE image_resource SET url = REPLACE(url, '/需求管理/未分类/', '/需求管理/') WHERE url LIKE '%/需求管理/未分类/%';
UPDATE image_resource SET url = REPLACE(url, '/需求管理/需求管理/', '/需求管理/') WHERE url LIKE '%/需求管理/需求管理/%';

-- 3. 修正path: 同上
UPDATE image_resource SET path = REPLACE(path, '/未分类/', '/') WHERE path LIKE '%/未分类/%';
UPDATE image_resource SET path = REPLACE(path, '/需求管理/需求管理/', '/需求管理/') WHERE path LIKE '%/需求管理/需求管理/%';

-- 4. 文件系统移动 (手动执行)
-- 将旧路径下的文件移动到新路径:
-- uploads/images/需求管理/未分类/* → uploads/images/需求管理/
-- uploads/requirements/需求管理/未分类/* → uploads/requirements/需求管理/
-- uploads/requirements/需求管理/需求管理/* → uploads/requirements/需求管理/
-- uploads/requirements/需求管理/需求管理/需求清单/* → uploads/requirements/需求管理/需求清单/

-- 验证修正结果
SELECT id, filename, category, domain, product, url, path
FROM image_resource
WHERE category = '需求管理'
ORDER BY id;
