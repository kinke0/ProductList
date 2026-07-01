-- ============================================================
-- V1.0.2 数据库表结构变更
-- 日期: 2026-06-01
-- 说明: V1.0.2 新增字段和索引，用于支持业务分类关联、业务域关联、审批状态、层级关系修复
-- 注意: 数据修复（level/parent_id/is_leaf/domain_id/category_id 的数据修正）通过 Java API
--       PUT /api/data/fix-hierarchy/{versionId} 执行，不在本脚本中
-- ============================================================

-- 1. data_entry 表新增 category_id 字段（关联 base_category 业务分类）
ALTER TABLE data_entry ADD COLUMN category_id bigint;

-- 2. data_entry 表新增 domain_id 字段（关联 base_domain 业务域）
ALTER TABLE data_entry ADD COLUMN domain_id bigint;

-- 3. data_entry 表新增 approval_status 字段（审批状态，默认'待提交'）
ALTER TABLE data_entry ADD COLUMN approval_status VARCHAR(20) DEFAULT '待提交';

-- 4. 新增索引：按版本+层级查询
CREATE INDEX IF NOT EXISTS idx_entry_version_level ON data_entry(version_id, level);

-- 5. 新增索引：按版本+父节点+排序查询
CREATE INDEX IF NOT EXISTS idx_entry_version_parent ON data_entry(version_id, parent_id, sort_order);

-- 6. 新增索引：按版本+层级+父节点+排序查询
CREATE INDEX IF NOT EXISTS idx_entry_version_level_parent ON data_entry(version_id, level, parent_id, sort_order);

-- 7. 新增索引：按版本+业务分类+业务域查询
CREATE INDEX IF NOT EXISTS idx_entry_biz ON data_entry(version_id, col_业务分类, col_业务域);