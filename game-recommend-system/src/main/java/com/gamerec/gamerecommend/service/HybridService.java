package com.gamerec.gamerecommend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Supplier;

/**
 * 混合推荐服务
 *
 * 加权融合五种推荐算法的结果：
 *
 * 权重分配：
 *   SVD: 0.30（精度最高）
 *   User-CF: 0.25
 *   Item-CF: 0.25
 *   Content-Based: 0.15
 *   Popularity: 0.05（兜底）
 *
 * 混合公式：hybridScore = Σ(weight_i × 1/rank_i)
 *
 * @author GameRec Team
 */
@Slf4j
@Service
public class HybridService {

    @Autowired
    private UserCFService userCFService;

    @Autowired
    private ItemCFService itemCFService;

    @Autowired
    private ContentBasedService contentBasedService;

    @Autowired
    private SVDService svdService;

    @Autowired
    private PopularityService popularityService;

    /** 默认权重 */
    private static final double[] DEFAULT_WEIGHTS = {0.25, 0.25, 0.15, 0.30, 0.05};

    /**
     * 安全推荐调用（捕获异常，不影响整体）
     *
     * @param supplier 推荐函数
     * @return 推荐结果（异常时返回空列表）
     */
    private List<Map<String, Object>> safeRecommend(Supplier<List<Map<String, Object>>> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("推荐算法执行异常: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 将排名转化为得分（1/rank）
     *
     * @param recommendations 推荐列表
     * @param weight          权重
     * @param scores          得分Map（累加）
     */
    private void addScores(List<Map<String, Object>> recommendations, double weight,
                           Map<Long, Map<String, Object>> allItems,
                           Map<Long, Double> scores) {
        for (int i = 0; i < recommendations.size(); i++) {
            Long gameId = (Long) recommendations.get(i).get("gameId");
            double rankScore = 1.0 / (i + 1); // 1/rank

            scores.merge(gameId, weight * rankScore, Double::sum);

            // 保存游戏信息
            if (!allItems.containsKey(gameId)) {
                allItems.put(gameId, recommendations.get(i));
            }
        }
    }

    /**
     * 混合推荐
     *
     * @param userId  目标用户ID
     * @param topN    推荐结果数
     * @param weights 权重数组 [User-CF, Item-CF, CB, SVD, Popularity]
     * @return 推荐游戏列表
     */
    public List<Map<String, Object>> hybridRecommend(Long userId, int topN, double[] weights) {
        long startTime = System.currentTimeMillis();

        if (weights == null) {
            weights = DEFAULT_WEIGHTS;
        }

        int fetchSize = Math.max(topN * 2, 30); // 各算法取足够多的候选

        // 并行获取各算法推荐结果（实际串行，因为依赖先后顺序）
        List<Map<String, Object>> userCFResults = safeRecommend(() ->
                userCFService.recommend(userId, 10, fetchSize));
        List<Map<String, Object>> itemCFResults = safeRecommend(() ->
                itemCFService.recommend(userId, fetchSize));
        List<Map<String, Object>> cbResults = safeRecommend(() ->
                contentBasedService.recommend(userId, fetchSize));
        List<Map<String, Object>> svdResults = safeRecommend(() ->
                svdService.recommend(userId, fetchSize));
        List<Map<String, Object>> popResults = safeRecommend(() ->
                popularityService.recommend(fetchSize));

        // 加权融合
        Map<Long, Double> scores = new HashMap<>();
        Map<Long, Map<String, Object>> allItems = new HashMap<>();

        addScores(userCFResults, weights[0], allItems, scores);
        addScores(itemCFResults, weights[1], allItems, scores);
        addScores(cbResults, weights[2], allItems, scores);
        addScores(svdResults, weights[3], allItems, scores);
        addScores(popResults, weights[4], allItems, scores);

        // 按得分降序，取Top-N
        List<Map<String, Object>> results = scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("gameId", entry.getKey());
                    item.put("hybridScore", Math.round(entry.getValue() * 10000.0) / 10000.0);

                    Map<String, Object> gameInfo = allItems.get(entry.getKey());
                    if (gameInfo != null) {
                        item.put("gameName", gameInfo.get("gameName"));
                        item.put("gameNameCn", gameInfo.get("gameNameCn"));
                        item.put("genres", gameInfo.get("genres"));
                    }
                    return item;
                })
                .collect(java.util.stream.Collectors.toList());

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("混合推荐完成: userId={}, topN={}, 融合{}种算法, 结果{}条, 耗时{}ms",
                userId, topN, 5, results.size(), elapsed);

        return results;
    }

    /**
     * 使用默认权重的混合推荐
     */
    public List<Map<String, Object>> hybridRecommend(Long userId, int topN) {
        return hybridRecommend(userId, topN, DEFAULT_WEIGHTS);
    }
}
