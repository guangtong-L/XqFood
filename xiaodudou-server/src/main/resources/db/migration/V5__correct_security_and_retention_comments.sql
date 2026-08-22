-- 仅修正文档性 COMMENT，不改变字段类型、可空性、索引或业务数据。
-- V1 已发布且必须保持 checksum 不变，因此通过增量迁移纠正历史中的不实安全口径。

ALTER TABLE `t_user`
    MODIFY COLUMN `phone` VARCHAR(20) DEFAULT NULL
        COMMENT '敏感个人信息，当前未做字段级静态加密，生产启用前必须整改';

ALTER TABLE `t_ai_call_log`
    MODIFY COLUMN `audit_status` TINYINT DEFAULT NULL
        COMMENT '1 通过 0 拦截 NULL 未审核';

ALTER TABLE `t_ai_call_log`
    COMMENT = 'AI 调用日志，留存策略待法务确认';
