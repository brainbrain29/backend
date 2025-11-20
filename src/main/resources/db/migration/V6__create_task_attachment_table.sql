-- 创建任务附件表
-- 结构与 log_attachment 相同，但关联到 task 表

CREATE TABLE task_attachment (
    attachment_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '附件ID',
    task_id INT NOT NULL COMMENT '关联的任务ID',
    original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_filename VARCHAR(255) NOT NULL UNIQUE COMMENT '存储在服务器上的安全文件名',
    file_type VARCHAR(100) COMMENT '文件MIME类型',
    file_size BIGINT COMMENT '文件大小(字节)',
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    uploaded_by INT COMMENT '上传者ID',
    FOREIGN KEY (task_id) REFERENCES task(task_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务附件表';
