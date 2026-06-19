-- 物流追踪系统数据库 schema
-- 包含 shipment(物流发货表)、shipment_trace(物流轨迹表)、delivery_issue(异常签收表)、delivery_urge(催单记录表)

SET NAMES utf8mb4;
SET character_set_client = utf8mb4;

USE shop;

-- 物流发货表
CREATE TABLE IF NOT EXISTS `shipment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
  `express_company_code` VARCHAR(32) NOT NULL COMMENT '快递公司编码（字典表）',
  `express_company_name` VARCHAR(64) NOT NULL COMMENT '快递公司名称',
  `tracking_no` VARCHAR(64) NOT NULL COMMENT '运单号',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '物流状态 0已发货 1已揽收 2运输中 3派送中 4已签收 5异常签收',
  `shipped_at` DATETIME NOT NULL COMMENT '发货时间',
  `signed_at` DATETIME DEFAULT NULL COMMENT '签收时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_tracking_no` (`tracking_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物流发货表';

-- 物流轨迹表
CREATE TABLE IF NOT EXISTS `shipment_trace` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shipment_id` BIGINT NOT NULL COMMENT '物流ID',
  `status` TINYINT NOT NULL COMMENT '轨迹状态 0已发货 1已揽收 2运输中 3派送中 4已签收 5异常签收',
  `location` VARCHAR(256) DEFAULT NULL COMMENT '节点位置',
  `description` VARCHAR(512) NOT NULL COMMENT '轨迹描述',
  `operator` VARCHAR(128) DEFAULT NULL COMMENT '操作人/快递员',
  `operator_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `trace_time` DATETIME NOT NULL COMMENT '轨迹时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_shipment_id` (`shipment_id`),
  KEY `idx_trace_time` (`trace_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物流轨迹表';

-- 催单记录表
CREATE TABLE IF NOT EXISTS `delivery_urge` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shipment_id` BIGINT NOT NULL COMMENT '物流ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `urge_date` DATE NOT NULL COMMENT '催单日期（用于限制每日1次）',
  `remark` VARCHAR(256) DEFAULT NULL COMMENT '用户备注',
  `handled` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已处理 0未处理 1已处理',
  `admin_id` BIGINT DEFAULT NULL COMMENT '处理管理员ID',
  `admin_remark` VARCHAR(256) DEFAULT NULL COMMENT '管理员处理备注',
  `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shipment_date` (`shipment_id`, `urge_date`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_handled` (`handled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='催单记录表';

-- 异常签收表
CREATE TABLE IF NOT EXISTS `delivery_issue` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shipment_id` BIGINT NOT NULL COMMENT '物流ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `issue_type` VARCHAR(32) NOT NULL COMMENT '问题类型：damaged=破损 wrong=错发 missing=丢件 other=其他',
  `description` VARCHAR(1024) NOT NULL COMMENT '问题描述',
  `photos` VARCHAR(1024) DEFAULT NULL COMMENT '照片URL（多个逗号分隔）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态 0待处理 1处理中 2已解决 3已驳回',
  `admin_id` BIGINT DEFAULT NULL COMMENT '处理管理员ID',
  `admin_remark` VARCHAR(512) DEFAULT NULL COMMENT '管理员处理备注',
  `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_shipment_id` (`shipment_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='异常签收表';

-- 快递公司字典数据
INSERT INTO `shipment` (id, order_id, order_no, express_company_code, express_company_name, tracking_no, status, shipped_at) VALUES
(1, 0, 'SAMPLE', 'SF', '顺丰速运', 'SF1234567890', 4, '2024-01-01 10:00:00')
ON DUPLICATE KEY UPDATE id=id;

DELETE FROM `shipment` WHERE order_id = 0;
