-- V1.0.12: 新增 col_intelligent 列（智能化标签）
-- TEXT 类型，默认空字符串，"1" 表示勾选智能化，空字符串表示未勾选
ALTER TABLE data_entry ADD COLUMN col_intelligent TEXT DEFAULT '';
