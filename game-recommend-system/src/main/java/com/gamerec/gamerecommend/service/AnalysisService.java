package com.gamerec.gamerecommend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gamerec.gamerecommend.entity.Game;
import com.gamerec.gamerecommend.entity.Rating;
import com.gamerec.gamerecommend.mapper.GameMapper;
import com.gamerec.gamerecommend.mapper.RatingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 探索性数据分析服务（EDA）
 *
 * 六个分析维度：
 *   1. 伪评分分布
 *   2. 玩家活跃度分析
 *   3. 游戏热度分析
 *   4. 高分游戏分析
 *   5. 游玩时长分布（创新）
 *   6. 购买-游玩转化率（创新）
 *
 * @author GameRec Team
 */
@Slf4j
@Service
public class AnalysisService {

    @Autowired
    private RatingMapper ratingMapper;

    @Autowired
    private GameMapper gameMapper;

    /**
     * 伪评分分布
     *
     * @return Map<评分, 数量>
     */
    public Map<String, Object> getRatingDistribution() {
        QueryWrapper<Rating> wrapper = new QueryWrapper<>();
        wrapper.eq("is_valid", 1).select("rating", "count(*) as count")
                .groupBy("rating").orderByAsc("rating");

        List<Map<String, Object>> list = ratingMapper.selectMaps(wrapper);

        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> distribution = new ArrayList<>();
        long total = 0;
        for (Map<String, Object> row : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            int rating = ((Number) row.get("rating")).intValue();
            long count = ((Number) row.get("count")).longValue();
            item.put("rating", rating);
            item.put("count", count);
            item.put("meaning", getRatingMeaning(rating));
            distribution.add(item);
            total += count;
        }
        result.put("distribution", distribution);
        result.put("total", total);
        return result;
    }

    /**
     * 最活跃玩家Top-N
     *
     * @param limit 返回数量
     * @return 活跃玩家列表
     */
    public List<Map<String, Object>> getTopActiveUsers(int limit) {
        QueryWrapper<Rating> wrapper = new QueryWrapper<>();
        wrapper.eq("is_valid", 1)
                .select("user_id", "count(*) as play_count", "sum(play_hours) as total_hours")
                .groupBy("user_id").orderByDesc("play_count").last("LIMIT " + limit);

        return ratingMapper.selectMaps(wrapper);
    }

    /**
     * 热门游戏Top-N（基于加权综合评分）
     *
     * 评分公式：综合热度 = total_reviews × 0.4 + positive_ratings × 0.3
     *                       + purchase_count × 0.15 + play_count × 0.15
     * 结合 Steam 真实数据与系统行为数据，避免单一指标的偏差。
     *
     * @param limit 返回数量
     * @return 热门游戏列表（含游戏名、类型、购买/游玩数等）
     */
    public List<Map<String, Object>> getTopHotGames(int limit) {
        // 1. 查询有 Steam 评价的游戏基础数据（含发布日期用于时效加权）
        QueryWrapper<Game> gameWrapper = new QueryWrapper<>();
        gameWrapper.select("game_id", "game_name", "game_name_cn",
                        "genres", "genres_cn", "total_reviews", "positive_ratings",
                        "negative_ratings", "positive_percentual", "metacritic",
                        "developer", "price", "is_free", "release_date")
                .gt("total_reviews", 0)
                .orderByDesc("total_reviews")
                .last("LIMIT 5000");
        List<Map<String, Object>> games = gameMapper.selectMaps(gameWrapper);

        // 2. 查询行为统计数据（购买数、游玩数）
        QueryWrapper<Rating> ratingWrapper = new QueryWrapper<>();
        ratingWrapper.eq("is_valid", 1)
                .select("game_id", "count(*) as purchase_count",
                        "SUM(CASE WHEN play_hours > 0 THEN 1 ELSE 0 END) as play_count")
                .groupBy("game_id");
        List<Map<String, Object>> ratingStats = ratingMapper.selectMaps(ratingWrapper);

        // 3. 构建 gameId → ratingStats 映射
        Map<Long, Map<String, Object>> statsMap = new HashMap<>();
        for (Map<String, Object> row : ratingStats) {
            Long gid = ((Number) row.get("game_id")).longValue();
            statsMap.put(gid, row);
        }

        // 4. 计算加权热度分（含新游戏时效加成）
        java.time.LocalDate now = java.time.LocalDate.now();
        for (Map<String, Object> game : games) {
            Long gid = ((Number) game.get("game_id")).longValue();
            long totalReviews = game.get("total_reviews") != null ?
                    ((Number) game.get("total_reviews")).longValue() : 0;
            long positiveRatings = game.get("positive_ratings") != null ?
                    ((Number) game.get("positive_ratings")).longValue() : 0;

            Map<String, Object> stats = statsMap.get(gid);
            long purchaseCount = 0, playCount = 0;
            if (stats != null) {
                purchaseCount = ((Number) stats.get("purchase_count")).longValue();
                playCount = stats.get("play_count") != null ?
                        ((Number) stats.get("play_count")).longValue() : 0;
            }

            double hotScore = totalReviews * 0.4 + positiveRatings * 0.3
                    + purchaseCount * 0.15 + playCount * 0.15;

            // 新游戏热度加成：近期发布的游戏获得额外曝光
            // 1年内的游戏最多获得50%额外加成，随时间递减
            Object releaseDateObj = game.get("release_date");
            if (releaseDateObj != null) {
                java.time.LocalDate releaseDate;
                if (releaseDateObj instanceof java.sql.Date) {
                    releaseDate = ((java.sql.Date) releaseDateObj).toLocalDate();
                } else if (releaseDateObj instanceof java.time.LocalDate) {
                    releaseDate = (java.time.LocalDate) releaseDateObj;
                } else {
                    String dateStr = releaseDateObj.toString();
                    if (dateStr.length() >= 10) {
                        releaseDate = java.time.LocalDate.parse(dateStr.substring(0, 10));
                    } else {
                        releaseDate = null;
                    }
                }
                if (releaseDate != null) {
                    long daysSinceRelease = java.time.temporal.ChronoUnit.DAYS.between(releaseDate, now);
                    if (daysSinceRelease >= 0 && daysSinceRelease < 365) {
                        double recencyFactor = 1.0 - (daysSinceRelease / 365.0); // 0~1，越新越高
                        hotScore = hotScore * (1.0 + recencyFactor * 0.5);
                    }
                }
            }

            game.put("purchase_count", purchaseCount);
            game.put("play_count", playCount);
            game.put("hot_score", Math.round(hotScore * 10.0) / 10.0);
        }

        // 5. 按 hotScore 降序排序
        games.sort((a, b) -> Double.compare(
                (Double) b.get("hot_score"), (Double) a.get("hot_score")));
        return games.subList(0, Math.min(limit, games.size()));
    }

