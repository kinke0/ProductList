-- 新增需求管理模块表
-- req_item: 需求条目
CREATE TABLE IF NOT EXISTS req_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    req_no VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT '提出',
    priority VARCHAR(10) DEFAULT '中',
    category VARCHAR(100),
    created_by BIGINT,
    assigned_to BIGINT,
    reject_reason TEXT,
    released_version VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- req_log: 需求操作日志
CREATE TABLE IF NOT EXISTS req_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    req_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    comment TEXT,
    operated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
