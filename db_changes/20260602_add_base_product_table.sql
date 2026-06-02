-- 创建产品分类表（L3层级）
CREATE TABLE IF NOT EXISTS base_product (
    id BIGSERIAL PRIMARY KEY,
    version_id BIGINT NOT NULL,
    domain_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_product_version ON base_product(version_id);
CREATE INDEX IF NOT EXISTS idx_product_domain ON base_product(domain_id);
CREATE INDEX IF NOT EXISTS idx_product_sort ON base_product(version_id, domain_id, sort_order);

-- data_entry 表新增 product_id 字段
ALTER TABLE data_entry ADD COLUMN IF NOT EXISTS product_id BIGINT;

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_entry_product ON data_entry(product_id);
