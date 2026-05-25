-- ============================================================
-- V3: 订单表（会员购买）
-- 字段设计兼容微信支付 + 后期支付宝
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_order` (
    `id`              BIGINT       NOT NULL                COMMENT '订单ID（雪花）',
    `out_trade_no`    VARCHAR(64)  NOT NULL                COMMENT '商户订单号（幂等键，对外）',
    `user_id`         BIGINT       NOT NULL                COMMENT '用户ID',
    `package_code`    VARCHAR(32)  NOT NULL                COMMENT '套餐编码 POSTPARTUM_CARD/WEANING_YEAR/CHILD_YEAR',
    `package_name`    VARCHAR(64)  NOT NULL                COMMENT '套餐名称（快照，防止后期改价）',
    `amount_fen`      INT          NOT NULL                COMMENT '订单金额（分）',
    `vip_level`       TINYINT      NOT NULL                COMMENT '会员等级 1月子 2辅食 3儿童',
    `valid_days`      INT          NOT NULL                COMMENT '有效天数',
    `status`          VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PAID/CANCELLED/REFUNDED/EXPIRED',
    `pay_channel`     VARCHAR(16)  DEFAULT NULL            COMMENT 'wechat/alipay/mock',
    `pay_transaction_id` VARCHAR(64) DEFAULT NULL          COMMENT '支付平台流水号',
    `paid_at`         DATETIME     DEFAULT NULL            COMMENT '支付完成时间',
    `expire_at`       DATETIME     DEFAULT NULL            COMMENT '订单过期时间（默认 +30 分钟）',
    `refunded_at`     DATETIME     DEFAULT NULL            COMMENT '退款时间',
    `refund_reason`   VARCHAR(256) DEFAULT NULL,
    `extra`           JSON         DEFAULT NULL            COMMENT '扩展（优惠券、推荐人等）',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_out_trade_no` (`out_trade_no`),
    KEY `idx_user_status` (`user_id`, `status`, `created_at`),
    KEY `idx_status_expire` (`status`, `expire_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='订单表';
