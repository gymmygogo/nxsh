-- --------------------------------------------------------
-- 1. 家属与老人绑定关系表
-- --------------------------------------------------------
CREATE TABLE `family_elderly_bind` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `family_id` BIGINT(20) NOT NULL COMMENT '家属ID',
  `elderly_id` BIGINT(20) NOT NULL COMMENT '老人ID',
  `relation_name` VARCHAR(50) NOT NULL COMMENT '关系称谓（如：女儿）',
  `is_primary` TINYINT(1) DEFAULT 0 COMMENT '是否主账号/管理员',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bind` (`family_id`, `elderly_id`)
) COMMENT='家属与老人绑定关系表';

-- --------------------------------------------------------
-- 2. 家属信息表
-- --------------------------------------------------------
CREATE TABLE `user_family` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
  `name` VARCHAR(50) DEFAULT NULL COMMENT '姓名',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`)
) COMMENT='家属信息表';

-- --------------------------------------------------------
-- 3. 老人基本信息及配置表
-- --------------------------------------------------------
CREATE TABLE `user_elderly` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `phone` VARCHAR(20) NOT NULL COMMENT '手机号（登录账号）',
  `name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称/亲切称谓（如：张爷爷）',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `gender` TINYINT(1) DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
  `age` INT(3) DEFAULT NULL COMMENT '年龄',
  `height` DECIMAL(5,2) DEFAULT NULL COMMENT '身高(cm)',
  `weight` DECIMAL(5,2) DEFAULT NULL COMMENT '体重(kg)',
  `chronic_diseases` VARCHAR(255) DEFAULT NULL COMMENT '健康标签（逗号分隔或JSON）',
  `guardian_active` TINYINT(1) DEFAULT 0 COMMENT '守护预警开关：0-关闭 1-开启',
  `guardian_threshold` INT(11) DEFAULT 24 COMMENT '预警阈值（小时）：12/24/48',
  `last_active_time` DATETIME DEFAULT NULL COMMENT '最后活跃时间（判定失联）',
  `dnd_start_time` TIME DEFAULT '22:00:00' COMMENT '免打扰开始时间',
  `dnd_end_time` TIME DEFAULT '06:00:00' COMMENT '免打扰结束时间',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间/创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`)
) COMMENT='老人基本信息及配置表';

-- --------------------------------------------------------
-- 4. 健康测量提醒配置表
-- --------------------------------------------------------
CREATE TABLE `health_remind_config` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `elderly_id` BIGINT(20) NOT NULL COMMENT '关联老人ID',
  `type` TINYINT(1) NOT NULL COMMENT '类型：1-血压 2-血糖',
  `remind_time` TIME NOT NULL COMMENT '提醒时间（如 08:00:00）',
  `voice_url` VARCHAR(255) DEFAULT NULL COMMENT '家属录制的语音文件URL',
  `is_active` TINYINT(1) DEFAULT 1 COMMENT '开关状态',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT='健康测量提醒配置表';

-- --------------------------------------------------------
-- 5. 健康测量历史记录表
-- --------------------------------------------------------
CREATE TABLE `health_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `elderly_id` BIGINT(20) NOT NULL COMMENT '关联老人ID',
  `device_id` VARCHAR(64) DEFAULT NULL COMMENT '绑定的硬件设备ID(如ESP32)',
  `type` TINYINT(1) NOT NULL COMMENT '类型：1-血压 2-血糖',
  `sys` INT DEFAULT NULL COMMENT '血压收缩压(高压)',
  `dia` INT DEFAULT NULL COMMENT '血压舒张压(低压)',
  `glucose` DECIMAL(5,2) DEFAULT NULL COMMENT '血糖值',
  `status` INT DEFAULT NULL COMMENT '健康状态：0-正常 1-偏高 2-偏低',
  `record_time` DATETIME DEFAULT NULL COMMENT '测量时间',
  `note` VARCHAR(255) DEFAULT NULL COMMENT '备注信息(如:按键单击)',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_elderly_time` (`elderly_id`, `record_time`)
) COMMENT='健康测量历史记录表';

-- --------------------------------------------------------
-- 6. 服药打卡记录表
-- --------------------------------------------------------
CREATE TABLE `medicine_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `medicine_id` BIGINT(20) NOT NULL,
  `elderly_id` BIGINT(20) NOT NULL,
  `plan_time` DATETIME NOT NULL COMMENT '应服药的具体日期时间',
  `take_time` DATETIME DEFAULT NULL COMMENT '实际点击“已服”的时间',
  `status` TINYINT(1) DEFAULT 0 COMMENT '状态：0-未服 1-已服 2-稍后提醒',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT='服药打卡记录表';

-- --------------------------------------------------------
-- 7. 服药时间计划表
-- --------------------------------------------------------
CREATE TABLE `medicine_plan` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `medicine_id` BIGINT(20) NOT NULL COMMENT '关联药品ID',
  `elderly_id` BIGINT(20) NOT NULL COMMENT '冗余老人ID方便查询',
  `take_time` TIME NOT NULL COMMENT '计划服用时间（如 08:00:00）',
  `frequency_type` INT DEFAULT NULL COMMENT '频次类型',
  `frequency_value` INT DEFAULT NULL COMMENT '频次具体值',
  `week_days` VARCHAR(32) DEFAULT NULL COMMENT '星期具体值，支持如 1,3,5',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_time` (`elderly_id`, `take_time`)
) COMMENT='服药时间计划表';

