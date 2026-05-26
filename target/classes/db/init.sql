-- PostgreSQL 迁移脚本
-- 执行 DDL 前请先创建数据库: CREATE DATABASE superpower;

-- sys_user
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(100),
    role_id BIGINT,
    status INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- sys_role
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- sys_role_menu
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    UNIQUE(role_id, menu_id)
);

-- sys_menu
CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT,
    name VARCHAR(100) NOT NULL,
    permission VARCHAR(100),
    type INTEGER DEFAULT 1,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- data_version
CREATE TABLE IF NOT EXISTS data_version (
    id BIGSERIAL PRIMARY KEY,
    version_no VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'draft',
    released_at TIMESTAMP,
    released_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- data_version_changelog
CREATE TABLE IF NOT EXISTS data_version_changelog (
    id BIGSERIAL PRIMARY KEY,
    version_id BIGINT NOT NULL,
    entry_id BIGINT,
    change_type VARCHAR(20) NOT NULL,
    field_name VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    operated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- data_entry
CREATE TABLE IF NOT EXISTS data_entry (
    id BIGSERIAL PRIMARY KEY,
    version_id BIGINT NOT NULL,
    parent_id BIGINT,
    level INTEGER NOT NULL,
    sort_order INTEGER DEFAULT 0,
    is_leaf BOOLEAN DEFAULT TRUE,
    col_产品系统 VARCHAR(500),
    col_应用角色 VARCHAR(500),
    col_招标参数说明 TEXT,
    col_功能说明 TEXT,
    col_状态 VARCHAR(100),
    col_业务分类 VARCHAR(200),
    col_业务域 VARCHAR(200),
    col_版本划分 VARCHAR(200),
    col_远 VARCHAR(50),
    col_交付工作量人月 VARCHAR(100),
    col_控标点 VARCHAR(50),
    col_控标点截图1 TEXT,
    col_控标点截图2 TEXT,
    col_控标点截图3 TEXT,
    col_控标点文档说明 TEXT,
    col_软著 VARCHAR(500),
    col_备注 TEXT,
    col_智慧医疗 VARCHAR(100),
    col_智慧服务 VARCHAR(100),
    col_智慧管理 VARCHAR(100),
    col_互联互通 VARCHAR(100),
    col_产品系统标识 VARCHAR(100),
    col_模块标识 VARCHAR(100),
    col_其他解决方案标记 VARCHAR(200),
    col_文档维护人员 VARCHAR(100),
    col_产品经理 VARCHAR(100),
    col_父记录 VARCHAR(500),
    col_内部版本 VARCHAR(100),
    col_智能化 VARCHAR(50),
    col_曜 VARCHAR(50),
    col_驰 VARCHAR(50),
    col_FY23 NUMERIC,
    col_FY24 NUMERIC,
    col_FY25 NUMERIC,
    col_FY26 NUMERIC,
    col_FY27 NUMERIC,
    col_FY28 NUMERIC,
    col_FY29 NUMERIC,
    col_研发成本合计 NUMERIC,
    col_销量曜 INTEGER,
    col_销量远 INTEGER,
    col_销量驰 INTEGER,
    col_出厂套价保本 NUMERIC,
    col_负责人 VARCHAR(200),
    col_产品线 VARCHAR(200),
    col_资产类型 VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT
);
