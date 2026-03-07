CREATE TABLE `appointment_records` (
                                       `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                       `username` VARCHAR(50) NOT NULL COMMENT '用户名',
                                       `id_card` VARCHAR(18) DEFAULT NULL COMMENT '身份证号',
                                       `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
                                       `department` VARCHAR(100) DEFAULT NULL COMMENT '科室',
                                       `date` DATE DEFAULT NULL COMMENT '就诊日期',
                                       `time` TIME DEFAULT NULL COMMENT '就诊时间',
                                       `doctor_name` VARCHAR(50) DEFAULT NULL COMMENT '医生姓名',
                                       `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
                                       `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
                                       PRIMARY KEY (`id`),
                                       INDEX `idx_username` (`username`), -- 常用查询字段索引
                                       INDEX `idx_date` (`date`)            -- 方便按日期筛选
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='就诊预约记录表';