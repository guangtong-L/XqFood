-- ============================================================
-- V4: 用户反馈表（意见反馈 / 投诉 / Bug 报告）
-- 应用商店审核必备能力，对接前端 /pages/help/index 的反馈框
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_feedback` (
    `id`           BIGINT        NOT NULL                COMMENT '反馈ID（雪花）',
    `user_id`      BIGINT        NOT NULL                COMMENT '用户ID',
    `content`      VARCHAR(1000) NOT NULL                COMMENT '反馈内容',
    `contact`      VARCHAR(64)   DEFAULT NULL            COMMENT '联系方式（手机/微信号，选填）',
    `category`     VARCHAR(32)   NOT NULL DEFAULT 'general' COMMENT 'general/bug/suggestion/business',
    `status`       VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/RESOLVED/IGNORED',
    `reply`        VARCHAR(1000) DEFAULT NULL            COMMENT '客服回复',
    `replied_at`   DATETIME      DEFAULT NULL            COMMENT '回复时间',
    `client_info`  JSON          DEFAULT NULL            COMMENT '设备/版本/平台等环境信息',
    `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_created` (`user_id`, `created_at`),
    KEY `idx_status_created` (`status`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户反馈';