    /**
     * 高分游戏Top-N（过滤评分人数≥10，防止统计偏差）
     *
     * @param limit 返回数量
     * @return 高分游戏列表
     */
    public List<Map<String, Object>> getTopRatedGames(int limit) {
        QueryWrapper<Rating> wrapper = new QueryWrapper<>();
        wrapper.eq("is_valid", 1)
                .select("game_id", "game_name", "count(*) as rating_count",
                        "round(avg(rating),2) as avg_rating",
                        "round(avg(play_hours),2) as avg_hours")
                .groupBy("game_id", "game_name")
                .having("count(*) >= 10")
                .orderByDesc("avg_rating")
                .last("LIMIT " + limit);

        return ratingMapper.selectMaps(wrapper);
    }

    /**
     * 游玩时长分布（创新）- 优化版：单次SQL查询替代8次查询
     *
     * 按游玩时长区间分组统计
     *
     * @return 时长分布数据
     */
    public Map<String, Object> getPlayHoursDistribution() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 使用单次 CASE WHEN 聚合查询替代8次独立查询
        QueryWrapper<Rating> wrapper = new QueryWrapper<>();
        wrapper.eq("is_valid", 1)
                .select(
                    "SUM(CASE WHEN play_hours = 0 THEN 1 ELSE 0 END) as bin_0",
                    "SUM(CASE WHEN play_hours > 0 AND play_hours <= 5 THEN 1 ELSE 0 END) as bin_1_5",
                    "SUM(CASE WHEN play_hours > 5 AND play_hours <= 20 THEN 1 ELSE 0 END) as bin_5_20",
                    "SUM(CASE WHEN play_hours > 20 AND play_hours <= 50 THEN 1 ELSE 0 END) as bin_20_50",
                    "SUM(CASE WHEN play_hours > 50 AND play_hours <= 100 THEN 1 ELSE 0 END) as bin_50_100",
                    "SUM(CASE WHEN play_hours > 100 AND play_hours <= 500 THEN 1 ELSE 0 END) as bin_100_500",
                    "SUM(CASE WHEN play_hours > 500 AND play_hours <= 2000 THEN 1 ELSE 0 END) as bin_500_2000",
                    "SUM(CASE WHEN play_hours > 2000 THEN 1 ELSE 0 END) as bin_2000_plus",
                    "ROUND(AVG(play_hours), 2) as avg_hours",
                    "MAX(play_hours) as max_hours",
                    "MIN(play_hours) as min_hours"
                );

        List<Map<String, Object>> statsList = ratingMapper.selectMaps(wrapper);

        String[] labels = {"0h（买了没玩）", "0.1-5h", "5-20h", "20-50h", "50-100h", "100-500h", "500-2000h", ">2000h"};
        int[] pseudoRatings = {1, 2, 3, 4, 5, 5, 5, 5};
        String[] binKeys = {"bin_0", "bin_1_5", "bin_5_20", "bin_20_50", "bin_50_100", "bin_100_500", "bin_500_2000", "bin_2000_plus"};

        List<Map<String, Object>> distribution = new ArrayList<>();
        long total = 0;

        if (!statsList.isEmpty()) {
            Map<String, Object> row = statsList.get(0);
            for (int i = 0; i < binKeys.length; i++) {
                long count = ((Number) row.getOrDefault(binKeys[i], 0L)).longValue();
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("range", labels[i]);
                item.put("count", count);
                item.put("pseudoRating", pseudoRatings[i]);
                distribution.add(item);
                total += count;
            }

            // 统计摘要
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("avg_hours", row.get("avg_hours"));
            stats.put("max_hours", row.get("max_hours"));
            stats.put("min_hours", row.get("min_hours"));
            result.put("stats", stats);
        }

        result.put("distribution", distribution);
        result.put("total", total);

