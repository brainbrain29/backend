-- 1. 部门表
CREATE TABLE department (
    org_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    org_name VARCHAR(64) NOT NULL
);

-- 2. 员工表
CREATE TABLE employee (
    employee_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    org_id INT,
    employee_name VARCHAR(64) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    gender TINYINT NOT NULL,
    email VARCHAR(64) NOT NULL,
    position TINYINT NOT NULL,
    emp_password VARCHAR(20) NOT NULL,
    CONSTRAINT fk_employee_department FOREIGN KEY (org_id)
        REFERENCES department(org_id)
        ON DELETE SET NULL
);

-- 3. 团队表
CREATE TABLE team (
    team_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    org_id INT NOT NULL,
    team_name VARCHAR(64) NOT NULL,
    CONSTRAINT fk_team_department FOREIGN KEY (org_id)
        REFERENCES department(org_id)
        ON DELETE CASCADE
);

-- 4. 员工团队关系表
CREATE TABLE employee_team (
    employee_id INT NOT NULL,
    team_id INT NOT NULL,
    is_leader TINYINT NOT NULL,
    PRIMARY KEY (employee_id, team_id),
    CONSTRAINT fk_relation_employee FOREIGN KEY (employee_id)
        REFERENCES employee(employee_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_relation_team FOREIGN KEY (team_id)
        REFERENCES team(team_id)
        ON DELETE CASCADE
);

-- 5. 项目表
CREATE TABLE project (
    project_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(64) NOT NULL,
    content VARCHAR(255),
    start_time DATETIME,
    end_time DATETIME,
    project_status TINYINT NOT NULL,
    project_priority TINYINT NOT NULL,
    sender_id INT NOT NULL,
    CONSTRAINT fk_project_sender FOREIGN KEY (sender_id)
        REFERENCES employee(employee_id)
        ON DELETE CASCADE
);

-- 6. 里程碑表
CREATE TABLE milestone (
    milestone_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(64) NOT NULL,
    content VARCHAR(255),
    project_id INT NOT NULL,
    milestone_no TINYINT NOT NULL,
    CONSTRAINT fk_milestone_project FOREIGN KEY (project_id)
        REFERENCES project(project_id)
        ON DELETE CASCADE
);

-- 7. 任务表
CREATE TABLE task (
    task_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(64) NOT NULL,
    content VARCHAR(255),
    start_time DATETIME,
    end_time DATETIME,
    task_status TINYINT NOT NULL,
    task_priority TINYINT NOT NULL,
    assignee_id INT,
    sender_id INT NOT NULL,
    task_type TINYINT NOT NULL,
    created_by_who TINYINT NOT NULL,
    milestone_id INT,
    CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_id)
        REFERENCES employee(employee_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_task_sender FOREIGN KEY (sender_id)
        REFERENCES employee(employee_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_task_milestone FOREIGN KEY (milestone_id)
        REFERENCES milestone(milestone_id)
        ON DELETE SET NULL
);

-- 8. 日志表
CREATE TABLE log (
    log_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    task_id INT,
    created_time DATETIME NOT NULL,
    content VARCHAR(255) NOT NULL,
    emoji TINYINT NOT NULL,
    attachment VARCHAR(255),
    employee_location VARCHAR(50),
    CONSTRAINT fk_log_employee FOREIGN KEY (employee_id)
        REFERENCES employee(employee_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_log_task FOREIGN KEY (task_id)
        REFERENCES task(task_id)
        ON DELETE SET NULL
);

-- 9. 重要事项表
CREATE TABLE important_matter (
    matter_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    content VARCHAR(255) NOT NULL,
    deadline DATETIME NOT NULL,
    assignee_id INT NOT NULL,
    matter_status TINYINT NOT NULL,
    matter_priority TINYINT NOT NULL,
    serial_num TINYINT NOT NULL,
    visible_range TINYINT NOT NULL,
    CONSTRAINT fk_matter_employee FOREIGN KEY (assignee_id)
        REFERENCES employee(employee_id)
        ON DELETE CASCADE
);

-- 10. 通知表
CREATE TABLE notice (
    notice_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sender_id INT NOT NULL,
    notice_type TINYINT NOT NULL,
    content VARCHAR(255),
    created_time DATETIME NOT NULL,
    CONSTRAINT fk_notice_sender FOREIGN KEY (sender_id)
        REFERENCES employee(employee_id)
        ON DELETE CASCADE
);

-- 11. 通知员工关系表
CREATE TABLE notice_employee (
    notice_id INT NOT NULL,
    receiver_id INT NOT NULL,
    notice_status INT NOT NULL,
    PRIMARY KEY (notice_id, receiver_id),
    CONSTRAINT fk_relation_notice FOREIGN KEY (notice_id)
        REFERENCES notice(notice_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_relation_receiver FOREIGN KEY (receiver_id)
        REFERENCES employee(employee_id)
        ON DELETE CASCADE
);
