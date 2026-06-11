-- V1.0.7 修复图片目录层级：将 image_resource.product 从 L4+ 修正为 L3
-- 同时修正 data_entry.col_功能说明 中引用的旧图片 URL 路径
-- 物理文件迁移需要配合 shell 脚本执行

-- ============================================================
-- Step 1: 建立 product → L3 映射临时表
-- ============================================================

-- 通过 parent_id 递归链：L4+ → L3 映射
CREATE TEMP TABLE IF NOT EXISTS tmp_product_l3_map (
    product TEXT,
    category TEXT,
    domain TEXT,
    l3_product TEXT,
    l3_category TEXT,
    l3_domain TEXT
);

WITH RECURSIVE chain(id, level, col_ps, col_cat, col_dom, l3_ps, l3_cat, l3_dom) AS (
    SELECT id, level, "col_产品系统", "col_业务分类", "col_业务域", "col_产品系统", "col_业务分类", "col_业务域"
    FROM data_entry WHERE level = 3 AND version_id = 3
    UNION ALL
    SELECT d.id, d.level, d."col_产品系统", d."col_业务分类", d."col_业务域", c.l3_ps, c.l3_cat, c.l3_dom
    FROM data_entry d JOIN chain c ON d.parent_id = c.id AND d.version_id = 3
)
INSERT INTO tmp_product_l3_map (product, category, domain, l3_product, l3_category, l3_domain)
SELECT DISTINCT c.col_ps, c.col_cat, c.col_dom, c.l3_ps, c.l3_cat, c.l3_dom
FROM chain c
WHERE c.level > 3 AND c.col_ps IS NOT NULL AND c.col_ps != '';

-- 1.1.3.* → 1.1.1.* 映射（migration 图片用了不同编码体系）
-- 通过纯产品名称匹配
INSERT OR IGNORE INTO tmp_product_l3_map (product, category, domain, l3_product, l3_category, l3_domain)
SELECT DISTINCT ir.product, ir.category, ir.domain, c.l3_ps, c.l3_cat, c.l3_dom
FROM image_resource ir
JOIN (
    WITH RECURSIVE chain2(id, level, col_ps, col_cat, col_dom, l3_ps, l3_cat, l3_dom) AS (
        SELECT id, level, "col_产品系统", "col_业务分类", "col_业务域", "col_产品系统", "col_业务分类", "col_业务域"
        FROM data_entry WHERE level = 3 AND version_id = 3
        UNION ALL
        SELECT d.id, d.level, d."col_产品系统", d."col_业务分类", d."col_业务域", c.l3_ps, c.l3_cat, c.l3_dom
        FROM data_entry d JOIN chain2 c ON d.parent_id = c.id AND d.version_id = 3
    )
    SELECT col_ps, col_cat, col_dom, l3_ps, l3_cat, l3_dom FROM chain2 WHERE level > 3
) c ON c.col_cat = ir.category
    AND SUBSTR(ir.product, INSTR(ir.product, ' ')) = SUBSTR(c.col_ps, INSTR(c.col_ps, ' '))
WHERE ir.version_id = 3
    AND ir.product LIKE '1.1.3.%'
    AND ir.product NOT IN (SELECT product FROM tmp_product_l3_map WHERE category = ir.category AND domain = ir.domain);

-- 5.3.4.7.1 盘点管理 / 5.3.4.8.1 月结管理 → 5.3.4 住院药房系统, domain 5.2→5.3
INSERT OR IGNORE INTO tmp_product_l3_map (product, category, domain, l3_product, l3_category, l3_domain)
VALUES
    ('5.3.4.7.1 盘点管理', '5. 智慧医疗', '5.2 急诊诊疗业务', '5.3.4 住院药房系统', '5. 智慧医疗', '5.3 住院诊疗业务'),
    ('5.3.4.8.1 月结管理', '5. 智慧医疗', '5.2 急诊诊疗业务', '5.3.4 住院药房系统', '5. 智慧医疗', '5.3 住院诊疗业务');

-- ============================================================
-- Step 2: 修复 image_resource（v3, v4, v5）
-- ============================================================

-- 保存修复前的旧记录用于后续物理文件迁移和 URL 替换
CREATE TEMP TABLE IF NOT EXISTS tmp_image_fix_log (
    id INTEGER,
    version_id INTEGER,
    old_product TEXT,
    old_category TEXT,
    old_domain TEXT,
    old_url TEXT,
    old_path TEXT,
    new_product TEXT,
    new_category TEXT,
    new_domain TEXT,
    new_url TEXT
);

