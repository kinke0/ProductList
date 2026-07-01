-- 创建产品分类L1表（统计分类）
CREATE TABLE IF NOT EXISTS base_product_l1 (
    id BIGSERIAL PRIMARY KEY,
    version_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_product_l1_version ON base_product_l1(version_id);
CREATE INDEX IF NOT EXISTS idx_product_l1_sort ON base_product_l1(version_id, sort_order);

-- 创建产品分类L2表（核心业务方向）
CREATE TABLE IF NOT EXISTS base_product_l2 (
    id BIGSERIAL PRIMARY KEY,
    version_id BIGINT NOT NULL,
    l1_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_product_l2_version ON base_product_l2(version_id);
CREATE INDEX IF NOT EXISTS idx_product_l2_l1 ON base_product_l2(l1_id);
CREATE INDEX IF NOT EXISTS idx_product_l2_sort ON base_product_l2(version_id, l1_id, sort_order);

-- 修改base_product表，添加l1_id和l2_id字段
ALTER TABLE base_product ADD COLUMN IF NOT EXISTS l1_id BIGINT;
ALTER TABLE base_product ADD COLUMN IF NOT EXISTS l2_id BIGINT;

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_product_l1 ON base_product(l1_id);
CREATE INDEX IF NOT EXISTS idx_product_l2 ON base_product(l2_id);
