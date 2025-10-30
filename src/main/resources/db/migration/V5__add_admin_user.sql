-- 添加系统管理员账号
INSERT INTO employee (org_id, employee_name, phone, gender, email, position, emp_password)
VALUES (NULL, 'admin', '13900000000', 1, 'admin@test.com', 0, 'admin')
ON DUPLICATE KEY UPDATE employee_name = 'admin';
