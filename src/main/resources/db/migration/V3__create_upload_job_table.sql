CREATE TABLE upload_job (
    upload_job_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NULL,
    file_size BIGINT NULL,
    local_path VARCHAR(1024) NOT NULL,
    oss_object_key VARCHAR(1024) NULL,
    status VARCHAR(50) NOT NULL,
    error_message VARCHAR(2000) NULL,
    created_time DATETIME NOT NULL,
    updated_time DATETIME NOT NULL,
    INDEX idx_upload_job_user_id (user_id),
    INDEX idx_upload_job_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
