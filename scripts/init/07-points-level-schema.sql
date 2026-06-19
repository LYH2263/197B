-- 积分与等级体系数据库 schema

SET NAMES utf8mb4;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET character_set_results = utf8mb4;
SET collation_connection = utf8mb4_0900_ai_ci;

USE shop;

-- 用户表扩展：积分余额、累计消费、等级
ALTER TABLE `user`
  ADD COLUMN IF NOT EXISTS `points` INT NOT NULL DEFAULT 0 COMMENT '积分余额' AFTER `role`,
  ADD COLUMN IF NOT EXISTS `total_consume` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计消费金额' AFTER `points`,
  ADD COLUMN IF NOT EXISTS `level` TINYINT NOT NULL DEFAULT 1 COMMENT '等级 1=Lv1 2=Lv2 3=Lv3 4=Lv4 5=Lv5' AFTER `total_consume`;

-- 订单表扩展：积分抵扣金额、使用积分数
ALTER TABLE `order_main`
  ADD COLUMN IF NOT EXISTS `points_discount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '积分抵扣金额' AFTER `discount_amount`,
  ADD COLUMN IF NOT EXISTS `points_used` INT NOT NULL DEFAULT 0 COMMENT '使用积分数' AFTER `points_discount`,
  ADD COLUMN IF NOT EXISTS `points_earned` INT NOT NULL DEFAULT 0 COMMENT '本单获得积分数' AFTER `points_used`;

-- 积分流水表
CREATE TABLE IF NOT EXISTS `points_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `type` TINYINT NOT NULL COMMENT '类型 1=收货获得 2=下单抵扣 3=兑换扣减 4=管理员调整 5=退款退回',
  `change_points` INT NOT NULL COMMENT '变动积分（正数=增加，负数=扣减）',
  `balance` INT NOT NULL COMMENT '变动后余额',
  `related_type` VARCHAR(32) DEFAULT NULL COMMENT '关联类型 order/redeem/admin',
  `related_id` BIGINT DEFAULT NULL COMMENT '关联ID',
  `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分流水';

-- 积分兑换商品表
CREATE TABLE IF NOT EXISTS `points_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL COMMENT '商品名称',
  `image` VARCHAR(256) DEFAULT NULL COMMENT '商品图片',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
  `points_required` INT NOT NULL COMMENT '所需积分',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '库存',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0下架 1上架',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分兑换商品';

-- 积分兑换单表
CREATE TABLE IF NOT EXISTS `points_exchange_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `exchange_no` VARCHAR(32) NOT NULL COMMENT '兑换单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `points_product_id` BIGINT NOT NULL COMMENT '兑换商品ID',
  `product_name` VARCHAR(128) NOT NULL COMMENT '商品名称（快照）',
  `product_image` VARCHAR(256) DEFAULT NULL COMMENT '商品图片（快照）',
  `points_cost` INT NOT NULL COMMENT '消耗积分',
  `receiver_name` VARCHAR(64) DEFAULT NULL COMMENT '收货人',
  `receiver_phone` VARCHAR(20) DEFAULT NULL COMMENT '收货电话',
  `receiver_address` VARCHAR(256) DEFAULT NULL COMMENT '收货地址',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1已发货 2已完成 3已取消',
  `express_company` VARCHAR(64) DEFAULT NULL COMMENT '快递公司',
  `express_no` VARCHAR(64) DEFAULT NULL COMMENT '快递单号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exchange_no` (`exchange_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分兑换单';

-- 初始化积分兑换商品
INSERT IGNORE INTO `points_product` (`id`, `name`, `image`, `description`, `points_required`, `stock`, `status`, `sort_order`) VALUES
(1, '10元无门槛优惠券', '/images/default-product.svg', '全场通用无门槛优惠券，满0.01元可用', 1000, 999, 1, 1),
(2, '50元优惠券(满200可用)', '/images/default-product.svg', '满200元减50元优惠券', 3000, 500, 1, 2),
(3, '定制马克杯', '/images/default-product.svg', '精美定制陶瓷马克杯，包邮到家', 5000, 200, 1, 3),
(4, '品牌T恤', '/images/shirt-mens.png', '高品质纯棉T恤，多色可选', 8000, 100, 1, 4),
(5, '蓝牙耳机', '/images/default-product.svg', '高品质无线蓝牙耳机，音质出众', 20000, 50, 1, 5);