-- --------------------------------------------------------
-- 8. 药品基础信息与库存表
-- --------------------------------------------------------
CREATE TABLE `medicine_info` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `elderly_id` BIGINT(20) NOT NULL COMMENT '所属老人ID',
  `name` VARCHAR(100) NOT NULL COMMENT '药品名称',
  `dosage_desc` VARCHAR(50) NOT NULL COMMENT '单次剂量描述（如：1粒，5ml）',
  `photo_url` VARCHAR(255) DEFAULT NULL COMMENT '药物照片URL',
  `current_stock` INT(11) NOT NULL DEFAULT 0 COMMENT '当前库存量（每次服用自动减）',
  `low_stock_threshold` INT(11) DEFAULT 5 COMMENT '低库存预警阈值',
  `voice_remind_url` VARCHAR(255) DEFAULT NULL COMMENT '亲情提醒语音URL',
  `family_voice_url` VARCHAR(500) DEFAULT NULL COMMENT '家属录制的提醒语音URL',
  `start_date` DATE DEFAULT NULL COMMENT '服药开始日期',
  `end_date` DATE DEFAULT NULL COMMENT '服药结束日期（长期可为空）',
  `is_active` TINYINT(1) DEFAULT 1 COMMENT '是否正在服用',
  `version` INT DEFAULT 1 COMMENT '乐观锁版本号',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT='药品基础信息与库存表';

-- --------------------------------------------------------
-- 9. AI对话历史记录表
-- --------------------------------------------------------
CREATE TABLE `ai_chat_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `elderly_id` BIGINT(20) NOT NULL COMMENT '老人ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `sender_role` TINYINT(1) NOT NULL COMMENT '发送者：1-老人 2-AI',
  `user_type` INT DEFAULT NULL COMMENT '用户类型(如:1老人 2家属)',
  `user_message` TEXT DEFAULT NULL COMMENT '用户发送的消息内容',
  `ai_response` TEXT DEFAULT NULL COMMENT 'AI的回复内容',
  `content_text` TEXT DEFAULT NULL COMMENT '文本内容（语音转写的文字或AI生成的文字）',
  `audio_url` VARCHAR(255) DEFAULT NULL COMMENT '语音文件地址（如果需要回放）',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '对话时间/创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT='AI对话历史记录表';

-- --------------------------------------------------------
-- 10. 位置上报记录表
-- --------------------------------------------------------
CREATE TABLE `location_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `scene` VARCHAR(16) DEFAULT NULL COMMENT 'SHARE/SOS/ACTIVE',
  `elderly_id` BIGINT(20) NOT NULL,
  `latitude` DECIMAL(10, 6) NOT NULL COMMENT '纬度',
  `longitude` DECIMAL(10, 6) NOT NULL COMMENT '经度',
  `address_detail` VARCHAR(255) DEFAULT NULL COMMENT '逆地理编码后的详细地址',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '详细地址',
  `trigger_type` TINYINT(1) NOT NULL COMMENT '触发类型：1-紧急呼叫SOS 2-日常活跃登录',
  `log_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '定位上报时间',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上报时间/创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_elderly_time` (`elderly_id`, `create_time`)
) COMMENT='位置上报记录表';

-- --------------------------------------------------------
-- 11. 紧急联系人表
-- --------------------------------------------------------
CREATE TABLE `emergency_contact` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `elderly_id` BIGINT(20) NOT NULL,
  `name` VARCHAR(50) NOT NULL COMMENT '联系人姓名',
  `phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
  `relationship` VARCHAR(32) DEFAULT NULL COMMENT '联系人关系',
  `priority` INT(3) DEFAULT 0 COMMENT '优先级（数值越小优先级越高，用于排序）',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT='紧急联系人表';

-- --------------------------------------------------------
-- 12. AI护理对话回复记录表
-- --------------------------------------------------------
CREATE TABLE `ai_care_reply` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `elderly_id` BIGINT(20) NOT NULL COMMENT '老人ID',
  `user_text` TEXT DEFAULT NULL COMMENT '老人的输入文本',
  `ai_text` TEXT DEFAULT NULL COMMENT 'AI生成的回复文本',
  `ai_voice_url` VARCHAR(255) DEFAULT NULL COMMENT 'AI生成的语音文件URL',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_elderly_time` (`elderly_id`, `create_time`)
) COMMENT='AI护理对话回复记录表';
