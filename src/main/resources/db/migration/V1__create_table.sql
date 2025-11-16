-- ===============================
-- V1__init_schema.sql
-- 数据库表结构初始化脚本
-- ===============================

-- ========================================
-- 第一部分: 创建所有表(不含外键)
-- ========================================

-- 1. 部门表
CREATE TABLE department (
    org_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    org_name VARCHAR(64) NOT NULL COMMENT '部门名称'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 2. 员工表
CREATE TABLE employee (
    employee_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    org_id INT COMMENT '所属部门ID',
    employee_name VARCHAR(64) NOT NULL COMMENT '员工姓名',
    phone VARCHAR(20) NOT NULL UNIQUE COMMENT '手机号',
    gender TINYINT NOT NULL COMMENT '0:女 1:男',
    email VARCHAR(64) NOT NULL UNIQUE COMMENT '邮箱',
    position TINYINT NOT NULL COMMENT '0:CEO 1:部门经理 2:团队长 3:员工',
    emp_password VARCHAR(20) NOT NULL COMMENT '密码',
    avatar_url VARCHAR(255) COMMENT '头像URL,管理员用于Web端,其他员工用于移动端'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';

-- 3. 团队表
CREATE TABLE team (
    team_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    org_id INT NOT NULL COMMENT '所属部门ID',
    team_name VARCHAR(64) NOT NULL COMMENT '团队名称'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队表';

-- 4. 员工团队关系表
CREATE TABLE employee_team (
    employee_id INT NOT NULL COMMENT '员工ID',
    team_id INT NOT NULL COMMENT '团队ID',
    is_leader TINYINT NOT NULL COMMENT '0:成员 1:团队长',
    PRIMARY KEY (employee_id, team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工团队关系表';

-- 5. 项目表
CREATE TABLE project (
    project_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(64) NOT NULL COMMENT '项目标题',
    content VARCHAR(255) COMMENT '项目描述',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    project_status TINYINT NOT NULL COMMENT '0:待处理 1:进行中 2:已完成',
    sender_id INT NOT NULL COMMENT '创建人ID',
    team_id INT COMMENT '负责团队ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- 6. 里程碑表
CREATE TABLE milestone (
    milestone_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(64) NOT NULL COMMENT '里程碑标题',
    content VARCHAR(255) COMMENT '里程碑描述',
    project_id INT NOT NULL COMMENT '所属项目ID',
    milestone_no TINYINT NOT NULL COMMENT '里程碑序号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='里程碑表';

-- 7. 任务表
CREATE TABLE task (
    task_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(64) NOT NULL COMMENT '任务标题',
    content VARCHAR(255) COMMENT '任务描述',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    task_status TINYINT NOT NULL COMMENT '0:待处理 1:进行中 2:已完成',
    task_priority TINYINT NOT NULL COMMENT '0:低 1:中 2:高',
    assignee_id INT COMMENT '负责人ID',
    sender_id INT NOT NULL COMMENT '创建人ID',
    task_type TINYINT NOT NULL COMMENT '任务类型',
    milestone_id INT COMMENT '关联里程碑ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

-- 8. 日志表
CREATE TABLE log (
    log_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL COMMENT '员工ID',
    task_id INT COMMENT '关联任务ID',
    created_time DATETIME NOT NULL COMMENT '创建时间',
    content VARCHAR(255) NOT NULL COMMENT '日志内容',
    emoji TINYINT NOT NULL COMMENT '心情表情',
    attachment VARCHAR(255) COMMENT '附件路径',
    employee_location VARCHAR(50) COMMENT '员工位置',
    employee_position TINYINT COMMENT '员工职位'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志表';

-- 9. 日志附件表
CREATE TABLE log_attachment (
    attachment_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '附件ID',
    log_id INT NOT NULL COMMENT '关联的日志ID',
    original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_filename VARCHAR(255) NOT NULL UNIQUE COMMENT '存储在服务器上的安全文件名',
    file_type VARCHAR(100) COMMENT '文件MIME类型',
    file_size BIGINT COMMENT '文件大小(字节)',
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    uploaded_by INT COMMENT '上传者ID',
    FOREIGN KEY (log_id) REFERENCES log(log_id)
        ON DELETE CASCADE -- 核心：删除日志时，自动删除此条附件记录
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志附件表';

-- 10. 重要事项表
CREATE TABLE important_matter (
    matter_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL COMMENT '事项标题',
    content VARCHAR(500) NOT NULL COMMENT '事项内容',
    department_id INT COMMENT '发布部门ID',
    publish_time DATETIME COMMENT '发布时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='重要事项表';

-- 11. 重要任务表
CREATE TABLE important_task (
    task_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL COMMENT '负责人ID',
    task_content VARCHAR(255) NOT NULL COMMENT '任务内容',
    deadline DATETIME NOT NULL COMMENT '截止时间',
    task_status TINYINT NOT NULL DEFAULT 0 COMMENT '0:待处理 1:进行中 2:已完成',
    task_priority TINYINT NOT NULL DEFAULT 1 COMMENT '0:低 1:中 2:高',
    serial_num TINYINT NOT NULL COMMENT '序号1-10',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='重要任务表';

-- 12. 通知表
CREATE TABLE notice (
    notice_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sender_id INT NOT NULL COMMENT '发送人ID',
    notice_type TINYINT NOT NULL COMMENT '通知类型',
    content VARCHAR(255) COMMENT '通知内容',
    created_time DATETIME NOT NULL COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- 13. 通知员工关系表
CREATE TABLE notice_employee (
    notice_id INT NOT NULL COMMENT '通知ID',
    receiver_id INT NOT NULL COMMENT '接收人ID',
    notice_status INT NOT NULL COMMENT '通知状态: 0:未读 1:已读',
    PRIMARY KEY (notice_id, receiver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知员工关系表';

-- 14. 刷新令牌表
CREATE TABLE refresh_token (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL COMMENT '用户ID',
    refresh_token VARCHAR(255) NOT NULL COMMENT '刷新令牌',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/USED/REVOKED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    expires_at DATETIME NOT NULL COMMENT '过期时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='刷新令牌表';

-- ========================================
-- 第二部分: 添加外键约束
-- ========================================

ALTER TABLE employee
    ADD CONSTRAINT fk_employee_department FOREIGN KEY (org_id)
        REFERENCES department(org_id) ON DELETE SET NULL;

ALTER TABLE team
    ADD CONSTRAINT fk_team_department FOREIGN KEY (org_id)
        REFERENCES department(org_id) ON DELETE CASCADE;

ALTER TABLE employee_team
    ADD CONSTRAINT fk_et_employee FOREIGN KEY (employee_id)
        REFERENCES employee(employee_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_et_team FOREIGN KEY (team_id)
        REFERENCES team(team_id) ON DELETE CASCADE;

ALTER TABLE project
    ADD CONSTRAINT fk_project_sender FOREIGN KEY (sender_id)
        REFERENCES employee(employee_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_project_team FOREIGN KEY (team_id)
        REFERENCES team(team_id) ON DELETE SET NULL;

ALTER TABLE milestone
    ADD CONSTRAINT fk_milestone_project FOREIGN KEY (project_id)
        REFERENCES project(project_id) ON DELETE CASCADE;

ALTER TABLE task
    ADD CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_id)
        REFERENCES employee(employee_id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_task_sender FOREIGN KEY (sender_id)
        REFERENCES employee(employee_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_task_milestone FOREIGN KEY (milestone_id)
        REFERENCES milestone(milestone_id) ON DELETE SET NULL;

ALTER TABLE log
    ADD CONSTRAINT fk_log_employee FOREIGN KEY (employee_id)
        REFERENCES employee(employee_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_log_task FOREIGN KEY (task_id)
        REFERENCES task(task_id) ON DELETE SET NULL;

ALTER TABLE log_attachment
    ADD CONSTRAINT fk_log_att_log FOREIGN KEY (log_id)
        REFERENCES log(log_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_log_att_employee FOREIGN KEY (uploaded_by)
        REFERENCES employee(employee_id) ON DELETE SET NULL;

ALTER TABLE important_matter
    ADD CONSTRAINT fk_matter_department FOREIGN KEY (department_id)
        REFERENCES department(org_id) ON DELETE SET NULL;

ALTER TABLE important_task
    ADD CONSTRAINT fk_imp_task_employee FOREIGN KEY (employee_id)
        REFERENCES employee(employee_id) ON DELETE CASCADE;

ALTER TABLE notice
    ADD CONSTRAINT fk_notice_sender FOREIGN KEY (sender_id)
        REFERENCES employee(employee_id) ON DELETE CASCADE;

ALTER TABLE notice_employee
    ADD CONSTRAINT fk_ne_notice FOREIGN KEY (notice_id)
        REFERENCES notice(notice_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_ne_receiver FOREIGN KEY (receiver_id)
        REFERENCES employee(employee_id) ON DELETE CASCADE;

ALTER TABLE refresh_token
    ADD CONSTRAINT fk_token_user FOREIGN KEY (user_id)
        REFERENCES employee(employee_id) ON DELETE CASCADE;

-- ========================================
-- 第三部分: 创建索引
-- ========================================

CREATE INDEX idx_employee_org ON employee(org_id);
CREATE INDEX idx_team_org ON team(org_id);
CREATE INDEX idx_project_sender ON project(sender_id);
CREATE INDEX idx_project_team ON project(team_id);
CREATE INDEX idx_milestone_project ON milestone(project_id);
CREATE INDEX idx_task_assignee ON task(assignee_id);
CREATE INDEX idx_task_sender ON task(sender_id);
CREATE INDEX idx_task_milestone ON task(milestone_id);
CREATE INDEX idx_log_employee ON log(employee_id);
CREATE INDEX idx_log_task ON log(task_id);
CREATE INDEX idx_log_created ON log(created_time);
CREATE INDEX idx_log_att_log ON log_attachment(log_id);
CREATE INDEX idx_imp_task_emp ON important_task(employee_id);
CREATE INDEX idx_imp_task_serial ON important_task(serial_num);
CREATE INDEX idx_notice_sender ON notice(sender_id);
CREATE INDEX idx_token_user ON refresh_token(user_id);
CREATE INDEX idx_token_value ON refresh_token(refresh_token);