        return result;
    }

    /**
     * 购买-游玩转化率分析（创新）
     *
     * @return 转化率分布数据
     */
    public Map<String, Object> getPurchasePlayRatio() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 总体转化率
        long totalPurchase = ratingMapper.selectCount(
                new QueryWrapper<Rating>().eq("is_valid", 1));
        long totalPlay = ratingMapper.selectCount(
                new QueryWrapper<Rating>().eq("is_valid", 1).gt("play_hours", 0));
        double overallRatio = totalPurchase > 0 ? (double) totalPlay / totalPurchase : 0;

        result.put("totalPurchase", totalPurchase);
        result.put("totalPlay", totalPlay);
        result.put("overallRatio", String.format("%.2f%%", overallRatio * 100));

        // 按用户分组统计转化率分布
        QueryWrapper<Rating> wrapper = new QueryWrapper<>();
        wrapper.eq("is_valid", 1)
                .select("user_id",
                        "count(*) as purchase_count",
                        "sum(case when play_hours > 0 then 1 else 0 end) as play_count")
                .groupBy("user_id");

        List<Map<String, Object>> userStats = ratingMapper.selectMaps(wrapper);

        int highConversion = 0;    // ≥80%
        int mediumConversion = 0;  // 50-80%
        int lowConversion = 0;     // <50%

        for (Map<String, Object> user : userStats) {
            long purchase = ((Number) user.get("purchase_count")).longValue();
            long play = ((Number) user.get("play_count")).longValue();
            double ratio = purchase > 0 ? (double) play / purchase : 0;
            if (ratio >= 0.8) highConversion++;
            else if (ratio >= 0.5) mediumConversion++;
            else lowConversion++;
        }

        List<Map<String, Object>> distribution = new ArrayList<>();
        Map<String, Object> high = new LinkedHashMap<>();
        high.put("level", "高转化率（≥80%）");
        high.put("count", highConversion);
        distribution.add(high);

        Map<String, Object> medium = new LinkedHashMap<>();
        medium.put("level", "中等转化率（50%-80%）");
        medium.put("count", mediumConversion);
        distribution.add(medium);

        Map<String, Object> low = new LinkedHashMap<>();
        low.put("level", "低转化率（<50%）");
        low.put("count", lowConversion);
        distribution.add(low);

        result.put("distribution", distribution);

        return result;
    }

    /**
     * 综合概览统计
     */
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();

        overview.put("userCount", ratingMapper.selectMaps(
                new QueryWrapper<Rating>().eq("is_valid", 1)
                        .select("distinct user_id").groupBy("user_id")).size());

        overview.put("gameCount", gameMapper.selectCount(null));

        overview.put("ratingCount", ratingMapper.selectCount(
                new QueryWrapper<Rating>().eq("is_valid", 1)));

        // 平均伪评分
        QueryWrapper<Rating> avgWrapper = new QueryWrapper<>();
        avgWrapper.eq("is_valid", 1).select("avg(rating) as avg_rating");
        List<Map<String, Object>> avgList = ratingMapper.selectMaps(avgWrapper);
        if (!avgList.isEmpty()) {
            overview.put("avgRating", avgList.get(0).get("avg_rating"));
        }

        // 平均游玩时长（所有有效评价）
        QueryWrapper<Rating> hoursWrapper = new QueryWrapper<>();
        hoursWrapper.eq("is_valid", 1).select("avg(play_hours) as avg_play_hours");
        List<Map<String, Object>> hoursList = ratingMapper.selectMaps(hoursWrapper);
        if (!hoursList.isEmpty()) {
            overview.put("avgPlayHours", hoursList.get(0).get("avg_play_hours"));
        }

        // 免费游戏数量
        overview.put("freeGameCount", gameMapper.selectCount(
                new QueryWrapper<Game>().eq("is_free", 1)));

        return overview;
    }

    // ==================== 新增：游戏评价分析 ====================

    /**
     * 游戏评价分布分析（利用 steam_games.csv 的 total_reviews/review_score 等字段）
     *
     * @return 评价分布数据
     */
    public Map<String, Object> getReviewDistribution() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 评价等级分布
        QueryWrapper<Game> wrapper = new QueryWrapper<>();
        wrapper.select("review_score_desc", "count(*) as count")
                .isNotNull("review_score_desc")
                .ne("review_score_desc", "")
                .groupBy("review_score_desc")
                .orderByDesc("count");

        List<Map<String, Object>> reviewLevels = gameMapper.selectMaps(wrapper);

        // 将 "N user reviews" (1-9条) 和 "No user reviews" 归并为 "评价不足"
        // 真实 Steam 用户评价等级包括:
        // Overwhelmingly Positive, Very Positive, Positive, Mostly Positive, Mixed,
        // Mostly Negative, Negative, Very Negative, Overwhelmingly Negative
        List<Map<String, Object>> distribution = new ArrayList<>();
        long insufficientCount = 0;
        long total = 0;

        Set<String> knownLevels = new HashSet<>(Arrays.asList(
                "Overwhelmingly Positive", "Very Positive", "Positive",
                "Mostly Positive", "Mixed", "Mostly Negative",
                "Negative", "Very Negative", "Overwhelmingly Negative"
        ));

        for (Map<String, Object> row : reviewLevels) {
            String desc = (String) row.get("review_score_desc");
            long count = ((Number) row.get("count")).longValue();

            if (knownLevels.contains(desc)) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("level", desc);
                item.put("levelCn", translateReviewDesc(desc));
                item.put("count", count);
                distribution.add(item);
                total += count;
            } else {
                // "No user reviews", "1 user reviews", "2 user reviews"... 等归并
                insufficientCount += count;
            }
        }

        // 添加归并后的 "评价不足" 分类
        if (insufficientCount > 0) {
            Map<String, Object> insufficient = new LinkedHashMap<>();
            insufficient.put("level", "Insufficient Reviews");
            insufficient.put("levelCn", "评价不足");
            insufficient.put("count", insufficientCount);
            distribution.add(insufficient);
            total += insufficientCount;
        }

        result.put("distribution", distribution);
        result.put("total", total);

        // Metacritic 评分分布
        QueryWrapper<Game> mcWrapper = new QueryWrapper<>();
        mcWrapper.select(
                "SUM(CASE WHEN metacritic >= 90 THEN 1 ELSE 0 END) as mc_90_plus",
                "SUM(CASE WHEN metacritic >= 75 AND metacritic < 90 THEN 1 ELSE 0 END) as mc_75_90",
                "SUM(CASE WHEN metacritic >= 50 AND metacritic < 75 THEN 1 ELSE 0 END) as mc_50_75",
                "SUM(CASE WHEN metacritic > 0 AND metacritic < 50 THEN 1 ELSE 0 END) as mc_lt_50",
                "SUM(CASE WHEN metacritic IS NULL OR metacritic = 0 THEN 1 ELSE 0 END) as mc_none",
                "ROUND(AVG(CASE WHEN metacritic > 0 THEN metacritic END), 1) as mc_avg"
        );
        List<Map<String, Object>> mcStats = gameMapper.selectMaps(mcWrapper);
        if (!mcStats.isEmpty()) {
            result.put("metacriticStats", mcStats.get(0));
        }

        // 评价数分布
        QueryWrapper<Game> reviewCountWrapper = new QueryWrapper<>();
        reviewCountWrapper.select(
                "SUM(CASE WHEN total_reviews >= 100000 THEN 1 ELSE 0 END) as massive",
                "SUM(CASE WHEN total_reviews >= 10000 AND total_reviews < 100000 THEN 1 ELSE 0 END) as many",
                "SUM(CASE WHEN total_reviews >= 1000 AND total_reviews < 10000 THEN 1 ELSE 0 END) as moderate",
                "SUM(CASE WHEN total_reviews >= 100 AND total_reviews < 1000 THEN 1 ELSE 0 END) as few",
                "SUM(CASE WHEN total_reviews > 0 AND total_reviews < 100 THEN 1 ELSE 0 END) as very_few",
                "SUM(CASE WHEN total_reviews = 0 OR total_reviews IS NULL THEN 1 ELSE 0 END) as none"
        );
        List<Map<String, Object>> reviewCountStats = gameMapper.selectMaps(reviewCountWrapper);
        if (!reviewCountStats.isEmpty()) {
            result.put("reviewCountStats", reviewCountStats.get(0));
        }

        return result;
    }

    /**
     * 最高评分游戏 Top-N（利用 Metacritic + 好评率综合排序）
     */
    public List<Map<String, Object>> getTopCriticGames(int limit) {
        QueryWrapper<Game> wrapper = new QueryWrapper<>();
        wrapper.select("game_id", "game_name", "game_name_cn",
                        "metacritic", "positive_percentual", "review_score_desc",
                        "total_reviews", "genres", "genres_cn", "developer", "publisher")
                .isNotNull("metacritic")
                .gt("metacritic", 0)
                .gt("total_reviews", 100)
                .orderByDesc("metacritic")
                .last("LIMIT " + limit);

        return gameMapper.selectMaps(wrapper);
    }

    /**
     * 最多评价游戏 Top-N
     */
    public List<Map<String, Object>> getTopReviewedGames(int limit) {
        QueryWrapper<Game> wrapper = new QueryWrapper<>();
        wrapper.select("game_id", "game_name", "game_name_cn",
                        "total_reviews", "positive_percentual", "review_score_desc",
                        "genres", "genres_cn", "metacritic")
                .gt("total_reviews", 0)
                .orderByDesc("total_reviews")
                .last("LIMIT " + limit);

        return gameMapper.selectMaps(wrapper);
    }

    // ==================== 新增：开发商/发行商分析 ====================

    /**
     * 顶级开发商分析（按发行游戏数量排序）
     */
    public List<Map<String, Object>> getTopDevelopers(int limit) {
        QueryWrapper<Game> wrapper = new QueryWrapper<>();
        wrapper.select("developer", "count(*) as game_count",
                        "ROUND(AVG(positive_percentual), 3) as avg_positive_rate",
                        "ROUND(AVG(metacritic), 1) as avg_metacritic",
                        "SUM(total_reviews) as total_reviews")
                .isNotNull("developer")
                .ne("developer", "")
                .groupBy("developer")
                .orderByDesc("game_count")
                .last("LIMIT " + limit);

        return gameMapper.selectMaps(wrapper);
    }

    /**
     * 顶级发行商分析（按发行游戏数量排序）
     */
    public List<Map<String, Object>> getTopPublishers(int limit) {
        QueryWrapper<Game> wrapper = new QueryWrapper<>();
        wrapper.select("publisher", "count(*) as game_count",
                        "ROUND(AVG(positive_percentual), 3) as avg_positive_rate",
                        "ROUND(AVG(metacritic), 1) as avg_metacritic",
                        "SUM(total_reviews) as total_reviews")
                .isNotNull("publisher")
                .ne("publisher", "")
                .groupBy("publisher")
                .orderByDesc("game_count")
                .last("LIMIT " + limit);

        return gameMapper.selectMaps(wrapper);
    }

    /**
     * 平台分布分析
     */
    public Map<String, Object> getPlatformDistribution() {
        Map<String, Object> result = new LinkedHashMap<>();

        QueryWrapper<Game> wrapper = new QueryWrapper<>();
        wrapper.select(
                "SUM(CASE WHEN platforms LIKE '%windows%' THEN 1 ELSE 0 END) as windows",
                "SUM(CASE WHEN platforms LIKE '%mac%' THEN 1 ELSE 0 END) as mac",
                "SUM(CASE WHEN platforms LIKE '%linux%' THEN 1 ELSE 0 END) as linux"
        );
        List<Map<String, Object>> stats = gameMapper.selectMaps(wrapper);
        if (!stats.isEmpty()) {
            result.put("platforms", stats.get(0));
        }
        result.put("totalGames", gameMapper.selectCount(null));

        return result;
    }

    /**
     * 价格分布分析
     */
    public Map<String, Object> getPriceDistribution() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 免费游戏数量
        long freeCount = gameMapper.selectCount(
                new QueryWrapper<Game>().eq("is_free", 1));

        // 付费游戏价格分布
        QueryWrapper<Game> priceWrapper = new QueryWrapper<>();
        priceWrapper.select(
                "SUM(CASE WHEN price > 0 AND price <= 5 THEN 1 ELSE 0 END) as price_0_5",
                "SUM(CASE WHEN price > 5 AND price <= 10 THEN 1 ELSE 0 END) as price_5_10",
                "SUM(CASE WHEN price > 10 AND price <= 20 THEN 1 ELSE 0 END) as price_10_20",
                "SUM(CASE WHEN price > 20 AND price <= 30 THEN 1 ELSE 0 END) as price_20_30",
                "SUM(CASE WHEN price > 30 AND price <= 60 THEN 1 ELSE 0 END) as price_30_60",
                "SUM(CASE WHEN price > 60 THEN 1 ELSE 0 END) as price_60_plus",
                "ROUND(AVG(CASE WHEN price > 0 THEN price END), 2) as avg_price",
                "MAX(price) as max_price"
        );
        List<Map<String, Object>> priceStats = gameMapper.selectMaps(priceWrapper);

        long paidCount = gameMapper.selectCount(
                new QueryWrapper<Game>().eq("is_free", 0));

        result.put("freeCount", freeCount);
        result.put("paidCount", paidCount);
        result.put("total", freeCount + paidCount);
        if (!priceStats.isEmpty()) {
            result.put("priceStats", priceStats.get(0));
        }

        return result;
    }

    // ==================== 词云数据 ====================

    /**
     * 获取游戏标签词云数据
     * 从 steamspy_tags_cn 和 genres_cn 中提取高频标签，用于前端词云可视化
     */
    public List<Map<String, Object>> getTagCloudData() {
        // 查询所有游戏的标签和类型字段
        QueryWrapper<Game> wrapper = new QueryWrapper<>();
        wrapper.select("steamspy_tags_cn", "genres_cn", "total_reviews");
        List<Map<String, Object>> games = gameMapper.selectMaps(wrapper);

        // 统计所有标签出现次数，并加权 total_reviews
        Map<String, Long> tagCount = new HashMap<>();
        Map<String, Long> tagWeight = new HashMap<>();

        for (Map<String, Object> game : games) {
            String tags = (String) game.get("steamspy_tags_cn");
            String genres = (String) game.get("genres_cn");
            long reviews = game.get("total_reviews") != null
                    ? ((Number) game.get("total_reviews")).longValue() : 0;
            reviews = Math.max(1, reviews);

            // 处理 steamspy_tags
            if (tags != null && !tags.isEmpty()) {
                for (String tag : tags.split("[;；]")) {
                    String t = tag.trim();
                    if (!t.isEmpty() && t.length() < 15) {
                        tagCount.merge(t, 1L, Long::sum);
                        tagWeight.merge(t, reviews, Long::sum);
                    }
                }
            }

            // 处理 genres
            if (genres != null && !genres.isEmpty()) {
                for (String g : genres.split("[;；]")) {
                    String gg = g.trim();
                    if (!gg.isEmpty() && gg.length() < 15) {
                        tagCount.merge(gg, 1L, Long::sum);
                        tagWeight.merge(gg, reviews, Long::sum);
                    }
                }
            }
        }

        // 转换为输出格式，按加权频率排序
        List<Map<String, Object>> result = tagCount.entrySet().stream()
                .map(entry -> {
                    String name = entry.getKey();
                    long count = entry.getValue();
                    long weight = tagWeight.getOrDefault(name, 0L);
                    // 综合权重 = 出现次数 * 0.3 + 评价总量权重 * 0.7
                    double score = count * 0.3 + Math.log(weight + 1) * 0.7;
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", name);
                    item.put("count", count);
                    item.put("weight", weight);
                    item.put("score", Math.round(score * 100.0) / 100.0);
                    return item;
                })
                .sorted((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")))
                .limit(120)
                .collect(Collectors.toList());

        return result;
    }

    // ==================== 新增：雷达图数据质量指标 ====================

    /**
     * 获取数据质量雷达图指标（6 维度，0-100）
     *
     * 指标定义：
     *   dataCompleteness  — 游戏核心字段非空率（game_name_cn / genres_cn / developer / publisher）
     *   reviewCoverage    — 有 Steam 评价的游戏占比
     *   genreDiversity    — 类型多样性（实际类型数 / 10 × 100，上限 100）
     *   priceReasonability — 价格合理度（100 - 异常高价比例 × 100）
     *   activity          — 用户活跃度（有游玩记录的用户占比）
     *   playability       — 游戏可玩性（有游玩行为的游戏占比）
     *
     * @return 6 项指标字典
     */
    public Map<String, Object> getRadarMetrics() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. dataCompleteness：核心字段非空率
        QueryWrapper<Game> completeWrapper = new QueryWrapper<>();
        completeWrapper.select(
                "SUM(CASE WHEN game_name_cn IS NOT NULL AND game_name_cn != '' THEN 1 ELSE 0 END) as cn_count",
                "SUM(CASE WHEN genres_cn IS NOT NULL AND genres_cn != '' THEN 1 ELSE 0 END) as genre_cn_count",
                "SUM(CASE WHEN developer IS NOT NULL AND developer != '' THEN 1 ELSE 0 END) as dev_count",
                "SUM(CASE WHEN publisher IS NOT NULL AND publisher != '' THEN 1 ELSE 0 END) as pub_count",
                "COUNT(*) as total"
        );
        List<Map<String, Object>> compStats = gameMapper.selectMaps(completeWrapper);
        double dataCompleteness = 0;
        if (!compStats.isEmpty()) {
            Map<String, Object> row = compStats.get(0);
            long total = ((Number) row.get("total")).longValue();
            long cn = ((Number) row.get("cn_count")).longValue();
            long genreCn = ((Number) row.get("genre_cn_count")).longValue();
            long dev = ((Number) row.get("dev_count")).longValue();
            long pub = ((Number) row.get("pub_count")).longValue();
            dataCompleteness = total > 0
                    ? Math.round((cn + genreCn + dev + pub) * 100.0 / (total * 4.0))
                    : 0;
        }

        // 2. reviewCoverage：有 Steam 评价的游戏占比
        long totalGames = gameMapper.selectCount(null);
        long reviewedGames = gameMapper.selectCount(
                new QueryWrapper<Game>().isNotNull("review_score_desc")
                        .ne("review_score_desc", "")
                        .ne("review_score_desc", "No user reviews"));
        long reviewCoverage = totalGames > 0
                ? Math.round(reviewedGames * 100.0 / totalGames) : 0;

        // 3. genreDiversity：类型多样性
        QueryWrapper<Game> genreWrapper = new QueryWrapper<>();
        genreWrapper.select("genres_cn").isNotNull("genres_cn").ne("genres_cn", "");
        List<Map<String, Object>> genreRows = gameMapper.selectMaps(genreWrapper);
        Set<String> allGenres = new HashSet<>();
        for (Map<String, Object> row : genreRows) {
            String gc = (String) row.get("genres_cn");
            if (gc != null) {
                for (String g : gc.split("[;；]")) {
                    String gg = g.trim();
                    if (!gg.isEmpty()) allGenres.add(gg);
                }
            }
        }
        long genreDiversity = Math.min(100, Math.round(allGenres.size() * 100.0 / 10.0));

        // 4. priceReasonability：价格合理度（排除价格异常高的游戏 > $60）
        long totalPaid = gameMapper.selectCount(
                new QueryWrapper<Game>().eq("is_free", 0));
        long reasonablePrice = gameMapper.selectCount(
                new QueryWrapper<Game>().eq("is_free", 0)
                        .gt("price", 0).le("price", 60));
        long priceReasonability = totalPaid > 0
                ? Math.round(reasonablePrice * 100.0 / totalPaid) : 0;

        // 5. activity：有游玩记录的用户占比
        long totalUsers = ratingMapper.selectMaps(
                new QueryWrapper<Rating>().eq("is_valid", 1)
                        .select("distinct user_id").groupBy("user_id")).size();
        long activeUsers = ratingMapper.selectMaps(
                new QueryWrapper<Rating>().eq("is_valid", 1).gt("play_hours", 0)
                        .select("distinct user_id").groupBy("user_id")).size();
        long activity = totalUsers > 0
                ? Math.round(activeUsers * 100.0 / totalUsers) : 0;

        // 6. playability：有游玩行为的游戏占比
        long totalRatedGames = ratingMapper.selectMaps(
                new QueryWrapper<Rating>().eq("is_valid", 1)
                        .select("distinct game_id").groupBy("game_id")).size();
        long playedGames = ratingMapper.selectMaps(
                new QueryWrapper<Rating>().eq("is_valid", 1).gt("play_hours", 0)
                        .select("distinct game_id").groupBy("game_id")).size();
        long playability = totalRatedGames > 0
                ? Math.round(playedGames * 100.0 / totalRatedGames) : 0;

        List<Map<String, Object>> metrics = new ArrayList<>();
        String[] names = {"数据完整性", "评价覆盖率", "类型多样性", "价格合理度", "用户活跃度", "游戏可玩性"};
        long[] values = {Math.round(dataCompleteness), reviewCoverage, genreDiversity, priceReasonability, activity, playability};
        for (int i = 0; i < names.length; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", names[i]);
            m.put("value", values[i]);
            metrics.add(m);
        }
        result.put("metrics", metrics);
        result.put("maxValue", 100);
        result.put("avgValue", Math.round(Arrays.stream(values).average().orElse(0)));

        return result;
    }

    // ==================== 新增：发行年份趋势数据（用于走势图） ====================

    /**
     * 获取按游戏发行年份聚合的趋势数据（因 create_time 均为同一天，改用 release_date）
     *
     * 返回近 20 年的数据，替代前端的 30 天模拟数据
     *
     * @param periods 返回的数据点数量（默认 20）
     * @return 趋势数据
     */
    public Map<String, Object> getReleaseYearTrends(int periods) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 步骤1：取得所有游戏的 game_id 和 release_year
        QueryWrapper<Game> basicWrapper = new QueryWrapper<>();
        basicWrapper.select("game_id", "YEAR(release_date) as release_year")
                .isNotNull("release_date")
                .gt("YEAR(release_date)", 0)
                .last("ORDER BY release_year DESC");
        List<Map<String, Object>> gameYears = gameMapper.selectMaps(basicWrapper);

        // 步骤2：按 game_id 聚合 rating
        QueryWrapper<Rating> ratingAggWrapper = new QueryWrapper<>();
        ratingAggWrapper.eq("is_valid", 1)
                .select("game_id", "AVG(rating) as avg_rating", "COUNT(*) as rating_count",
                        "SUM(CASE WHEN play_hours > 0 THEN 1 ELSE 0 END) as play_count")
                .groupBy("game_id");
        List<Map<String, Object>> ratingAgg = ratingMapper.selectMaps(ratingAggWrapper);

        // 构建 gameId → ratingStats 快速映射
        Map<Long, Double> avgRatingMap = new HashMap<>();
        Map<Long, Long> ratingCountMap = new HashMap<>();
        Map<Long, Long> playCountMap = new HashMap<>();
        for (Map<String, Object> r : ratingAgg) {
            Long gid = ((Number) r.get("game_id")).longValue();
            avgRatingMap.put(gid, r.get("avg_rating") != null
                    ? ((Number) r.get("avg_rating")).doubleValue() : 0);
            ratingCountMap.put(gid, r.get("rating_count") != null
                    ? ((Number) r.get("rating_count")).longValue() : 0);
            playCountMap.put(gid, r.get("play_count") != null
                    ? ((Number) r.get("play_count")).longValue() : 0);
        }

        // 步骤3：按年份聚合
        Map<Integer, Long> yearGameCount = new TreeMap<>();
        Map<Integer, Double> yearRatingSum = new TreeMap<>();
        Map<Integer, Long> yearRatedGames = new TreeMap<>();
        Map<Integer, Long> yearPlaySum = new TreeMap<>();

        for (Map<String, Object> g : gameYears) {
            Long gid = ((Number) g.get("game_id")).longValue();
            int year = ((Number) g.get("release_year")).intValue();
            yearGameCount.merge(year, 1L, Long::sum);
            Double avgR = avgRatingMap.get(gid);
            if (avgR != null && avgR > 0) {
                yearRatingSum.merge(year, avgR, Double::sum);
                yearRatedGames.merge(year, 1L, Long::sum);
            }
            Long pc = playCountMap.get(gid);
            if (pc != null) {
                yearPlaySum.merge(year, pc, Long::sum);
            }
        }

        // 取最近的 periods 个年份
        List<Integer> sortedYears = new ArrayList<>(yearGameCount.keySet());
        Collections.sort(sortedYears);
        if (sortedYears.size() > periods) {
            sortedYears = sortedYears.subList(sortedYears.size() - periods, sortedYears.size());
        }

        List<String> labels = new ArrayList<>();
        List<Double> avgRatingSeries = new ArrayList<>();
        List<Long> gameCountSeries = new ArrayList<>();
        List<Long> playCountSeries = new ArrayList<>();

        for (int year : sortedYears) {
            labels.add(String.valueOf(year));
            long rg = yearRatedGames.getOrDefault(year, 0L);
            double rs = yearRatingSum.getOrDefault(year, 0.0);
            avgRatingSeries.add(rg > 0 ? Math.round(rs * 100.0 / rg) / 100.0 : 0.0);
            gameCountSeries.add(yearGameCount.getOrDefault(year, 0L));
            playCountSeries.add(yearPlaySum.getOrDefault(year, 0L));
        }

        result.put("labels", labels);
        result.put("avgRating", avgRatingSeries);
        result.put("gameCount", gameCountSeries);
        result.put("playCount", playCountSeries);
        result.put("periods", sortedYears.size());

        return result;
    }

    // ==================== 新增：质量监控指标 ====================

    /**
     * 获取数据质量监控统计（基于 is_valid 逻辑删除标记）
     *
     * @return 质量统计数据
     */
    public Map<String, Object> getQualityStats() {
        Map<String, Object> result = new LinkedHashMap<>();

        long totalRatings = ratingMapper.selectCount(null);
        long validRatings = ratingMapper.selectCount(
                new QueryWrapper<Rating>().eq("is_valid", 1));
        long invalidRatings = totalRatings - validRatings;

        // 异常游玩时长统计（>10000h 视为异常）
        long anomalyPlayHours = ratingMapper.selectCount(
                new QueryWrapper<Rating>().eq("is_valid", 1)
                        .gt("play_hours", 10000));
        // 高风险：is_valid=0 的数据
        long highRisk = invalidRatings;

        // 质量率：有效数据占比
        long qualityRate = totalRatings > 0
                ? Math.round(validRatings * 100.0 / totalRatings) : 0;

        result.put("totalRatings", totalRatings);
        result.put("validRatings", validRatings);
        result.put("invalidRatings", invalidRatings);
        result.put("qualityRate", qualityRate);
        result.put("anomalyCount", anomalyPlayHours);
        result.put("highRiskCount", highRisk);

        return result;
    }

    // ==================== 新增：KPI 迷你趋势数据 ====================

    /**
     * 获取 KPI 卡片迷你趋势数据（4 组 × 20 点，用于首页 Home.vue）
     *
     * 使用发行年份维度替代时间序列
     *   series[0] — 平均评分趋势（近 20 年）
     *   series[1] — 免费游戏占比趋势
     *   series[2] — 转化率趋势（游玩/购买）
     *   series[3] — 平均游玩时长趋势
     *
     * @param points 每组数据点数量（默认 20）
     * @return 4 组趋势数据
     */
    public Map<String, Object> getKpiTrends(int points) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 获取所有发行年份的游戏和评分数据
        QueryWrapper<Game> gw = new QueryWrapper<>();
        gw.select("game_id", "YEAR(release_date) as release_year", "is_free", "price")
                .isNotNull("release_date").gt("YEAR(release_date)", 0);
        List<Map<String, Object>> games = gameMapper.selectMaps(gw);

        // rating 聚合
        QueryWrapper<Rating> rw = new QueryWrapper<>();
        rw.eq("is_valid", 1)
                .select("game_id", "AVG(rating) as avg_r", "AVG(play_hours) as avg_h",
                        "COUNT(*) as cnt", "SUM(CASE WHEN play_hours > 0 THEN 1 ELSE 0 END) as play_cnt")
                .groupBy("game_id");
        List<Map<String, Object>> ratingAgg = ratingMapper.selectMaps(rw);

        Map<Long, Double> gAvgRating = new HashMap<>();
        Map<Long, Double> gAvgHours = new HashMap<>();
        Map<Long, Long> gPlayCount = new HashMap<>();
        Map<Long, Long> gTotalCount = new HashMap<>();
        for (Map<String, Object> r : ratingAgg) {
            Long gid = ((Number) r.get("game_id")).longValue();
            if (r.get("avg_r") != null) gAvgRating.put(gid, ((Number) r.get("avg_r")).doubleValue());
            if (r.get("avg_h") != null) gAvgHours.put(gid, ((Number) r.get("avg_h")).doubleValue());
            if (r.get("play_cnt") != null) gPlayCount.put(gid, ((Number) r.get("play_cnt")).longValue());
            if (r.get("cnt") != null) gTotalCount.put(gid, ((Number) r.get("cnt")).longValue());
        }

        // 按年份聚合
        Map<Integer, List<Double>> yearRatings = new TreeMap<>();
        Map<Integer, List<Double>> yearHours = new TreeMap<>();
        Map<Integer, Long> yearFreeCount = new TreeMap<>();
        Map<Integer, Long> yearTotalGames = new TreeMap<>();
        Map<Integer, Long> yearPlayCount = new TreeMap<>();
        Map<Integer, Long> yearPurchaseCount = new TreeMap<>();

        for (Map<String, Object> g : games) {
            Long gid = ((Number) g.get("game_id")).longValue();
            int yr = ((Number) g.get("release_year")).intValue();
            int isFree = g.get("is_free") != null ? ((Number) g.get("is_free")).intValue() : 0;

            yearTotalGames.merge(yr, 1L, Long::sum);
            if (isFree == 1) yearFreeCount.merge(yr, 1L, Long::sum);

            Double ar = gAvgRating.get(gid);
            Double ah = gAvgHours.get(gid);
            if (ar != null && ar > 0) {
                yearRatings.computeIfAbsent(yr, k -> new ArrayList<>()).add(ar);
            }
            if (ah != null && ah > 0) {
                yearHours.computeIfAbsent(yr, k -> new ArrayList<>()).add(ah);
            }
            Long pc = gPlayCount.get(gid);
            Long tc = gTotalCount.get(gid);
            if (pc != null) yearPlayCount.merge(yr, pc, Long::sum);
            if (tc != null) yearPurchaseCount.merge(yr, tc, Long::sum);
        }

        // 取最近 points 个年份
        List<Integer> years = new ArrayList<>(yearTotalGames.keySet());
        Collections.sort(years);
        if (years.size() > points) {
            years = years.subList(years.size() - points, years.size());
        }

        List<Double> avgRatingTrend = new ArrayList<>();
        List<Double> freeRatioTrend = new ArrayList<>();
        List<Double> conversionTrend = new ArrayList<>();
        List<Double> avgHoursTrend = new ArrayList<>();

        for (int yr : years) {
            // 平均评分
            List<Double> ratings = yearRatings.get(yr);
            if (ratings != null && !ratings.isEmpty()) {
                double sum = 0;
                for (Double d : ratings) sum += d;
                avgRatingTrend.add(Math.round(sum * 100.0 / ratings.size()) / 100.0);
            } else {
                avgRatingTrend.add(0.0);
            }
            // 免费占比
            long tgc = yearTotalGames.getOrDefault(yr, 0L);
            long fgc = yearFreeCount.getOrDefault(yr, 0L);
            freeRatioTrend.add(tgc > 0 ? Math.round(fgc * 10000.0 / tgc) / 100.0 : 0.0);
            // 转化率（游玩数 / 购买数）
            long playC = yearPlayCount.getOrDefault(yr, 0L);
            long purchC = yearPurchaseCount.getOrDefault(yr, 0L);
            conversionTrend.add(purchC > 0 ? Math.round(playC * 10000.0 / purchC) / 100.0 : 0.0);
            // 平均游玩时长
            List<Double> hours = yearHours.get(yr);
            if (hours != null && !hours.isEmpty()) {
                double sum = 0;
                for (Double d : hours) sum += d;
                avgHoursTrend.add(Math.round(sum * 100.0 / hours.size()) / 100.0);
            } else {
                avgHoursTrend.add(0.0);
            }
        }

        result.put("labels", years);
        result.put("series", new Object[]{avgRatingTrend, freeRatioTrend, conversionTrend, avgHoursTrend});

        return result;
    }

    // ==================== 新增：类型卡片迷你走势数据 ====================

    /**
     * 获取 6 个主要游戏类型的发展趋势（按发行年份统计该类型游戏数量变化）
     *
     * @param points 每组数据点数量（默认 30）
     * @return 类型趋势数据
     */
    public Map<String, Object> getGenreTrends(int points) {
        Map<String, Object> result = new LinkedHashMap<>();

        final String[] GENRE_TARGETS = {"动作", "角色扮演", "策略", "独立", "冒险", "休闲"};
        final String[][] GENRE_PATTERNS = {
                {"动作", "射击", "FPS", "TPS", "格斗", "ACT"},
                {"角色扮演", "RPG", "养成", "Rogue", "ARPG"},
                {"策略", "战棋", "SLG", "塔防", "模拟"},
                {"独立", "小品", "解谜", "Puzzle"},
                {"冒险", "探索", "开放世界", "沙盒"},
                {"休闲", "益智", "卡牌", "音乐", "Casual"}
        };

        // 查询所有游戏的发行年份和类型
        QueryWrapper<Game> gw = new QueryWrapper<>();
        gw.select("game_id", "YEAR(release_date) as release_year", "genres_cn", "steamspy_tags_cn")
                .isNotNull("release_date").gt("YEAR(release_date)", 0);
        List<Map<String, Object>> games = gameMapper.selectMaps(gw);

        // 按年份统计各类型游戏数
        // Map<年份, Map<类型索引, 计数>>
        Map<Integer, long[]> yearGenreCounts = new TreeMap<>();

        for (Map<String, Object> g : games) {
            int yr = ((Number) g.get("release_year")).intValue();
            String genresCn = (String) g.get("genres_cn");
            String tagsCn = (String) g.get("steamspy_tags_cn");
            String combined = (genresCn != null ? genresCn : "")
                    + ";" + (tagsCn != null ? tagsCn : "");

            yearGenreCounts.putIfAbsent(yr, new long[GENRE_TARGETS.length]);

            long[] counts = yearGenreCounts.get(yr);
            for (int i = 0; i < GENRE_PATTERNS.length; i++) {
                for (String pattern : GENRE_PATTERNS[i]) {
                    if (combined.contains(pattern)) {
                        counts[i]++;
                        break;
                    }
                }
            }
        }

        // 取最近 points 个年份
        List<Integer> years = new ArrayList<>(yearGenreCounts.keySet());
        Collections.sort(years);
        if (years.size() > points) {
            years = years.subList(years.size() - points, years.size());
        }

        List<String> labels = new ArrayList<>();
        for (int yr : years) labels.add(String.valueOf(yr));

        List<List<Long>> series = new ArrayList<>();
        for (int i = 0; i < GENRE_TARGETS.length; i++) {
            List<Long> serie = new ArrayList<>();
            for (int yr : years) {
                long[] counts = yearGenreCounts.get(yr);
                serie.add(counts != null ? counts[i] : 0L);
            }
            series.add(serie);
        }

        result.put("labels", labels);
        result.put("genreNames", Arrays.asList(GENRE_TARGETS));
        result.put("series", series);

        return result;
    }

    // ==================== 工具方法 ====================

    private String getRatingMeaning(int rating) {
        switch (rating) {
            case 1: return "不感兴趣（买了没玩）";
            case 2: return "尝试一下（0~5h）";
            case 3: return "一般喜欢（5~20h）";
            case 4: return "比较喜欢（20~50h）";
            case 5: return "非常喜欢（>50h）";
            default: return "未知";
        }
    }

    private String translateReviewDesc(String desc) {
        if (desc == null) return "未知";
        switch (desc) {
            case "Overwhelmingly Positive": return "好评如潮";
            case "Very Positive": return "特别好评";
            case "Positive": return "好评";
            case "Mostly Positive": return "多半好评";
            case "Mixed": return "褒贬不一";
            case "Mostly Negative": return "多半差评";
            case "Negative": return "差评";
            case "Very Negative": return "特别差评";
            case "Overwhelmingly Negative": return "差评如潮";
            case "No user reviews": return "暂无评价";
            default: return desc;
        }
    }
}
