SET NAMES utf8mb4;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET character_set_results = utf8mb4;
SET collation_connection = utf8mb4_0900_ai_ci;

USE shop;

-- 秒杀场次表
CREATE TABLE IF NOT EXISTS `seckill_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `seckill_price` DECIMAL(12,2) NOT NULL COMMENT '秒杀价',
  `total_stock` INT NOT NULL COMMENT '总库存',
  `sold_stock` INT NOT NULL DEFAULT 0 COMMENT '已售库存',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间',
  `per_user_limit` INT NOT NULL DEFAULT 1 COMMENT '每人限购数量',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_start_end` (`start_time`, `end_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='秒杀场次';

-- 秒杀token表（排队校验用，短期有效）
CREATE TABLE IF NOT EXISTS `seckill_token` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `token` VARCHAR(64) NOT NULL COMMENT 'token字符串',
  `session_id` BIGINT NOT NULL COMMENT '场次ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `expire_at` DATETIME NOT NULL COMMENT '过期时间',
  `used` TINYINT NOT NULL DEFAULT 0 COMMENT '0未使用 1已使用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`),
  KEY `idx_session_user` (`session_id`, `user_id`),
  KEY `idx_expire` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='秒杀token';

-- 订单主表增加 order_type 字段和 seckill_session_id 字段
ALTER TABLE `order_main`
  ADD COLUMN `order_type` VARCHAR(20) NOT NULL DEFAULT 'normal' COMMENT '订单类型 normal=普通 seckill=秒杀' AFTER `order_no`,
  ADD COLUMN `seckill_session_id` BIGINT DEFAULT NULL COMMENT '秒杀场次ID' AFTER `order_type`;

ALTER TABLE `order_main`
  ADD KEY `idx_order_type` (`order_type`),
  ADD KEY `idx_seckill_session` (`seckill_session_id`);

-- 插入示例秒杀场次（3个进行中/即将开始的场次）
INSERT INTO `seckill_session` (`product_id`, `seckill_price`, `total_stock`, `sold_stock`, `start_time`, `end_time`, `per_user_limit`, `status`)
SELECT 1, 1999.00, 10, 0, DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_ADD(NOW(), INTERVAL 1 HOUR), 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `seckill_session` WHERE `id` = 1);

INSERT INTO `seckill_session` (`product_id`, `seckill_price`, `total_stock`, `sold_stock`, `start_time`, `end_time`, `per_user_limit`, `status`)
SELECT 2, 3999.00, 5, 0, DATE_SUB(NOW(), INTERVAL 5 MINUTE), DATE_ADD(NOW(), INTERVAL 2 HOUR), 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `seckill_session` WHERE `id` = 2);

INSERT INTO `seckill_session` (`product_id`, `seckill_price`, `total_stock`, `sold_stock`, `start_time`, `end_time`, `per_user_limit`, `status`)
SELECT 3, 899.00, 20, 0, DATE_ADD(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 3 HOUR), 2, 1
WHERE NOT EXISTS (SELECT 1 FROM `seckill_session` WHERE `id` = 3);

INSERT INTO `seckill_session` (`product_id`, `seckill_price`, `total_stock`, `sold_stock`, `start_time`, `end_time`, `per_user_limit`, `status`)
SELECT 4, 1299.00, 15, 0, DATE_ADD(NOW(), INTERVAL 5 MINUTE), DATE_ADD(NOW(), INTERVAL 4 HOUR), 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `seckill_session` WHERE `id` = 4);
