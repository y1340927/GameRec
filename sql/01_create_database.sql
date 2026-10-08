-- ============================================================
-- 基于玩家画像的游戏推荐系统 - GameRec
-- 第1天：数据库设计与建表 DDL
-- 数据库：game_recommend_db
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS game_recommend_db 
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE game_recommend_db;

-- ============================================================
-- 1. 玩家表 (user)
-- 数据来源：simulated_user_behaviors.csv 中的 user_id
-- 预计数据量：10,000条
-- ============================================================
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '原始用户ID（1~10000）',
    `purchase_count` INT DEFAULT 0 COMMENT '购买游戏总数',
    `play_count` INT DEFAULT 0 COMMENT '游玩游戏总数（play_hours>0）',
    `total_play_hours` DOUBLE DEFAULT 0 COMMENT '总游玩时长(小时)',
    `avg_play_hours` DOUBLE DEFAULT 0 COMMENT '平均游玩时长(小时)',
    `purchase_play_ratio` DOUBLE DEFAULT 0 COMMENT '购买-游玩转化率（游玩数/购买数）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='玩家表';

-- ============================================================
-- 2. 游戏表 (game)
-- 数据来源：steam.csv（主表，27,075款）+ steam_games.csv（扩展，含中文名）
-- 设计说明：
--   - 以 steam.csv 为核心，27,075款游戏
--   - steam_games.csv 提供中文游戏名、开发商/发行商（JSON格式）等扩展信息
--   - 两表通过 appid / steam_appid 关联
-- ============================================================
CREATE TABLE `game` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `game_id` BIGINT NOT NULL COMMENT '游戏ID（对应steam.csv的appid）',
    `game_name` VARCHAR(500) NOT NULL COMMENT '游戏名称（英文，来自steam.csv）',
    `game_name_cn` VARCHAR(500) DEFAULT NULL COMMENT '游戏中文名（优先翻译表映射，其次steam_games.csv中文名）',
    `release_date` DATE DEFAULT NULL COMMENT '发布日期',
    `developer` VARCHAR(500) DEFAULT NULL COMMENT '开发商（来自steam.csv）',
    `publisher` VARCHAR(500) DEFAULT NULL COMMENT '发行商（来自steam.csv）',
    `developers_json` TEXT DEFAULT NULL COMMENT '开发商列表（JSON数组，来自steam_games.csv）',
    `publishers_json` TEXT DEFAULT NULL COMMENT '发行商列表（JSON数组，来自steam_games.csv）',
    `platforms` VARCHAR(500) DEFAULT NULL COMMENT '支持平台（英文，分号分隔）',
    `platforms_cn` VARCHAR(500) DEFAULT NULL COMMENT '支持平台（中文，分号分隔）',
    `required_age` INT DEFAULT 0 COMMENT '年龄限制',
    `categories` TEXT DEFAULT NULL COMMENT '游戏分类（英文，分号分隔）',
    `categories_cn` TEXT DEFAULT NULL COMMENT '游戏分类（中文，分号分隔）',
    `genres` VARCHAR(500) DEFAULT NULL COMMENT '游戏类型（英文，分号分隔）',
    `genres_cn` VARCHAR(500) DEFAULT NULL COMMENT '游戏类型（中文，分号分隔）',
    `genres_json` TEXT DEFAULT NULL COMMENT '游戏类型（JSON数组，来自steam_games.csv）',
    `steamspy_tags` TEXT DEFAULT NULL COMMENT 'SteamSpy标签（英文，分号分隔）',
    `steamspy_tags_cn` TEXT DEFAULT NULL COMMENT 'SteamSpy标签（中文，分号分隔）',
    `achievements` INT DEFAULT 0 COMMENT '成就数量',
    `positive_ratings` INT DEFAULT 0 COMMENT '好评数',
    `negative_ratings` INT DEFAULT 0 COMMENT '差评数',
    `average_playtime` INT DEFAULT 0 COMMENT '平均游玩时长（分钟）',
    `median_playtime` INT DEFAULT 0 COMMENT '中位游玩时长（分钟）',
    `owners` VARCHAR(100) DEFAULT NULL COMMENT '拥有者数量范围',
    `price` DOUBLE DEFAULT 0 COMMENT '价格（美元）',
    `is_free` TINYINT DEFAULT 0 COMMENT '是否免费：0-否 1-是',
    `total_reviews` INT DEFAULT 0 COMMENT '总评价数（来自steam_games.csv）',
    `total_positive` INT DEFAULT 0 COMMENT '总好评数（来自steam_games.csv）',
    `total_negative` INT DEFAULT 0 COMMENT '总差评数（来自steam_games.csv）',
    `review_score` DOUBLE DEFAULT 0 COMMENT '评价分数（来自steam_games.csv）',
    `review_score_desc` VARCHAR(100) DEFAULT NULL COMMENT '评价描述',
    `review_score_desc_cn` VARCHAR(100) DEFAULT NULL COMMENT '评价描述中文',
    `positive_percentual` DOUBLE DEFAULT 0 COMMENT '好评率',
    `metacritic` INT DEFAULT NULL COMMENT 'Metacritic评分',
    `price_initial` DOUBLE DEFAULT 0 COMMENT '初始价格（美元）',
    `short_description` TEXT DEFAULT NULL COMMENT '游戏简介（自动生成）',
    `detailed_description` TEXT DEFAULT NULL COMMENT '详细描述（自动生成）',
    `purchase_count` INT DEFAULT 0 COMMENT '被购买总次数（从行为数据统计）',
    `play_count` INT DEFAULT 0 COMMENT '被游玩总次数（从行为数据统计）',
    `avg_play_hours` DOUBLE DEFAULT 0 COMMENT '平均游玩时长-小时（从行为数据统计）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_game_id` (`game_id`),
    INDEX `idx_game_name` (`game_name`(100)),
    INDEX `idx_genres` (`genres`(100)),
    INDEX `idx_developer` (`developer`(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏表';

-- ============================================================
-- 3. 评分表 (rating)
-- 数据来源：simulated_user_behaviors.csv 中有游玩记录的数据
-- 核心创新：rating字段存储的是游玩时长分箱后的伪评分（1-5分）
-- play_hours保留原始游玩时长数据
-- is_valid字段用于数据清洗（逻辑删除）
-- ============================================================
CREATE TABLE `rating` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `game_id` BIGINT NOT NULL COMMENT '游戏ID',
    `game_name` VARCHAR(500) DEFAULT NULL COMMENT '游戏名称（冗余，便于查询）',
    `rating` INT NOT NULL COMMENT '伪评分（1-5分，由游玩时长分箱转化）',
    `play_hours` DOUBLE DEFAULT 0 COMMENT '原始游玩时长(小时)',
    `is_valid` TINYINT DEFAULT 1 COMMENT '是否有效：1-有效 0-无效（逻辑删除）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_game_id` (`game_id`),
    INDEX `idx_rating` (`rating`),
    INDEX `idx_user_game` (`user_id`, `game_id`),
    INDEX `idx_is_valid` (`is_valid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评分表（游玩时长分箱→伪评分，核心创新）';

-- ============================================================
-- 4. 训练集 (rating_train)
-- 按行为序列前80%划分
-- ============================================================
CREATE TABLE `rating_train` LIKE `rating`;

-- ============================================================
-- 5. 测试集 (rating_test)
-- 按行为序列后20%划分
-- ============================================================
CREATE TABLE `rating_test` LIKE `rating`;

-- ============================================================
-- 6. 玩家画像表 (user_profile)
-- 存储RFM价值分层、活跃度、玩家类型、兴趣标签等完整画像
-- ============================================================
CREATE TABLE `user_profile` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    -- RFM价值分层
    `recency` BIGINT DEFAULT NULL COMMENT '最近游玩距今天数（R）',
    `frequency` INT DEFAULT NULL COMMENT '游玩游戏总次数（F）',
    `monetary` DOUBLE DEFAULT NULL COMMENT '平均伪评分（M）',
    `r_score` INT DEFAULT NULL COMMENT 'R评分（1-4分，四分位法）',
    `f_score` INT DEFAULT NULL COMMENT 'F评分（1-4分，四分位法）',
    `m_score` INT DEFAULT NULL COMMENT 'M评分（1-4分，四分位法）',
    `value_level` VARCHAR(20) DEFAULT NULL COMMENT '价值等级：高价值/中价值/低价值/沉默',
    -- 活跃度画像
    `activity_score` DOUBLE DEFAULT NULL COMMENT '活跃度原始得分（时间衰减加权）',
    `activity_normalized` DOUBLE DEFAULT NULL COMMENT '活跃度归一化得分（0-1）',
    `activity_level` VARCHAR(20) DEFAULT NULL COMMENT '活跃度等级：高热/中热/低热/冰封',
    -- 兴趣标签（JSON格式）
    `interest_tags` TEXT DEFAULT NULL COMMENT '兴趣标签（JSON数组，含标签名、平均分、频次）',
    -- 玩家类型分类（核心创新）
    `player_type` VARCHAR(20) DEFAULT NULL COMMENT '玩家类型：硬核/休闲/尝鲜/收藏',
    `total_play_hours` DOUBLE DEFAULT NULL COMMENT '总游玩时长(小时)',
    `avg_play_hours` DOUBLE DEFAULT NULL COMMENT '平均每游戏游玩时长(小时)',
    `purchase_count` INT DEFAULT NULL COMMENT '购买游戏总数',
    `play_count` INT DEFAULT NULL COMMENT '游玩游戏总数',
    `purchase_play_ratio` DOUBLE DEFAULT NULL COMMENT '购买-游玩转化率（游玩数/购买数）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    INDEX `idx_value_level` (`value_level`),
    INDEX `idx_activity_level` (`activity_level`),
    INDEX `idx_player_type` (`player_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='玩家画像表';

-- ============================================================
-- 7. 评估结果表 (evaluation_result)
-- 存储各算法在不同K值下的离线评估指标
-- ============================================================
CREATE TABLE `evaluation_result` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `algorithm_name` VARCHAR(50) NOT NULL COMMENT '算法名称：UserCF/ItemCF/CB/Popularity/SVD/Hybrid',
    `top_k` INT NOT NULL COMMENT 'Top-K值（5/10/15/20）',
    `precision_val` DOUBLE DEFAULT NULL COMMENT '准确率 Precision@K',
    `recall_val` DOUBLE DEFAULT NULL COMMENT '召回率 Recall@K',
    `f1_val` DOUBLE DEFAULT NULL COMMENT 'F1值 F1@K',
    `ndcg_val` DOUBLE DEFAULT NULL COMMENT 'NDCG值 NDCG@K',
    `valid_users` INT DEFAULT NULL COMMENT '有效评估用户数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_algorithm` (`algorithm_name`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估结果表';

-- ============================================================
-- 8. 数据导入记录表 (data_import_record)
-- 记录每次数据导入的详细情况，便于追踪和回溯
-- ============================================================
CREATE TABLE `data_import_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `data_type` VARCHAR(50) NOT NULL COMMENT '数据类型：USER/GAME/GAME_EXT/RATING',
    `file_name` VARCHAR(255) DEFAULT NULL COMMENT '导入的源文件名',
    `import_count` INT DEFAULT 0 COMMENT '读取总条数',
    `inserted_count` INT DEFAULT 0 COMMENT '新增条数',
    `updated_count` INT DEFAULT 0 COMMENT '更新条数',
    `skipped_count` INT DEFAULT 0 COMMENT '跳过条数',
    `failed_count` INT DEFAULT 0 COMMENT '失败条数',
    `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
    `start_time` DATETIME DEFAULT NULL COMMENT '导入开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '导入结束时间',
    `duration_seconds` DOUBLE DEFAULT NULL COMMENT '导入耗时（秒）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_data_type` (`data_type`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据导入记录表';

-- ============================================================
-- 9. 游戏别名表 (game_alias)
-- 用途：支持缩写搜索（如 CS:GO → Counter-Strike: Global Offensive）
-- 数据来源：手动整理的热门游戏缩写映射
-- ============================================================
CREATE TABLE IF NOT EXISTS `game_alias` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `game_id` BIGINT NOT NULL COMMENT '游戏ID（对应game.game_id）',
    `alias` VARCHAR(255) NOT NULL COMMENT '别名/缩写',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_alias` (`alias`),
    INDEX `idx_game_id` (`game_id`),
    CONSTRAINT `fk_alias_game` FOREIGN KEY (`game_id`) REFERENCES `game`(`game_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏别名表（支持缩写搜索）';

-- ============================================================
-- 验证建表结果
-- ============================================================
SHOW TABLES;

SELECT 
    TABLE_NAME AS '表名', 
    TABLE_ROWS AS '预估行数', 
    TABLE_COMMENT AS '注释'
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'game_recommend_db';
