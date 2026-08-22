-- 用户行为事实化：保留历史 cook，不伪造历史餐次/份数；收藏仅保留最早一条。
ALTER TABLE `t_user_recipe_action`
    ADD COLUMN `action_date` DATE DEFAULT NULL COMMENT '行为业务日期，cook 使用；历史数据按 created_at 回填',
    ADD COLUMN `meal_type` VARCHAR(16) DEFAULT NULL COMMENT 'breakfast/lunch/dinner/snack；历史 cook 未知时为空',
    ADD COLUMN `servings` DECIMAL(5,2) DEFAULT NULL COMMENT '实际记录份数；历史 cook 未知时为空',
    ADD COLUMN `idempotency_key` VARCHAR(128) DEFAULT NULL COMMENT '用户行为幂等键';

-- 删除重复收藏，仅保留 created_at 最早、同时间 id 最小的记录。
DELETE duplicate_row
FROM `t_user_recipe_action` duplicate_row
JOIN `t_user_recipe_action` retained
  ON retained.user_id = duplicate_row.user_id
 AND retained.recipe_id = duplicate_row.recipe_id
 AND retained.action = 'favorite'
 AND duplicate_row.action = 'favorite'
 AND (retained.created_at < duplicate_row.created_at
      OR (retained.created_at = duplicate_row.created_at AND retained.id < duplicate_row.id));

UPDATE `t_user_recipe_action`
SET `idempotency_key` = CONCAT('favorite:', recipe_id)
WHERE action = 'favorite';

UPDATE `t_user_recipe_action`
SET `action_date` = DATE(created_at),
    `idempotency_key` = CONCAT('legacy:', id)
WHERE action = 'cook';

-- share 或其他历史行为同样保留，以原 id 构造永不碰撞的历史键。
UPDATE `t_user_recipe_action`
SET `idempotency_key` = CONCAT('legacy:', id)
WHERE idempotency_key IS NULL;

ALTER TABLE `t_user_recipe_action`
    MODIFY COLUMN `idempotency_key` VARCHAR(128) NOT NULL COMMENT '用户行为幂等键',
    ADD UNIQUE KEY `uk_user_action_idempotency` (`user_id`, `action`, `idempotency_key`),
    ADD KEY `idx_user_cook_date` (`user_id`, `action`, `action_date`, `created_at`);
