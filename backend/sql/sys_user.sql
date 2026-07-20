-- 系统用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(200) NOT NULL COMMENT '密码（BCrypt 加密）',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `status`      TINYINT      DEFAULT 1  COMMENT '状态：1-启用 0-禁用',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN-管理员 USER-普通用户',
    `created_at`  DATETIME     DEFAULT NULL COMMENT '创建时间',
    `updated_at`  DATETIME     DEFAULT NULL COMMENT '更新时间',
    `is_deleted`  TINYINT      DEFAULT 0  COMMENT '软删除标记',
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';
