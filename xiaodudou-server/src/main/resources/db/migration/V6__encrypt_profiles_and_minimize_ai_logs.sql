-- V6: 敏感画像静态加密与 AI 日志最小化。
-- V1-V5 为不可变历史迁移，本版本只做向前兼容的增量变更。

ALTER TABLE `t_user_profile`
    MODIFY COLUMN `stage_type` VARCHAR(32) DEFAULT NULL
        COMMENT '历史明文字段，仅用于迁移；新写入必须为 NULL',
    ADD COLUMN `encrypted_payload` LONGTEXT DEFAULT NULL
        COMMENT 'AES-GCM 加密画像，格式含版本前缀、随机 IV 和认证标签' AFTER `health_notes`,
    ADD COLUMN `encryption_key_version` VARCHAR(16) DEFAULT NULL
        COMMENT '画像加密密钥版本' AFTER `encrypted_payload`;

ALTER TABLE `t_ai_call_log`
    ADD COLUMN `input_count` INT DEFAULT NULL
        COMMENT '非敏感输入项数量' AFTER `input_payload`,
    ADD COLUMN `output_count` INT DEFAULT NULL
        COMMENT '非敏感输出项数量' AFTER `output_payload`,
    ADD COLUMN `recipe_ids` JSON DEFAULT NULL
        COMMENT '推荐结果食谱 ID 集合，不含用户输入与画像' AFTER `output_count`;

-- 清理历史日志中的原始请求、低熵摘要和完整输出，避免继续保留敏感画像。
UPDATE `t_ai_call_log`
SET `input_hash` = NULL,
    `input_payload` = NULL,
    `output_payload` = NULL;

ALTER TABLE `t_user_profile`
    COMMENT = '用户阶段画像；新数据仅写 encrypted_payload，旧明文字段迁移后清空';

ALTER TABLE `t_ai_call_log`
    COMMENT = 'AI 最小化运行日志；保留周期由生产配置决定且不超过 365 天';
