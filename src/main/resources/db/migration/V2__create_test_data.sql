-- ===============================
-- V2__create_test_data.sql
-- 测试数据插入脚本
-- ===============================

-- 1. 插入部门
INSERT INTO department (org_name)
VALUES 
('技术部'),
('市场部');

-- 2. 插入员工
-- 角色分布：
-- 1 CEO
-- 2 部门经理（分别管理技术部、市场部）
-- 2 团队长（分别在两个部门）
-- 2 员工（各自隶属对应团队）

INSERT INTO employee (org_id, employee_name, phone, gender, email, position, emp_password)
VALUES
-- 系统管理员
(NULL, 'admin', '13900000000', 1, 'admin@test.com', 0, 'admin'),
-- CEO
(NULL, '张伟', '13800000001', 1, 'ceo@test.com', 0, '123456'),
-- 部门经理 - 技术部
(1, '李强', '13800000002', 1, 'tech_mgr@test.com', 1, '123456'),
-- 部门经理 - 市场部
(2, '王芳', '13800000003', 0, 'market_mgr@test.com', 1, '123456'),
-- 技术部团队长
(1, '赵磊', '13800000004', 1, 'tech_lead@test.com', 2, '123456'),
-- 市场部团队长
(2, '陈静', '13800000005', 0, 'market_lead@test.com', 2, '123456'),
-- 技术部员工
(1, '孙浩', '13800000006', 1, 'tech_emp@test.com', 3, '123456'),
-- 市场部员工
(2, '刘颖', '13800000007', 0, 'market_emp@test.com', 3, '123456');

-- 3. 插入团队
INSERT INTO team (org_id, team_name)
VALUES
(1, '技术团队'),
(2, '市场团队');

-- 4. 员工团队关系表
INSERT INTO employee_team (employee_id, team_id, is_leader)
VALUES
-- 技术团队：赵磊为团队长，孙浩为成员
(4, 1, 1),
(6, 1, 0),
-- 市场团队：陈静为团队长，刘颖为成员
(5, 2, 1),
(7, 2, 0);

-- 5. 插入测试项目
INSERT INTO project (title, content, start_time, end_time, project_status, project_priority, sender_id)
VALUES
('测试项目A', '这是一个测试用项目，用于任务演示。', '2025-10-01 09:00:00', '2025-10-30 18:00:00', 1, 2, 2);

-- 6. 插入两个里程碑
INSERT INTO milestone (title, content, project_id, milestone_no)
VALUES
('里程碑一', '初步功能完成', 1, 1),
('里程碑二', '整体测试完成', 1, 2);

-- 7. 插入任务
-- 四个任务：
-- (1) 未完成
-- (2) 已完成
-- (3) 已过期
-- (4) 待审核
INSERT INTO task (title, content, start_time, end_time, task_status, task_priority, assignee_id, sender_id, task_type, created_by_who, milestone_id)
VALUES
-- 未完成任务（十月中旬）
('接口联调', '完成接口对接工作', '2025-10-10 09:00:00', '2025-10-15 18:00:00', 0, 2, 6, 4, 1, 2, 1),
-- 已完成任务
('登录模块开发', '实现用户登录功能', '2025-10-01 09:00:00', '2025-10-05 18:00:00', 1, 1, 6, 4, 1, 2, 1),
-- 已过期任务（结束时间早于当前日期）
('市场调研报告', '提交季度调研报告', '2025-09-25 09:00:00', '2025-10-05 18:00:00', 2, 2, 7, 5, 1, 2, 2),
-- 待审核任务
('广告投放计划', '制定Q4广告计划', '2025-10-12 09:00:00', '2025-10-20 18:00:00', 3, 1, 7, 5, 1, 2, 2);

-- 8. 为每个任务插入日志
INSERT INTO log (employee_id, task_id, created_time, content, emoji, attachment, employee_location)
VALUES
(6, 1, '2025-10-11 10:00:00', '接口对接进展良好。', 1, NULL, '上海'),
(6, 2, '2025-10-03 15:00:00', '登录模块已完成开发并测试通过。', 2, NULL, '上海'),
(7, 3, '2025-10-06 09:00:00', '市场调研报告未按时提交。', 3, NULL, '北京'),
(7, 4, '2025-10-13 14:00:00', '广告计划初稿完成，等待审核。', 1, NULL, '北京');
