-- ============================================================
-- 基于玩家画像的游戏推荐系统 - GameRec
-- 新增：AI 接口设置表（用户可在系统设置页面自行配置 API）
-- 设计目的：
--   1. API Key 不再硬编码在 application.yml / 代码中，避免隐私泄露
--   2. 用户可在前端「系统设置」页面动态配置，无需重启后端
--   3. 数据库设置优先于配置文件，配置文件仅作为兜底默认值
-- ============================================================

USE game_recommend_db;

CREATE TABLE IF NOT EXISTS `ai_settings` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID（单行配置，固定 id=1）',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用 AI 功能：1-启用 0-禁用',
    `api_key` VARCHAR(512) DEFAULT NULL COMMENT 'AI 服务 API Key（用户自行填写，入库保存）',
    `model` VARCHAR(100) DEFAULT 'glm-4-flash-250414' COMMENT '模型名称（如智谱 GLM-4-Flash-250414）',
    `base_url` VARCHAR(255) DEFAULT 'https://open.bigmodel.cn/api/paas/v4' COMMENT 'API 基础地址（兼容 OpenAI 格式接口）',
    `max_tokens` INT DEFAULT 1024 COMMENT '单次请求最大 token 数',
    `temperature` DOUBLE DEFAULT 0.3 COMMENT '温度参数（0-1，越低越稳定）',
    `timeout_seconds` INT DEFAULT 90 COMMENT '请求超时时间（秒）',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 接口设置表（用户可自行配置，避免密钥硬编码）';

-- 初始化默认配置行（api_key 留空，由用户在系统设置页面填写）
INSERT INTO `ai_settings` (`id`, `enabled`, `api_key`, `model`, `base_url`, `max_tokens`, `temperature`, `timeout_seconds`)
VALUES (1, 1, NULL, 'glm-4-flash-250414', 'https://open.bigmodel.cn/api/paas/v4', 1024, 0.3, 90)
ON DUPLICATE KEY UPDATE `update_time` = CURRENT_TIMESTAMP;

SELECT
    TABLE_NAME AS '表名',
    TABLE_COMMENT AS '注释'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'game_recommend_db' AND TABLE_NAME = 'ai_settings';
