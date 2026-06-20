-- 优惠券模块数据库 schema

USE shop;

-- 优惠券模板表
CREATE TABLE IF NOT EXISTS `coupon` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(128) NOT NULL COMMENT '券名称',
  `type` TINYINT NOT NULL COMMENT '券类型 1满减 2折扣 3无门槛立减',
  `threshold` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛 0=无门槛',
  `face_value` DECIMAL(12,2) DEFAULT NULL COMMENT '面额（满减/立减使用）',
  `discount_rate` DECIMAL(3,2) DEFAULT NULL COMMENT '折扣率 0.00-1.00（折扣券使用）',
  `valid_start` DATETIME NOT NULL COMMENT '有效期开始',
  `valid_end` DATETIME NOT NULL COMMENT '有效期结束',
  `total_quantity` INT NOT NULL COMMENT '发放总量',
  `claimed_quantity` INT NOT NULL DEFAULT 0 COMMENT '已领取数量',
  `used_quantity` INT NOT NULL DEFAULT 0 COMMENT '已使用数量',
  `per_user_limit` INT NOT NULL DEFAULT 1 COMMENT '每人限领数量',
  `applicable_category` BIGINT DEFAULT NULL COMMENT '适用分类ID NULL=全场',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0作废 1正常',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_valid_time` (`valid_start`, `valid_end`),
  KEY `idx_category` (`applicable_category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券模板';

-- 用户优惠券表
CREATE TABLE IF NOT EXISTS `user_coupon` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `coupon_id` BIGINT NOT NULL COMMENT '优惠券模板ID',
  `code` VARCHAR(32) NOT NULL COMMENT '券码（唯一）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0未使用 1已使用 2已过期 3已作废',
  `used_order_id` BIGINT DEFAULT NULL COMMENT '使用订单ID',
  `used_at` DATETIME DEFAULT NULL COMMENT '使用时间',
  `claimed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `expired_at` DATETIME NOT NULL COMMENT '过期时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_coupon_id` (`coupon_id`),
  KEY `idx_user_coupon` (`user_id`, `coupon_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户优惠券';

-- 订单主表增加优惠券字段
ALTER TABLE `order_main`
  ADD COLUMN `coupon_id` BIGINT DEFAULT NULL COMMENT '使用的优惠券ID',
  ADD COLUMN `discount_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '优惠券抵扣金额';

ALTER TABLE `order_main`
  ADD KEY `idx_coupon_id` (`coupon_id`);
