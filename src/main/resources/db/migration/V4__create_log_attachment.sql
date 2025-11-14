CREATE TABLE log_attachment (
    attachment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    log_id BIGINT NOT NULL,

    -- '会议纪要.pdf' (用户上传时的原始名字)
    original_filename VARCHAR(255) NOT NULL, 

    -- 'a1b2-uuid-c3d4.pdf' (我们存在服务器上的安全名字)
    stored_filename VARCHAR(255) NOT NULL UNIQUE, 

    file_type VARCHAR(100),
    file_size BIGINT,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- 假设你的日志表叫 'log'，主键是 'log_id'
    -- (如果不是，请修改这里的 'log(log_id)')
    FOREIGN KEY (log_id) REFERENCES log(log_id)
        ON DELETE CASCADE -- 核心：删除日志时，自动删除此条附件记录
);