-- V3__create_token.sql
CREATE TABLE IF NOT EXISTS refresh_token (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    refresh_token VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE/USED/REVOKED
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES employee(employee_id)
);

-- 可选：添加索引
CREATE INDEX idx_user_id ON refresh_token(user_id);
CREATE INDEX idx_refresh_token ON refresh_token(refresh_token);
