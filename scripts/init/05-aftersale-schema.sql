-- 售后模块表结构

-- 售后主表
CREATE TABLE IF NOT EXISTS `after_sale` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `after_sale_no` VARCHAR(32) NOT NULL COMMENT '售后单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `order_item_id` BIGINT NOT NULL COMMENT '订单明细ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(128) NOT NULL COMMENT '商品名称',
  `product_image` VARCHAR(256) DEFAULT NULL COMMENT '商品图片',
  `type` TINYINT NOT NULL COMMENT '类型 1=退货退款 2=换货 3=仅退款',
  `reason` VARCHAR(128) NOT NULL COMMENT '售后原因',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '问题描述',
  `voucher_images` VARCHAR(1024) DEFAULT NULL COMMENT '凭证图片，逗号分隔最多3张',
  `original_price` DECIMAL(12,2) NOT NULL COMMENT '商品原价（订单明细单价）',
  `original_quantity` INT NOT NULL COMMENT '原购买数量',
  `original_total_amount` DECIMAL(12,2) NOT NULL COMMENT '订单行实付金额',
  `refund_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '实际退款金额（partial，≤实付）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0=待审核 1=已通过 2=已拒绝 3=待寄回 4=仓库确认 5=已退款 6=已换货发出 7=已取消',
  `reject_reason` VARCHAR(256) DEFAULT NULL COMMENT '拒绝原因',
  `return_company` VARCHAR(64) DEFAULT NULL COMMENT '退货快递公司',
  `return_tracking_no` VARCHAR(64) DEFAULT NULL COMMENT '退货物流单号',
  `exchange_company` VARCHAR(64) DEFAULT NULL COMMENT '换货发出快递公司',
  `exchange_tracking_no` VARCHAR(64) DEFAULT NULL COMMENT '换货发出物流单号',
  `supplement_id` BIGINT DEFAULT NULL COMMENT '换货补款单ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_after_sale_no` (`after_sale_no`),
  KEY `idx_user` (`user_id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_order_item` (`order_item_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='售后主表';

-- 售后操作日志（时间线）
CREATE TABLE IF NOT EXISTS `after_sale_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `after_sale_id` BIGINT NOT NULL COMMENT '售后ID',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID 空=系统',
  `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人名称（冗余）',
  `operator_role` VARCHAR(20) DEFAULT NULL COMMENT '操作人角色 user/admin/system',
  `action` VARCHAR(64) NOT NULL COMMENT '操作动作 CREATE/APPROVE/REJECT/USER_SHIP/WAREHOUSE_CONFIRM/REFUND/EXCHANGE_SHIP/CANCEL/PAY_SUPPLEMENT',
  `status_from` TINYINT DEFAULT NULL COMMENT '变更前状态',
  `status_to` TINYINT DEFAULT NULL COMMENT '变更后状态',
  `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注/详情',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_after_sale` (`after_sale_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='售后操作日志';

-- 换货补款单
CREATE TABLE IF NOT EXISTS `after_sale_supplement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `supplement_no` VARCHAR(32) NOT NULL COMMENT '补款单号',
  `after_sale_id` BIGINT NOT NULL COMMENT '售后ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `price_diff` DECIMAL(12,2) NOT NULL COMMENT '差价金额（正数=需补款）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0=待支付 1=已支付 2=已取消',
  `paid_at` DATETIME DEFAULT NULL COMMENT '支付时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_supplement_no` (`supplement_no`),
  KEY `idx_after_sale` (`after_sale_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='换货补款单';
