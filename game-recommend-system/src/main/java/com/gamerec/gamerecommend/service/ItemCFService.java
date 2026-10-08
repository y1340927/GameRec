package com.gamerec.gamerecommend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gamerec.gamerecommend.entity.Game;
import com.gamerec.gamerecommend.mapper.GameMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Item-CF 协同过滤推荐服务
 *
 * 核心思想："喜欢物品A的用户，也可能喜欢与A相似的物品B"
 *
 * 算法流程：
 *   1. 构建游戏共现矩阵（离线计算）
 *   2. 计算游戏间相似度：sim(i,j) = co_count(i,j) / √(count(i) × count(j))
 *   3. 基于用户已玩过的高分游戏，推荐相似游戏
 *
 * @author GameRec Team
 */
@Slf4j
@Service
public class ItemCFService {

    @Autowired
    private MatrixService matrixService;

    @Autowired
    private GameMapper gameMapper;

    /** 游戏相似度缓存：gameId -> (gameId -> similarity) */
    private Map<Long, Map<Long, Double>> itemSimilarityCache;

    /** 每个游戏的评分人数 */
    private Map<Long, Integer> itemCounts;

    private boolean built = false;

    /**
     * 构建游戏相似度矩阵
     *
     * @return 构建统计
     */
    public Map<String, Object> buildItemSimilarity() {
        long startTime = System.currentTimeMillis();

        Map<Long, Map<Long, Integer>> ratingMatrix = matrixService.getRatingMatrix();
        Map<Long, Map<Long, Integer>> cooccurrence = new ConcurrentHashMap<>();
        itemCounts = new ConcurrentHashMap<>();

        // 1. 构建共现矩阵
        // 【缺陷修复】旧实现遍历 invertedMatrix（game -> user -> rating），
        // 把"给该游戏打高分"的用户 ID 当成游戏 ID 两两配对，导致缓存键为 userId，
        // recommend() 中 itemSimilarityCache.get(gameId) 永远 miss，推荐恒为空。
        // 正确语义：对每个用户的 ≥4 分游戏两两配对，统计游戏间共现次数。
        for (Map.Entry<Long, Map<Long, Integer>> entry : ratingMatrix.entrySet()) {
            Map<Long, Integer> gameRatings = entry.getValue();

            // 只考虑高分游戏（≥4分）
            List<Long> highRatedGames = gameRatings.entrySet().stream()
                    .filter(e -> e.getValue() >= 4)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            // 两两配对
            for (int i = 0; i < highRatedGames.size(); i++) {
                Long gameI = highRatedGames.get(i);
                itemCounts.merge(gameI, 1, Integer::sum);

                cooccurrence.computeIfAbsent(gameI, k -> new ConcurrentHashMap<>());

                for (int j = i + 1; j < highRatedGames.size(); j++) {
                    Long gameJ = highRatedGames.get(j);

                    cooccurrence.get(gameI).merge(gameJ, 1, Integer::sum);

                    cooccurrence.computeIfAbsent(gameJ, k -> new ConcurrentHashMap<>())
                            .merge(gameI, 1, Integer::sum);
                }
            }
        }

        // 2. 计算相似度：sim(i,j) = co_count / √(count(i) × count(j))
        itemSimilarityCache = new ConcurrentHashMap<>();

        for (Map.Entry<Long, Map<Long, Integer>> entry : cooccurrence.entrySet()) {
            Long gameI = entry.getKey();
            int countI = itemCounts.getOrDefault(gameI, 1);

            Map<Long, Double> similarities = new ConcurrentHashMap<>();
            for (Map.Entry<Long, Integer> coEntry : entry.getValue().entrySet()) {
                Long gameJ = coEntry.getKey();
                int countJ = itemCounts.getOrDefault(gameJ, 1);
                int coCount = coEntry.getValue();

                double similarity = coCount / Math.sqrt((double) countI * countJ);
                if (similarity > 0.001) {
                    similarities.put(gameJ, similarity);
                }
            }
            itemSimilarityCache.put(gameI, similarities);
        }

        built = true;

        long elapsed = System.currentTimeMillis() - startTime;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gameCount", itemCounts.size());
        result.put("similarityPairs", itemSimilarityCache.values().stream().mapToLong(Map::size).sum());
        result.put("elapsedMs", elapsed);

        log.info("Item-CF相似度矩阵构建完成: {} 个游戏, {} 对相似关系, 耗时{}ms",
                itemCounts.size(), result.get("similarityPairs"), elapsed);

        return result;
    }

    /**
     * Item-CF推荐
     *
     * @param userId 目标用户ID
     * @param topN   推荐结果数
     * @return 推荐游戏列表
     */
    public List<Map<String, Object>> recommend(Long userId, int topN) {
        long startTime = System.currentTimeMillis();

        if (!built) {
            buildItemSimilarity();
        }

        Map<Long, Integer> userRatings = matrixService.getUserRatings(userId);
        if (userRatings.isEmpty()) {
            log.warn("用户{}无评分记录，无法进行Item-CF推荐", userId);
            return Collections.emptyList();
        }

        // 获取用户的高分游戏（≥4分）
        Set<Long> playedGames = userRatings.keySet();
        List<Long> highRatedGames = userRatings.entrySet().stream()
                .filter(e -> e.getValue() >= 4)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 收集候选游戏
        Map<Long, Double> candidateScores = new HashMap<>();

        for (Long gameId : highRatedGames) {
            Map<Long, Double> similarGames = itemSimilarityCache.get(gameId);
            if (similarGames == null) continue;

            int rating = userRatings.get(gameId);

            for (Map.Entry<Long, Double> sim : similarGames.entrySet()) {
                Long similarGameId = sim.getKey();
                double similarity = sim.getValue();

                // 排除已玩过的游戏
                if (!playedGames.contains(similarGameId)) {
                    candidateScores.merge(similarGameId, rating * similarity, Double::sum);
                }
            }
        }

        // 按得分降序，取Top-N
        List<Map<String, Object>> results = candidateScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("gameId", entry.getKey());
                    item.put("score", Math.round(entry.getValue() * 10000.0) / 10000.0);

                    Game game = gameMapper.selectOne(new QueryWrapper<Game>().eq("game_id", entry.getKey()));
                    if (game != null) {
                        item.put("gameName", game.getGameName());
                        item.put("gameNameCn", game.getGameNameCn());
                        item.put("genres", game.getGenres());
                    }
                    return item;
                })
                .collect(Collectors.toList());

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Item-CF推荐完成: userId={}, topN={}, 结果{}条, 耗时{}ms",
                userId, topN, results.size(), elapsed);

        return results;
    }

    public boolean isBuilt() {
        return built;
    }
}
