-- ============================================================
-- 基于玩家画像的游戏推荐系统 - GameRec
-- 数据库性能优化：索引优化 + 表结构优化 + 查询优化
-- ============================================================

USE game_recommend_db;

-- ============================================================
-- 第一部分：核心查询索引优化
-- ============================================================

-- 1. rating 表 - 推荐算法最频繁的查询路径
-- 问题：ProfileService 中的 RFM 计算需全表扫描 rating（getAllRecencies/getAllFrequencies 等）
-- 优化：复合索引覆盖最常见的查询组合
CREATE INDEX idx_rating_user_valid ON rating(user_id, is_valid);
CREATE INDEX idx_rating_game_valid ON rating(game_id, is_valid);
CREATE INDEX idx_rating_user_game_valid ON rating(user_id, game_id, is_valid);

-- 2. rating 表 - 分析查询优化（AnalysisService 中大量 group by 查询）
-- 热门游戏、高分游戏、游玩时长分布等都需要
CREATE INDEX idx_rating_valid_playhours ON rating(is_valid, play_hours);
CREATE INDEX idx_rating_valid_rating ON rating(is_valid, rating);

-- 3. game 表 - 游戏搜索和过滤优化
-- GameSearch 页面的 keyword 搜索和 genre 过滤
CREATE INDEX idx_game_name_cn ON game(game_name_cn(100));
CREATE INDEX idx_game_price ON game(price);
CREATE INDEX idx_game_is_free ON game(is_free);
CREATE INDEX idx_game_release_date ON game(release_date);
CREATE INDEX idx_game_positive_ratings ON game(positive_ratings DESC);

-- 4. game 表 - 热门/高分游戏排行优化
CREATE INDEX idx_game_purchase_count ON game(purchase_count DESC);
CREATE INDEX idx_game_play_count ON game(play_count DESC);
CREATE INDEX idx_game_avg_play_hours ON game(avg_play_hours DESC);

-- 5. user_profile 表 - 画像查询优化
-- 按玩家类型/价值等级/活跃度等级的过滤查询
CREATE INDEX idx_up_value_activity ON user_profile(value_level, activity_level);
CREATE INDEX idx_up_player_type_activity ON user_profile(player_type, activity_level);

-- 6. evaluation_result 表 - 评估查询优化
CREATE INDEX idx_eval_algo_k ON evaluation_result(algorithm_name, top_k);


-- ============================================================
-- 第二部分：表结构优化
-- ============================================================

-- 2.1 game 表新增字段：利用 steam_games.csv 的扩展评价数据
-- 这些字段在 games_merged.csv 中已有但 game 表缺失
ALTER TABLE game
    ADD COLUMN total_positive INT DEFAULT 0 COMMENT '总好评数（来自steam_games.csv）' AFTER total_reviews,
    ADD COLUMN total_negative INT DEFAULT 0 COMMENT '总差评数（来自steam_games.csv）' AFTER total_positive,
    ADD COLUMN review_score_desc_cn VARCHAR(100) DEFAULT NULL COMMENT '评价描述中文' AFTER review_score_desc,
    ADD COLUMN price_initial DOUBLE DEFAULT 0 COMMENT '初始价格（美元）' AFTER price;

-- 2.2 game 表增加游戏封面/缩略图字段（为后续扩展预留）
ALTER TABLE game
    ADD COLUMN header_image VARCHAR(500) DEFAULT NULL COMMENT '游戏封面图URL' AFTER metacritic;

-- 2.3 game 表增加游戏简介字段
ALTER TABLE game
    ADD COLUMN short_description TEXT DEFAULT NULL COMMENT '游戏简介' AFTER header_image,
    ADD COLUMN detailed_description TEXT DEFAULT NULL COMMENT '详细描述' AFTER short_description;

-- 2.4 user 表增加昵称字段（为个性化展示预留）
ALTER TABLE `user`
    ADD COLUMN nickname VARCHAR(100) DEFAULT NULL COMMENT '用户昵称' AFTER user_id,
    ADD COLUMN avatar_url VARCHAR(500) DEFAULT NULL COMMENT '头像URL' AFTER nickname;

-- 2.5 rating 表增加行为时间字段
ALTER TABLE rating
    ADD COLUMN behavior_time DATETIME DEFAULT NULL COMMENT '行为发生时间' AFTER play_hours;

-- 为行为时间创建索引
CREATE INDEX idx_rating_behavior_time ON rating(behavior_time);


-- ============================================================
-- 第三部分：数据库参数优化建议（需在MySQL配置中修改）
-- ============================================================

