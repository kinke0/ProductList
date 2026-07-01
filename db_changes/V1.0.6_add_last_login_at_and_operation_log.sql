ALTER TABLE sys_user ADD COLUMN last_login_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    action VARCHAR(50),
    module VARCHAR(50),
    description VARCHAR(500),
    target_id BIGINT,
    target_type VARCHAR(50),
    ip VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_operation_log_user_id ON operation_log(user_id);
CREATE INDEX idx_operation_log_created_at ON operation_log(created_at);
