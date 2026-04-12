/*
 Navicat Premium Dump SQL

 Source Server         : database
 Source Server Type    : MySQL
 Source Server Version : 80044 (8.0.44)
 Source Host           : localhost:3306
 Source Schema         : family_elderly_bind

 Target Server Type    : MySQL
 Target Server Version : 80044 (8.0.44)
 File Encoding         : 65001

 Date: 10/04/2026 00:04:12
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_chat_log
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_log`;
CREATE TABLE `ai_chat_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `elderly_id` bigint NOT NULL COMMENT '老人ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `sender_role` tinyint(1) NOT NULL COMMENT '发送者：1-老人 2-AI',
  `user_type` int NULL DEFAULT NULL COMMENT '用户类型(如:1老人 2家属)',
  `user_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '用户发送的消息内容',
  `ai_response` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'AI的回复内容',
  `content_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '文本内容（语音转写的文字或AI生成的文字）',
  `audio_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '语音文件地址（如果需要回放）',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '对话时间/创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI对话历史记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_chat_log
-- ----------------------------

-- ----------------------------
-- Table structure for emergency_contact
-- ----------------------------
DROP TABLE IF EXISTS `emergency_contact`;
CREATE TABLE `emergency_contact`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `elderly_id` bigint NOT NULL,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '联系人姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '联系电话',
  `relationship` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人关系',
  `priority` int NULL DEFAULT 0 COMMENT '优先级（数值越小优先级越高，用于排序）',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '紧急联系人表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of emergency_contact
-- ----------------------------

-- ----------------------------
-- Table structure for family_elderly_bind
-- ----------------------------
DROP TABLE IF EXISTS `family_elderly_bind`;
CREATE TABLE `family_elderly_bind`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `family_id` bigint NOT NULL COMMENT '家属ID',
  `elderly_id` bigint NOT NULL COMMENT '老人ID',
  `relation_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关系称谓（如：女儿）',
  `is_primary` tinyint(1) NULL DEFAULT 0 COMMENT '是否主账号/管理员',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_bind`(`family_id` ASC, `elderly_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '家属与老人绑定关系表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of family_elderly_bind
-- ----------------------------
INSERT INTO `family_elderly_bind` VALUES (1, 2, 1, '父子', 1, 0, '2026-04-07 13:30:24', '2026-04-07 13:30:24');

-- ----------------------------
-- Table structure for health_record
-- ----------------------------
DROP TABLE IF EXISTS `health_record`;
CREATE TABLE `health_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `elderly_id` bigint NOT NULL COMMENT '关联老人ID',
  `device_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '绑定的硬件设备ID(如ESP32)',
  `type` tinyint(1) NOT NULL COMMENT '类型：1-血压 2-血糖',
  `sys` int NULL DEFAULT NULL COMMENT '血压收缩压(高压)',
  `dia` int NULL DEFAULT NULL COMMENT '血压舒张压(低压)',
  `glucose` decimal(5, 2) NULL DEFAULT NULL COMMENT '血糖值',
  `status` int NULL DEFAULT NULL COMMENT '健康状态(比如 1:偏高 2:偏低)',
  `record_time` datetime NULL DEFAULT NULL COMMENT '测量的具体时间',
  `note` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注信息(如:按键单击)',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_elderly_time`(`elderly_id` ASC, `record_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '健康测量历史记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of health_record
-- ----------------------------

-- ----------------------------
-- Table structure for health_remind_config
-- ----------------------------
DROP TABLE IF EXISTS `health_remind_config`;
CREATE TABLE `health_remind_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `elderly_id` bigint NOT NULL COMMENT '关联老人ID',
  `type` tinyint(1) NOT NULL COMMENT '类型：1-血压 2-血糖',
  `remind_time` time NOT NULL COMMENT '提醒时间（如 08:00:00）',
  `voice_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '家属录制的语音文件URL',
  `is_active` tinyint(1) NULL DEFAULT 1 COMMENT '开关状态',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '健康测量提醒配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of health_remind_config
-- ----------------------------
INSERT INTO `health_remind_config` VALUES (1, 1, 1, '08:00:00', NULL, 1, 0, '2026-04-07 14:59:19', '2026-04-07 14:59:19');

-- ----------------------------
-- Table structure for location_log
-- ----------------------------
DROP TABLE IF EXISTS `location_log`;
CREATE TABLE `location_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scene` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'SHARE/SOS/ACTIVE',
  `elderly_id` bigint NOT NULL,
  `latitude` decimal(10, 6) NOT NULL COMMENT '纬度',
  `longitude` decimal(10, 6) NOT NULL COMMENT '经度',
  `address_detail` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '逆地理编码后的详细地址',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '详细地址',
  `trigger_type` tinyint(1) NOT NULL COMMENT '触发类型：1-紧急呼叫SOS 2-日常活跃登录',
  `log_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '定位上报时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上报时间/创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_elderly_time`(`elderly_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '位置上报记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of location_log
-- ----------------------------
INSERT INTO `location_log` VALUES (1, 'ACTIVE', 1, 23.186899, 113.419151, NULL, '获取地址中...', 1, '2026-04-09 23:54:44', 0, '2026-04-09 23:54:44', '2026-04-09 23:54:44');

-- ----------------------------
-- Table structure for medicine_info
-- ----------------------------
DROP TABLE IF EXISTS `medicine_info`;
CREATE TABLE `medicine_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `elderly_id` bigint NOT NULL COMMENT '所属老人ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '药品名称',
  `dosage_desc` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '单次剂量描述（如：1粒，5ml）',
  `photo_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '药物照片URL',
  `current_stock` int NOT NULL DEFAULT 0 COMMENT '当前库存量（每次服用自动减）',
  `low_stock_threshold` int NULL DEFAULT 5 COMMENT '低库存预警阈值',
  `voice_remind_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '亲情提醒语音URL',
  `family_voice_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '家属录制的提醒语音URL',
  `start_date` date NULL DEFAULT NULL COMMENT '服药开始日期',
  `end_date` date NULL DEFAULT NULL COMMENT '服药结束日期（长期可为空）',
  `is_active` tinyint(1) NULL DEFAULT 1 COMMENT '是否正在服用',
  `version` int NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '药品基础信息与库存表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of medicine_info
-- ----------------------------
INSERT INTO `medicine_info` VALUES (1, 1, '氨氯地平片', '1片/次', NULL, 100, 1, '', NULL, '2026-04-07', '2099-12-31', 1, 1, 0, '2026-04-07 14:55:22', '2026-04-07 14:55:22');

-- ----------------------------
-- Table structure for medicine_log
-- ----------------------------
DROP TABLE IF EXISTS `medicine_log`;
CREATE TABLE `medicine_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `medicine_id` bigint NOT NULL,
  `elderly_id` bigint NOT NULL,
  `plan_time` datetime NOT NULL COMMENT '应服药的具体日期时间',
  `take_time` datetime NULL DEFAULT NULL COMMENT '实际点击“已服”的时间',
  `status` tinyint(1) NULL DEFAULT 0 COMMENT '状态：0-未服 1-已服 2-稍后提醒',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '服药打卡记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of medicine_log
-- ----------------------------

-- ----------------------------
-- Table structure for medicine_plan
-- ----------------------------
DROP TABLE IF EXISTS `medicine_plan`;
CREATE TABLE `medicine_plan`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `medicine_id` bigint NOT NULL COMMENT '关联药品ID',
  `elderly_id` bigint NOT NULL COMMENT '冗余老人ID方便查询',
  `take_time` time NOT NULL COMMENT '计划服用时间（如 08:00:00）',
  `frequency_type` int NULL DEFAULT NULL COMMENT '频次类型',
  `frequency_value` int NULL DEFAULT NULL COMMENT '频次具体值',
  `week_days` int NULL DEFAULT NULL COMMENT '星期具体值',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_time`(`elderly_id` ASC, `take_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '服药时间计划表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of medicine_plan
-- ----------------------------
INSERT INTO `medicine_plan` VALUES (1, 1, 1, '08:00:00', 2, NULL, NULL, 0, '2026-04-07 14:55:22', '2026-04-07 14:55:22');

-- ----------------------------
-- Table structure for user_elderly
-- ----------------------------
DROP TABLE IF EXISTS `user_elderly`;
CREATE TABLE `user_elderly`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号（登录账号）',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称/亲切称谓（如：张爷爷）',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `gender` tinyint(1) NULL DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
  `age` int NULL DEFAULT NULL COMMENT '年龄',
  `height` decimal(5, 2) NULL DEFAULT NULL COMMENT '身高(cm)',
  `weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '体重(kg)',
  `chronic_diseases` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '健康标签（逗号分隔或JSON）',
  `guardian_active` tinyint(1) NULL DEFAULT 0 COMMENT '守护预警开关：0-关闭 1-开启',
  `guardian_threshold` int NULL DEFAULT 24 COMMENT '预警阈值（小时）：12/24/48',
  `last_active_time` datetime NULL DEFAULT NULL COMMENT '最后活跃时间（判定失联）',
  `dnd_start_time` time NULL DEFAULT '22:00:00' COMMENT '免打扰开始时间',
  `dnd_end_time` time NULL DEFAULT '06:00:00' COMMENT '免打扰结束时间',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '密码（BCrypt加密）',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '账号状态：1-正常 0-禁用',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间/创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_phone`(`phone` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '老人基本信息及配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_elderly
-- ----------------------------
INSERT INTO `user_elderly` VALUES (1, '15687646278', '张老头', '张老头', NULL, 0, NULL, NULL, NULL, NULL, 0, 24, '2026-04-09 23:59:03', '22:00:00', '06:00:00', '$2a$10$NBJwHc8qz8MTwFQ9dneXk.vgMJdAhGZs/6BEjRPux3SfkDii/Xm3K', 1, 0, '2026-04-06 17:54:27', '2026-04-10 00:00:00');

-- ----------------------------
-- Table structure for user_family
-- ----------------------------
DROP TABLE IF EXISTS `user_family`;
CREATE TABLE `user_family`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '姓名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '密码（BCrypt加密）',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '账号状态：1-正常 0-禁用',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_phone`(`phone` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '家属信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_family
-- ----------------------------
INSERT INTO `user_family` VALUES (1, '13888888888', '张三', '$10$NBJwHc8qz8MTwFQ9dneXk.vgMJdAhGZs/6BEjRPux3SfkDii/Xm3K', NULL, 1, 0, '2026-04-06 19:36:46', '2026-04-06 19:36:46');
INSERT INTO `user_family` VALUES (2, '15687646278', '李四', '$2a$10$SHls4kGYXO3lu4YnKN2qn.O0WEIP.eLJRaYpUlJTEEDkw9rUWdpbi', NULL, 1, 0, '2026-04-07 13:29:11', '2026-04-07 13:29:11');

SET FOREIGN_KEY_CHECKS = 1;