-- 建议在 my.ini / my.cnf 中设置：
-- 
-- [mysqld]
-- # InnoDB 缓冲池（设为服务器内存的 50-70%）
-- innodb_buffer_pool_size = 1G
-- 
-- # 日志文件大小
-- innodb_log_file_size = 256M
-- 
-- # 刷新方式（推荐值）
-- innodb_flush_log_at_trx_commit = 2
-- 
-- # 最大连接数
-- max_connections = 200
-- 
-- # 慢查询日志
-- slow_query_log = 1
-- slow_query_log_file = /var/log/mysql/slow.log
-- long_query_time = 1
-- 
-- # 查询缓存（MySQL 5.7）
-- query_cache_type = 1
-- query_cache_size = 64M
-- 
-- # 临时表大小
-- tmp_table_size = 64M
-- max_heap_table_size = 64M
-- 
-- # 排序缓冲区
-- sort_buffer_size = 4M
-- 
-- # 连接缓冲区
-- join_buffer_size = 4M


-- ============================================================
-- 第四部分：更新 game 表缺失的扩展数据
-- ============================================================

-- 从数据文件重新导入评价相关字段（需要Python脚本配合）
-- 这里提供手动更新的SQL模板：

-- 更新评价描述的中文翻译
-- UPDATE game SET review_score_desc_cn = 
--     CASE review_score_desc
--         WHEN 'Overwhelmingly Positive' THEN '好评如潮'
--         WHEN 'Very Positive' THEN '特别好评'
--         WHEN 'Positive' THEN '好评'
--         WHEN 'Mostly Positive' THEN '多半好评'
--         WHEN 'Mixed' THEN '褒贬不一'
--         WHEN 'Mostly Negative' THEN '多半差评'
--         WHEN 'Negative' THEN '差评'
--         WHEN 'Very Negative' THEN '特别差评'
--         WHEN 'Overwhelmingly Negative' THEN '差评如潮'
--         WHEN 'No user reviews' THEN '暂无评价'
--         ELSE review_score_desc
--     END;


-- ============================================================
-- 第五部分：查询优化示例
-- ============================================================

-- 优化前：ProfileService.getAllRecencies() - 全表扫描
-- EXPLAIN SELECT user_id, MAX(create_time) FROM rating WHERE is_valid = 1 GROUP BY user_id;
-- 优化后：利用 idx_rating_user_valid 索引

-- 优化前：AnalysisService.getPlayHoursDistribution() - 8次独立查询
-- 优化后：使用单次 CASE WHEN 查询
-- SELECT 
--     SUM(CASE WHEN play_hours = 0 THEN 1 ELSE 0 END) AS bin_0,
--     SUM(CASE WHEN play_hours > 0 AND play_hours <= 5 THEN 1 ELSE 0 END) AS bin_1_5,
--     SUM(CASE WHEN play_hours > 5 AND play_hours <= 20 THEN 1 ELSE 0 END) AS bin_5_20,
--     SUM(CASE WHEN play_hours > 20 AND play_hours <= 50 THEN 1 ELSE 0 END) AS bin_20_50,
--     SUM(CASE WHEN play_hours > 50 AND play_hours <= 100 THEN 1 ELSE 0 END) AS bin_50_100,
--     SUM(CASE WHEN play_hours > 100 AND play_hours <= 500 THEN 1 ELSE 0 END) AS bin_100_500,
--     SUM(CASE WHEN play_hours > 500 AND play_hours <= 2000 THEN 1 ELSE 0 END) AS bin_500_2000,
--     SUM(CASE WHEN play_hours > 2000 THEN 1 ELSE 0 END) AS bin_2000_plus
-- FROM rating WHERE is_valid = 1;

-- 优化前：ProfileService.getGlobalMaxActivityScore() - N+1查询
-- 优化后：使用窗口函数或单次聚合查询


-- ============================================================
-- 第六部分：验证索引效果
-- ============================================================

-- 查看所有索引
SELECT 
    TABLE_NAME AS '表名',
    INDEX_NAME AS '索引名',
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS '索引列',
    INDEX_TYPE AS '索引类型'
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'game_recommend_db'
GROUP BY TABLE_NAME, INDEX_NAME, INDEX_TYPE
ORDER BY TABLE_NAME, INDEX_NAME;

-- 查看表大小
SELECT 
    TABLE_NAME AS '表名',
    TABLE_ROWS AS '行数',
    ROUND(DATA_LENGTH / 1024 / 1024, 2) AS '数据大小(MB)',
    ROUND(INDEX_LENGTH / 1024 / 1024, 2) AS '索引大小(MB)',
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS '总大小(MB)'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'game_recommend_db'
ORDER BY (DATA_LENGTH + INDEX_LENGTH) DESC;
