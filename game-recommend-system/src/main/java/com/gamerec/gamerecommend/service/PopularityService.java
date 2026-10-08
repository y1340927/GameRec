package com.gamerec.gamerecommend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gamerec.gamerecommend.entity.Game;
import com.gamerec.gamerecommend.entity.RatingTrain;
import com.gamerec.gamerecommend.mapper.GameMapper;
import com.gamerec.gamerecommend.mapper.RatingTrainMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 流行度推荐服务（增强版）
 *
 * 用于冷启动场景（新用户无历史行为数据）
 *
 * 改进公式：综合热度 = Steam真实评价数×0.5 + 系统购买数×0.25 + 系统游玩数×0.25
 * 结合 Steam 真实数据与系统模拟行为数据，提升冷启动推荐质量。
 *
 * @author GameRec Team
 */
@Slf4j
@Service
public class PopularityService {

    /** 【问题 2.2】训练阶段只读训练集 */
    @Autowired
    private RatingTrainMapper ratingTrainMapper;

    @Autowired
    private GameMapper gameMapper;

    /**
     * 计算游戏流行度（改进版 - 综合真实与模拟数据）
     *
     * @param topN 返回数量
     * @return 热门游戏列表
     */
    public List<Map<String, Object>> getPopularGames(int topN) {
        long startTime = System.currentTimeMillis();

        // 1. 从模拟行为数据统计
        QueryWrapper<RatingTrain> wrapper = new QueryWrapper<>();
        wrapper.eq("is_valid", 1)
                .select("game_id", "count(*) as purchase_count",
                        "SUM(CASE WHEN play_hours > 0 THEN 1 ELSE 0 END) as play_count",
                        "avg(play_hours) as avg_hours",
                        "avg(rating) as avg_rating")
                .groupBy("game_id");
        List<Map<String, Object>> stats = ratingTrainMapper.selectMaps(wrapper);

        // 2. 批量查询对应游戏的 Steam 真实数据
        List<Long> gameIds = stats.stream()
                .map(r -> ((Number) r.get("game_id")).longValue())
                .collect(Collectors.toList());
        Map<Long, Game> gameMap = new HashMap<>();
        if (!gameIds.isEmpty()) {
            List<Game> games = gameMapper.selectList(
                    new QueryWrapper<Game>().in("game_id", gameIds));
            for (Game g : games) {
                gameMap.put(g.getGameId(), g);
            }
        }

        // 3. 综合评分：Steam真实数据(50%) + 系统购买数(25%) + 系统游玩数(25%)
        List<Map<String, Object>> scored = new ArrayList<>();
        for (Map<String, Object> row : stats) {
            Long gameId = ((Number) row.get("game_id")).longValue();
            long purchaseCount = ((Number) row.get("purchase_count")).longValue();
            long playCount = row.get("play_count") != null ?
                    ((Number) row.get("play_count")).longValue() : 0;

            Game game = gameMap.get(gameId);
            long steamReviews = game != null && game.getTotalReviews() != null ?
                    game.getTotalReviews() : 0;
            long steamPositive = game != null && game.getPositiveRatings() != null ?
                    game.getPositiveRatings() : 0;

            // 综合评分公式
            double steamScore = steamReviews * 0.4 + steamPositive * 0.1;
            double simScore = purchaseCount * 0.25 + playCount * 0.25;
            double popularity = steamScore + simScore;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("gameId", gameId);
            item.put("purchaseCount", purchaseCount);
            item.put("playCount", playCount);
            item.put("hotScore", Math.round(popularity * 100.0) / 100.0);
            if (game != null) {
                item.put("gameName", game.getGameName());
                item.put("gameNameCn", game.getGameNameCn());
                item.put("genres", game.getGenres());
                item.put("genresCn", game.getGenresCn());
                item.put("totalReviews", game.getTotalReviews());
                item.put("positiveRatings", game.getPositiveRatings());
                item.put("negativeRatings", game.getNegativeRatings());
                item.put("positivePercentual", game.getPositivePercentual());
                item.put("metacritic", game.getMetacritic());
                item.put("developer", game.getDeveloper());
                item.put("publisher", game.getPublisher());
                item.put("price", game.getPrice());
                item.put("isFree", game.getIsFree());
                item.put("reviewScoreDesc", game.getReviewScoreDesc());
                item.put("releaseDate", game.getReleaseDate());
            }
            scored.add(item);
        }

        // 4. 按热度降序
        scored.sort((a, b) -> Double.compare(
                (Double) b.get("hotScore"), (Double) a.get("hotScore")));

        List<Map<String, Object>> results = scored.stream()
                .limit(topN)
                .collect(Collectors.toList());

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("流行度推荐完成(增强版): topN={}, 耗时{}ms", topN, elapsed);

        return results;
    }

    /**
     * 推荐（用于冷启动）
     *
     * @param topN 返回数量
     * @return 热门游戏列表
     */
    public List<Map<String, Object>> recommend(int topN) {
        return getPopularGames(topN);
    }
}
