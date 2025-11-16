-- 为 notice 表添加 related_id 列，用于存储关联的任务ID或重要事项ID
ALTER TABLE `notice`
ADD COLUMN `related_id` INT NULL COMMENT '关联ID(任务ID/重要事项ID等)';

-- 添加索引以提高查询性能
CREATE INDEX `idx_notice_related_id` ON `notice` (`related_id`);
