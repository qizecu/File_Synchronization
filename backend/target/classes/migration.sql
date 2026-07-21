-- ============================================
-- 迁移脚本：sync_task_file 表新增 user_id 和 file_origin 字段
-- ============================================

ALTER TABLE `sync_task_file`
    ADD COLUMN IF NOT EXISTS `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID' AFTER `error_msg`,
    ADD COLUMN IF NOT EXISTS `file_origin` VARCHAR(16) DEFAULT 'SYNC' COMMENT '来源: SYNC/UPLOAD' AFTER `user_id`;

-- 已有记录的 user_id 保持 NULL，file_origin 默认 SYNC
UPDATE `sync_task_file` SET `file_origin` = 'SYNC' WHERE `file_origin` IS NULL;

-- 添加索引
ALTER TABLE `sync_task_file`
    ADD INDEX IF NOT EXISTS `idx_user_id` (`user_id`),
    ADD INDEX IF NOT EXISTS `idx_file_origin` (`file_origin`);
