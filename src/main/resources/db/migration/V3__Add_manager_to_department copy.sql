-- 为 department 表添加 manager_id 列，用于存储经理的员工ID
ALTER TABLE `department`
ADD COLUMN `manager_id` INT NULL COMMENT '部门经理ID';

-- 为 manager_id 列添加外键约束，使其关联到 employee 表的 employee_id
ALTER TABLE `department`
ADD CONSTRAINT `fk_department_manager`
FOREIGN KEY (`manager_id`) REFERENCES `employee` (`employee_id`)
ON DELETE SET NULL ON UPDATE CASCADE;