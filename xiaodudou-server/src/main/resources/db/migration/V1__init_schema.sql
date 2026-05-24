-- ============================================================
-- 小肚兜 AI - 初始化数据库 schema
-- 版本: V1 (M1 阶段)
-- 仅包含 M1 必须的表，更多表在 V2、V3 增量迁移
-- ============================================================

SET NAMES utf8mb4;

-- ---------------- 1. 用户表 ----------------
CREATE TABLE IF NOT EXISTS `t_user` (
    `id`          BIGINT       NOT NULL                COMMENT '用户ID（雪花）',
    `wx_openid`   VARCHAR(64)  NOT NULL                COMMENT '微信 openid',
    `wx_unionid`  VARCHAR(64)  DEFAULT NULL            COMMENT '微信 unionid',
    `nickname`    VARCHAR(64)  DEFAULT NULL            COMMENT '昵称',
    `avatar_url`  VARCHAR(512) DEFAULT NULL            COMMENT '头像 URL',
    `phone`       VARCHAR(20)  DEFAULT NULL            COMMENT '手机号（加密存储）',
    `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态 1正常 0禁用',
    `vip_level`   TINYINT      NOT NULL DEFAULT 0      COMMENT '会员等级 0 免费 1 月子卡 2 辅食卡 3 儿童卡',
    `vip_expire_at` DATETIME   DEFAULT NULL            COMMENT '会员到期时间',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wx_openid` (`wx_openid`),
    KEY `idx_phone` (`phone`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- ---------------- 2. 阶段画像表 ----------------
CREATE TABLE IF NOT EXISTS `t_user_profile` (
    `id`                BIGINT       NOT NULL,
    `user_id`           BIGINT       NOT NULL                COMMENT '用户ID',
    `stage_type`        VARCHAR(32)  NOT NULL                COMMENT 'PREPARE/PREGNANCY/POSTPARTUM/WEANING/CHILD',
    `pregnancy_week`    INT          DEFAULT NULL            COMMENT '孕周（仅孕期）',
    `postpartum_day`    INT          DEFAULT NULL            COMMENT '产后天数（仅月子）',
    `delivery_type`     VARCHAR(16)  DEFAULT NULL            COMMENT 'natural 顺产 / cesarean 剖宫',
    `feeding_type`      VARCHAR(16)  DEFAULT NULL            COMMENT 'breast 母乳 / mixed 混合 / formula 奶粉',
    `baby_birth_date`   DATE         DEFAULT NULL            COMMENT '宝宝出生日期（辅食/儿童期）',
    `allergies`         JSON         DEFAULT NULL            COMMENT '过敏源数组 ["egg","milk"]',
    `dislikes`          JSON         DEFAULT NULL            COMMENT '忌口数组',
    `health_notes`      VARCHAR(512) DEFAULT NULL            COMMENT '健康备注',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户阶段画像';

-- ---------------- 3. 食材主数据 ----------------
CREATE TABLE IF NOT EXISTS `t_ingredient` (
    `id`              BIGINT       NOT NULL,
    `name`            VARCHAR(64)  NOT NULL                  COMMENT '标准名（如"番茄"）',
    `alias`           JSON         DEFAULT NULL              COMMENT '别名 ["西红柿"]',
    `category`        VARCHAR(32)  NOT NULL                  COMMENT '蔬菜/水果/肉禽/海鲜/蛋奶/主食/豆制品/调味料/其他',
    `nutrition_per_100g` JSON      DEFAULT NULL              COMMENT '每100g营养，键为 kcal/protein/calcium/iron 等',
    `allergen_tags`   JSON         DEFAULT NULL              COMMENT '过敏标签 ["egg"]',
    `postpartum_taboo` TINYINT     NOT NULL DEFAULT 0        COMMENT '月子忌口 1是',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='食材主数据';

-- ---------------- 4. 食谱表 ----------------
CREATE TABLE IF NOT EXISTS `t_recipe` (
    `id`           BIGINT       NOT NULL,
    `title`        VARCHAR(128) NOT NULL                     COMMENT '菜名',
    `cover_url`    VARCHAR(512) DEFAULT NULL,
    `cook_minutes` INT          DEFAULT NULL,
    `difficulty`   TINYINT      DEFAULT NULL                 COMMENT '1-5',
    `stage_tags`   JSON         DEFAULT NULL                 COMMENT '阶段标签 ["postpartum_early","lactation"]',
    `nutrition`    JSON         DEFAULT NULL                 COMMENT '整菜营养（每份）',
    `steps`        JSON         DEFAULT NULL                 COMMENT '步骤数组 [{step:1,desc:"...",timer:60}]',
    `description`  VARCHAR(512) DEFAULT NULL,
    `status`       TINYINT      NOT NULL DEFAULT 1           COMMENT '1上架 0下架',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='食谱';

-- ---------------- 5. 食谱-食材关联 ----------------
CREATE TABLE IF NOT EXISTS `t_recipe_ingredient` (
    `id`            BIGINT      NOT NULL,
    `recipe_id`     BIGINT      NOT NULL,
    `ingredient_id` BIGINT      NOT NULL,
    `quantity`      VARCHAR(64) DEFAULT NULL                 COMMENT '"200g" "2个"',
    `is_optional`   TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_recipe_ingredient` (`recipe_id`, `ingredient_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='食谱-食材关联';

-- ---------------- 6. 用户行为：收藏/打卡 ----------------
CREATE TABLE IF NOT EXISTS `t_user_recipe_action` (
    `id`         BIGINT      NOT NULL,
    `user_id`    BIGINT      NOT NULL,
    `recipe_id`  BIGINT      NOT NULL,
    `action`     VARCHAR(16) NOT NULL                        COMMENT 'favorite/cook/share',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_action` (`user_id`, `action`, `created_at`),
    KEY `idx_recipe` (`recipe_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户食谱行为';

-- ---------------- 7. AI 调用日志（合规留存 ≥ 180 天）----------------
CREATE TABLE IF NOT EXISTS `t_ai_call_log` (
    `id`             BIGINT      NOT NULL,
    `user_id`        BIGINT      DEFAULT NULL,
    `endpoint`       VARCHAR(32) NOT NULL                    COMMENT 'recognize/recommend/nutrition',
    `input_hash`     VARCHAR(64) DEFAULT NULL,
    `input_payload`  JSON        DEFAULT NULL,
    `output_payload` JSON        DEFAULT NULL,
    `model_version`  VARCHAR(64) DEFAULT NULL,
    `cost_ms`        INT         DEFAULT NULL,
    `cost_tokens`    INT         DEFAULT NULL,
    `audit_status`   TINYINT     DEFAULT NULL                COMMENT '1 通过 0 拦截',
    `status`         TINYINT     NOT NULL DEFAULT 1          COMMENT '1 成功 0 失败',
    `created_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `created_at`),
    KEY `idx_endpoint_time` (`endpoint`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='AI 调用日志（合规留存）';
