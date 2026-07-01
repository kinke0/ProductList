-- 历史变更汇总脚本
-- 本文件汇总了项目迭代过程中所有的数据库表结构变更
-- 执行前提：先执行 src/main/resources/db/init.sql 创建基础表

-- 1. 新增 data_entry 表缺失字段（审批状态、分类关联）
ALTER TABLE data_entry ADD COLUMN approval_status VARCHAR(20) DEFAULT '待提交';
ALTER TABLE data_entry ADD COLUMN category_id BIGINT;
ALTER TABLE data_entry ADD COLUMN domain_id BIGINT;

-- 2. 新增业务分类表
CREATE TABLE IF NOT EXISTS base_category (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    version_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. 新增业务域表
CREATE TABLE IF NOT EXISTS base_domain (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    version_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. 新增通用选项表（解决方案/状态/应用角色等）
CREATE TABLE IF NOT EXISTS sys_option (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type VARCHAR(50) NOT NULL,
    value VARCHAR(200) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    version_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. 新增自定义清单表
CREATE TABLE IF NOT EXISTS custom_tab (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    version_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. 新增自定义清单条目关联表
CREATE TABLE IF NOT EXISTS custom_tab_entry (
    custom_tab_id BIGINT NOT NULL,
    entry_id BIGINT NOT NULL,
    PRIMARY KEY (custom_tab_id, entry_id)
);

-- 7. 新增审批日志表
CREATE TABLE IF NOT EXISTS approval_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    entry_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    comment TEXT,
    operated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. 新增文档生成记录表
CREATE TABLE IF NOT EXISTS doc_gen_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    version_id BIGINT NOT NULL,
    template_name VARCHAR(200),
    file_path VARCHAR(500),
    status VARCHAR(20) DEFAULT 'pending',
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 9. 新增图片资源表
CREATE TABLE IF NOT EXISTS image_resource (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_name VARCHAR(500) NOT NULL,
    original_name VARCHAR(500),
    file_path VARCHAR(1000) NOT NULL,
    file_size BIGINT DEFAULT 0,
    mime_type VARCHAR(100),
    directory VARCHAR(500),
    uploaded_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
