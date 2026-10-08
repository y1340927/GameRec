package com.gamerec.gamerecommend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gamerec.gamerecommend.entity.Game;
import com.gamerec.gamerecommend.mapper.GameMapper;
import com.gamerec.gamerecommend.util.SimilarityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * User-CF 协同过滤推荐服务
 *
 * 算法流程：
 *   1. 计算目标用户与其他所有用户的余弦相似度
 *   2. 找出最相似的K个用户（K近邻）
 *   3. 从这K个用户的评分中，找出目标用户没玩过的高分游戏
 *   4. 按推荐得分排序，返回Top-N
 *
 * 公式：cos(A,B) = (A·B) / (||A|| × ||B||)
 *
 * @author GameRec Team
 */
@Slf4j
@Service
public class UserCFService {

    @Autowired
    private MatrixService matrixService;

    @Autowired
    private GameMapper gameMapper;

    /**
     * User-CF推荐
     *
     * @param userId 目标用户ID
     * @param topK   相似用户数K
     * @param topN   推荐结果数N
     * @return 推荐游戏列表
     */
    public List<Map<String, Object>> recommend(Long userId, int topK, int topN) {
        long startTime = System.currentTimeMillis();

        // 确保评分矩阵已构建
        Map<Long, Map<Long, Integer>> ratingMatrix = matrixService.getRatingMatrix();
        Map<Long, Integer> targetRatings = matrixService.getUserRatings(userId);

        if (targetRatings.isEmpty()) {
            log.warn("用户{}无评分记录，无法进行User-CF推荐", userId);
            return Collections.emptyList();
        }

        // 1. 计算目标用户与所有其他用户的相似度
        Map<Long, Double> similarities = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Integer>> entry : ratingMatrix.entrySet()) {
            Long otherUserId = entry.getKey();
            if (otherUserId.equals(userId)) continue;

            Map<Long, Integer> otherRatings = entry.getValue();
            double similarity = SimilarityUtil.cosineSimilarityInt(targetRatings, otherRatings);

            if (similarity > 0) {
                similarities.put(otherUserId, similarity);
            }
        }

        // 2. 取Top-K相似用户
        List<Map.Entry<Long, Double>> topKSimilar = similarities.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .collect(Collectors.toList());

        if (topKSimilar.isEmpty()) {
            log.warn("用户{}无相似用户", userId);
            return Collections.emptyList();
        }

        // 3. 收集候选游戏并计算推荐得分
        Set<Long> playedGames = targetRatings.keySet();
        Map<Long, Double> candidateScores = new HashMap<>();

        for (Map.Entry<Long, Double> simUser : topKSimilar) {
            Long simUserId = simUser.getKey();
            double similarity = simUser.getValue();
            Map<Long, Integer> simUserRatings = ratingMatrix.get(simUserId);

            for (Map.Entry<Long, Integer> rating : simUserRatings.entrySet()) {
                Long gameId = rating.getKey();
                int r = rating.getValue();

                // 排除已玩过的游戏，只考虑高分游戏（≥4分）
                if (!playedGames.contains(gameId) && r >= 4) {
                    candidateScores.merge(gameId, r * similarity, Double::sum);
                }
            }
        }

        // 4. 按得分降序，取Top-N
        List<Map<String, Object>> results = candidateScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("gameId", entry.getKey());
                    item.put("score", Math.round(entry.getValue() * 10000.0) / 10000.0);

                    // 填充游戏名称
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
        log.info("User-CF推荐完成: userId={}, topK={}, topN={}, 结果{}条, 耗时{}ms",
                userId, topK, topN, results.size(), elapsed);

        return results;
    }
}
