-- ===============================
-- V4__add_mbti_column.sql
-- 为 employee 表添加 MBTI 字段并更新测试数据
-- ===============================

-- 添加 MBTI 字段
ALTER TABLE employee ADD COLUMN mbti VARCHAR(4) COMMENT 'MBTI性格类型（如INTJ、ENFP等）';

-- 为现有测试数据添加 MBTI 值
UPDATE employee SET mbti = 'INTJ' WHERE employee_id = 1;  -- admin
UPDATE employee SET mbti = 'ENTJ' WHERE employee_id = 2;  -- 张伟 (CEO)
UPDATE employee SET mbti = 'ISTJ' WHERE employee_id = 3;  -- 李强 (技术部经理)
UPDATE employee SET mbti = 'ENFP' WHERE employee_id = 4;  -- 王芳 (市场部经理)
UPDATE employee SET mbti = 'ESFJ' WHERE employee_id = 5;  -- 刘洋 (人力资源部经理)
UPDATE employee SET mbti = 'ESTJ' WHERE employee_id = 6;  -- 陈敏 (财务部经理)
UPDATE employee SET mbti = 'INTP' WHERE employee_id = 7;  -- 赵磊 (技术团队长)
UPDATE employee SET mbti = 'ENFJ' WHERE employee_id = 8;  -- 陈静 (市场团队长)
UPDATE employee SET mbti = 'ISFJ' WHERE employee_id = 9;  -- 周杰 (人力资源团队长)
UPDATE employee SET mbti = 'ISTJ' WHERE employee_id = 10; -- 吴娜 (财务团队长)
UPDATE employee SET mbti = 'ISTP' WHERE employee_id = 11; -- 孙浩 (技术部员工)
UPDATE employee SET mbti = 'ESFP' WHERE employee_id = 12; -- 刘颖 (市场部员工)
UPDATE employee SET mbti = 'INFJ' WHERE employee_id = 13; -- 郑强 (人力资源部员工)
UPDATE employee SET mbti = 'ISFP' WHERE employee_id = 14; -- 黄丽 (财务部员工)
UPDATE employee SET mbti = 'ENTP' WHERE employee_id = 15; -- 张三 (技术部员工)
UPDATE employee SET mbti = 'INFP' WHERE employee_id = 16; -- 李四 (技术部员工)
UPDATE employee SET mbti = 'ESTP' WHERE employee_id = 17; -- 王五 (市场部员工)
UPDATE employee SET mbti = 'ENFP' WHERE employee_id = 18; -- 赵六 (市场部员工)
UPDATE employee SET mbti = 'ISFJ' WHERE employee_id = 19; -- 孙七 (人力资源部员工)
UPDATE employee SET mbti = 'ESFJ' WHERE employee_id = 20; -- 周八 (财务部员工)