INSERT INTO tmp_image_fix_log (id, version_id, old_product, old_category, old_domain, old_url, old_path, new_product, new_category, new_domain, new_url)
SELECT ir.id, ir.version_id, ir.product, ir.category, ir.domain, ir.url, ir.path,
    m.l3_product,
    COALESCE(m.l3_category, ir.category),
    COALESCE(m.l3_domain, ir.domain),
    REPLACE(REPLACE(REPLACE(ir.url,
        '/' || ir.product || '/',
        '/' || m.l3_product || '/'),
        '/' || ir.domain || '/' || ir.product,
        '/' || COALESCE(m.l3_domain, ir.domain) || '/' || m.l3_product),
        ir.category || '/' || ir.domain || '/' || ir.product,
        COALESCE(m.l3_category, ir.category) || '/' || COALESCE(m.l3_domain, ir.domain) || '/' || m.l3_product)
FROM image_resource ir
JOIN tmp_product_l3_map m ON m.product = ir.product AND m.category = ir.category AND m.domain = ir.domain
WHERE ir.version_id IN (3, 4, 5)
    AND ir.category IS NOT NULL AND ir.category != '';

-- 更新 image_resource
UPDATE image_resource SET
    product = (SELECT l.new_product FROM tmp_image_fix_log l WHERE l.id = image_resource.id),
    category = (SELECT l.new_category FROM tmp_image_fix_log l WHERE l.id = image_resource.id),
    domain = (SELECT l.new_domain FROM tmp_image_fix_log l WHERE l.id = image_resource.id),
    url = (SELECT l.new_url FROM tmp_image_fix_log l WHERE l.id = image_resource.id)
WHERE id IN (SELECT id FROM tmp_image_fix_log);

-- ============================================================
-- Step 3: 修复 data_entry.col_功能说明 中的图片 URL
-- ============================================================

-- 对每条有旧 URL 的 entry，替换 URL 中的 product/domain/category 部分
-- 需要逐条处理（SQLite 不支持 UPDATE with JOIN + REPLACE 的复杂模式）

CREATE TEMP TABLE IF NOT EXISTS tmp_entry_url_fix (
    entry_id INTEGER,
    version_id INTEGER,
    old_url_fragment TEXT,
    new_url_fragment TEXT
);

INSERT INTO tmp_entry_url_fix (entry_id, version_id, old_url_fragment, new_url_fragment)
SELECT DISTINCT de.id, de.version_id,
    l.old_category || '/' || l.old_domain || '/' || l.old_product,
    l.new_category || '/' || l.new_domain || '/' || l.new_product
FROM data_entry de
JOIN tmp_image_fix_log l ON l.version_id = de.version_id
WHERE de.version_id IN (3, 4, 5)
    AND de."col_功能说明" LIKE '%' || l.old_category || '/' || l.old_domain || '/' || l.old_product || '%';

-- 执行替换
UPDATE data_entry SET "col_功能说明" = REPLACE("col_功能说明",
    (SELECT old_url_fragment FROM tmp_entry_url_fix WHERE entry_id = data_entry.id LIMIT 1),
    (SELECT new_url_fragment FROM tmp_entry_url_fix WHERE entry_id = data_entry.id LIMIT 1))
WHERE id IN (SELECT entry_id FROM tmp_entry_url_fix);

-- ============================================================
-- Step 4: 验证
-- ============================================================
SELECT '=== 修复验证 ===' AS info;
SELECT 'image_resource 修复数' AS label, COUNT(*) AS cnt FROM tmp_image_fix_log;
SELECT 'entry URL 修复数' AS label, COUNT(*) AS cnt FROM tmp_entry_url_fix;
SELECT '剩余非L3 product' AS label, COUNT(*) AS cnt FROM image_resource ir
WHERE ir.version_id IN (3,4,5) AND ir.category IS NOT NULL AND ir.category != ''
    AND ir.product NOT IN (
        SELECT DISTINCT "col_产品系统" FROM data_entry WHERE version_id=3 AND level=3 AND "col_产品系统" != ''
    );

-- 清理临时表（保留 tmp_image_fix_log 供物理文件迁移脚本使用）
DROP TABLE IF EXISTS tmp_product_l3_map;
DROP TABLE IF EXISTS tmp_entry_url_fix;
