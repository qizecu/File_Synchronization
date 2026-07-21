-- ============================================
-- 文件同步管理系统 - 数据库建表脚本
-- ============================================

CREATE DATABASE IF NOT EXISTS sync_manager
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE sync_manager;

-- ============================================
-- 1. 存储源配置表（MinIO / OBS 连接信息）
-- ============================================
CREATE TABLE storage_source (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    source_name     VARCHAR(64)  NOT NULL COMMENT '存储源名称',
    source_type     VARCHAR(16)  NOT NULL COMMENT '存储类型: MINIO / OBS',
    endpoint        VARCHAR(255) NOT NULL COMMENT '端点地址',
    access_key      VARCHAR(128) NOT NULL COMMENT '访问密钥',
    secret_key      VARCHAR(128) NOT NULL COMMENT '私有密钥',
    bucket          VARCHAR(64)  NOT NULL COMMENT '桶名称',
    region          VARCHAR(32)  DEFAULT NULL COMMENT '区域（OBS 需要）',
    prefix_path     VARCHAR(255) DEFAULT '/' COMMENT '文件前缀路径',
    enabled         TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用: 0-禁用 1-启用',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除 1-已删除',
    INDEX idx_source_type (source_type)
) ENGINE=InnoDB COMMENT='存储源配置表';

-- ============================================
-- 2. 同步任务表
-- ============================================
CREATE TABLE sync_task (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    task_name       VARCHAR(128) NOT NULL COMMENT '任务名称',
    task_type       VARCHAR(16)  NOT NULL COMMENT '任务类型: FULL(全量) / INCREMENTAL(增量)',
    source_id       BIGINT       NOT NULL COMMENT '关联存储源ID',
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '任务状态: PENDING / RUNNING / SUCCESS / FAILED / PARTIAL',
    total_files     INT          NOT NULL DEFAULT 0 COMMENT '总文件数',
    success_files   INT          NOT NULL DEFAULT 0 COMMENT '成功文件数',
    failed_files    INT          NOT NULL DEFAULT 0 COMMENT '失败文件数',
    skipped_files   INT          NOT NULL DEFAULT 0 COMMENT '跳过文件数',
    current_cursor  VARCHAR(512) DEFAULT NULL COMMENT '断点续传游标（记录当前同步到的文件标记）',
    error_msg       TEXT         DEFAULT NULL COMMENT '错误信息',
    started_at      DATETIME     DEFAULT NULL COMMENT '开始时间',
    completed_at    DATETIME     DEFAULT NULL COMMENT '完成时间',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '软删除',
    INDEX idx_status (status),
    INDEX idx_task_type (task_type),
    INDEX idx_source_id (source_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='同步任务表';

-- ============================================
-- 3. 同步任务文件明细表（MD5 校验 + 重试记录）
-- ============================================
CREATE TABLE sync_task_file (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    task_id         BIGINT       DEFAULT NULL COMMENT '关联同步任务ID（上传记录为NULL）',
    source_path     VARCHAR(512) NOT NULL COMMENT '源端文件路径',
    source_md5      VARCHAR(64)  DEFAULT NULL COMMENT '源端文件 MD5',
    source_size     BIGINT       DEFAULT 0 COMMENT '源端文件大小（字节）',
    target_path     VARCHAR(512) DEFAULT NULL COMMENT '本地目标路径',
    target_md5      VARCHAR(64)  DEFAULT NULL COMMENT '本地文件 MD5',
    target_size     BIGINT       DEFAULT 0 COMMENT '本地文件大小（字节）',
    file_status     VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '文件状态: PENDING / SUCCESS / FAILED / SKIPPED',
    retry_count     INT          NOT NULL DEFAULT 0 COMMENT '已重试次数',
    error_msg       TEXT         DEFAULT NULL COMMENT '错误信息',
    user_id         BIGINT       DEFAULT NULL COMMENT '操作用户ID',
    file_origin     VARCHAR(16)  DEFAULT 'SYNC' COMMENT '来源: SYNC/UPLOAD',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '软删除',
    INDEX idx_task_id (task_id),
    INDEX idx_file_status (file_status),
    INDEX idx_user_id (user_id),
    INDEX idx_file_origin (file_origin)
) ENGINE=InnoDB COMMENT='同步任务文件明细表';

-- ============================================
-- 4. 通知配置表（已废弃，手动执行 DROP 即可删除）
-- -- DROP TABLE IF EXISTS notify_config;
-- ============================================
/*
CREATE TABLE notify_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    config_name     VARCHAR(64)  NOT NULL COMMENT '配置名称',
    notify_type     VARCHAR(16)  NOT NULL COMMENT '通知类型: DINGTALK / WECOM',
    webhook_url     VARCHAR(512) NOT NULL COMMENT 'Webhook 地址',
    secret          VARCHAR(128) DEFAULT NULL COMMENT '签名密钥（可选）',
    enabled         TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '软删除'
) ENGINE=InnoDB COMMENT='通知配置表';
*/

-- ============================================
-- 5. 通知日志表（已废弃，手动执行 DROP 即可删除）
-- -- DROP TABLE IF EXISTS notify_log;
-- ============================================
/*
CREATE TABLE notify_log (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    task_id          BIGINT       DEFAULT NULL COMMENT '关联同步任务ID',
    notify_config_id BIGINT       DEFAULT NULL COMMENT '关联通知配置ID',
    notify_type      VARCHAR(16)  NOT NULL COMMENT '通知类型',
    notify_status    VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '通知状态: PENDING / SUCCESS / FAILED',
    request_body     TEXT         DEFAULT NULL COMMENT '请求内容',
    response_body    TEXT         DEFAULT NULL COMMENT '响应内容',
    error_msg        TEXT         DEFAULT NULL COMMENT '错误信息',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    is_deleted       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '软删除',
    INDEX idx_task_id (task_id)
) ENGINE=InnoDB COMMENT='通知日志表';
*/

-- ============================================
-- 6. 系统用户表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(200) NOT NULL COMMENT '密码（BCrypt 加密）',
    nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    status      TINYINT      DEFAULT 1  COMMENT '状态：1-启用 0-禁用',
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN-管理员 USER-普通用户',
    created_at  DATETIME     DEFAULT NULL COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT NULL COMMENT '更新时间',
    is_deleted  TINYINT      DEFAULT 0  COMMENT '软删除标记',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ============================================
-- 初始化测试数据
-- ============================================
INSERT INTO storage_source (source_name, source_type, endpoint, access_key, secret_key, bucket, enabled)
VALUES ('本地 MinIO 测试', 'MINIO', 'http://localhost:9000', 'minioadmin', 'minioadmin', 'test-images', 1);
