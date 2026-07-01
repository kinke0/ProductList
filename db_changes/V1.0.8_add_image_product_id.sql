-- V1.0.8: image_resource 新增 product_id 字段，关联 L3 级 data_entry 的 ID
-- 填充 product_id 的操作通过后端"非常规操作"按钮执行（递归查找 L3 祖先）

ALTER TABLE image_resource ADD COLUMN product_id BIGINT;
