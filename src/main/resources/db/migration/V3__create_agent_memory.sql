CREATE TABLE agent_memory (
    memory_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    memory_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_time DATETIME NOT NULL,
    updated_time DATETIME NOT NULL,
    INDEX idx_agent_memory_employee_updated (employee_id, updated_time DESC),
    CONSTRAINT fk_agent_memory_employee FOREIGN KEY (employee_id)
        REFERENCES employee(employee_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent long-term memory table';
