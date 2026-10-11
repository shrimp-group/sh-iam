CREATE TABLE `iam_demo` (

  -- 以下是主键字段
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',

  -- 以下是业务字段
  `biz_column` varchar(31) NOT NULL DEFAULT '' COMMENT '业务字段示例',
  
  -- 以下是系统字段
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(31) NOT NULL DEFAULT 'nobody' COMMENT '创建人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` varchar(31) NOT NULL DEFAULT 'nobody' COMMENT '更新人',
  `remark` varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '版本号',
  `deleted` bigint unsigned NOT NULL DEFAULT '0' COMMENT 'deleted',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `biz_column` (`biz_column`) USING BTREE,
) ENGINE=InnoDB COMMENT='表结构示例';
