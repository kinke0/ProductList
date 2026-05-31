-- 版本管理：新增退回次数字段
ALTER TABLE data_version ADD COLUMN rollback_count INTEGER DEFAULT 0;

-- 需求管理：新增业务域字段
ALTER TABLE req_item ADD COLUMN domain VARCHAR(100);
