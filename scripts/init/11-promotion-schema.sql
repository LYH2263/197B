-- 满减促销模块数据库 schema

USE shop;

-- 满减活动主表
CREATE TABLE IF NOT EXISTS `promotion` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(128) NOT NULL COMMENT '活动名称',
  `start_time` DATETIME NOT NULL COMMENT '活动开始时间',
  `end_time` DATETIME NOT NULL COMMENT '活动结束时间',
  `scope_type` TINYINT NOT NULL DEFAULT 1 COMMENT '适用范围 1全场 2指定分类',
  `applicable_category` BIGINT DEFAULT NULL COMMENT '指定分类ID scope_type=2时生效',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0关闭 1启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_time` (`start_time`, `end_time`),
  KEY `idx_category` (`applicable_category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='满减活动';

-- 满减档位表（支持多档满减，取最优一档）
CREATE TABLE IF NOT EXISTS `promotion_tier` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `promotion_id` BIGINT NOT NULL COMMENT '满减活动ID',
  `threshold` DECIMAL(12,2) NOT NULL COMMENT '满减门槛金额',
  `discount` DECIMAL(12,2) NOT NULL COMMENT '减免金额',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序（档位升序）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_promotion_id` (`promotion_id`),
  KEY `idx_threshold` (`threshold`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='满减档位';

-- 订单满减活动快照表（下单时锁定活动快照）
CREATE TABLE IF NOT EXISTS `order_promotion_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `promotion_id` BIGINT NOT NULL COMMENT '满减活动ID',
  `promotion_name` VARCHAR(128) NOT NULL COMMENT '活动名称快照',
  `tier_id` BIGINT NOT NULL COMMENT '使用的档位ID',
  `tier_threshold` DECIMAL(12,2) NOT NULL COMMENT '档位门槛快照',
  `tier_discount` DECIMAL(12,2) NOT NULL COMMENT '档位减免快照',
  `promotion_discount` DECIMAL(12,2) NOT NULL COMMENT '本单满减实际减免金额',
  `scope_type` TINYINT NOT NULL COMMENT '适用范围快照',
  `applicable_category` BIGINT DEFAULT NULL COMMENT '适用分类快照',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_promotion_id` (`promotion_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单满减活动快照';

-- 订单主表增加满减字段
ALTER TABLE `order_main`
  ADD COLUMN IF NOT EXISTS `promotion_id` BIGINT DEFAULT NULL COMMENT '使用的满减活动ID',
  ADD COLUMN IF NOT EXISTS `promotion_discount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '满减减免金额',
  ADD KEY IF NOT EXISTS `idx_promotion_id` (`promotion_id`);
