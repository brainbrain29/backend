-- 创建十大重要任务表
CREATE TABLE important_task (
    task_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    task_content VARCHAR(255) NOT NULL,
    deadline DATETIME NOT NULL,
    task_status TINYINT NOT NULL DEFAULT 0 COMMENT '0:待处理 1:进行中 2:已完成',
    task_priority TINYINT NOT NULL DEFAULT 1 COMMENT '0:低 1:中 2:高',
    serial_num TINYINT NOT NULL COMMENT '序号1-10',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_important_task_employee FOREIGN KEY (employee_id)
        REFERENCES employee(employee_id)
        ON DELETE CASCADE
);

-- 添加索引
CREATE INDEX idx_employee_id ON important_task(employee_id);
CREATE INDEX idx_serial_num ON important_task(serial_num);
