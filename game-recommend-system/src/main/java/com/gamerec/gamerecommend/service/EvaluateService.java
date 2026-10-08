package com.gamerec.gamerecommend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gamerec.gamerecommend.entity.EvaluationResult;
import com.gamerec.gamerecommend.entity.RatingTest;
import com.gamerec.gamerecommend.mapper.EvaluationResultMapper;
import com.gamerec.gamerecommend.mapper.RatingTestMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 推荐算法评估服务
 *
 * 离线评估指标：
 *   - Precision@K：推荐K个游戏中，用户实际喜欢的比例
 *   - Recall@K：用户实际喜欢的游戏中，被推荐的比例
 *   - F1@K：Precision和Recall的调和平均
 *   - NDCG@K：归一化折损累计增益
 *
 * @author GameRec Team
 */
@Slf4j
@Service
public class EvaluateService {

    /**
     * 测试集 Mapper
     *
     * 【问题 2.2】评估阶段"用户实际喜欢的游戏集合"的唯一数据源。
     * 与训练侧使用的 rating_train 物理隔离，杜绝模型提前见过测试答案导致指标虚高。
     */
    @Autowired
    private RatingTestMapper ratingTestMapper;

    @Autowired
    private EvaluationResultMapper evaluationResultMapper;

    @Autowired
    private UserCFService userCFService;

    @Autowired
    private ItemCFService itemCFService;

    @Autowired
    private ContentBasedService contentBasedService;

    @Autowired
    private PopularityService popularityService;

    @Autowired
    private SVDService svdService;

    @Autowired
    private HybridService hybridService;

    /**
     * 评估单个算法
     *
     * @param algorithmName 算法名称
     * @param recommender   推荐函数（userId, topK → 推荐列表）
     * @param topK          Top-K值
     * @return 评估结果
     */
    private Map<String, Object> evaluateAlgorithm(String algorithmName,
                                                   Function<Long, List<Map<String, Object>>> recommender,
                                                   int topK) {
        // 【问题 2.2】候选用户只从测试集中选取，且要求该用户在测试集中存在
        // rating>=4 的记录（即"真实喜欢集合"非空）。
        // 旧实现从全量 rating 取用户，导致大量采样用户在测试集中没有任何高分行，
        // 被 `if (likedGames.isEmpty()) continue;` 跳过，validUsers 只剩个位数。
        QueryWrapper<RatingTest> wrapper = new QueryWrapper<>();
        wrapper.eq("is_valid", 1)
                .ge("rating", 4)
                .select("DISTINCT user_id");
        List<Long> userIds = ratingTestMapper.selectObjs(wrapper).stream()
                .map(obj -> (Long) obj)
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            log.warn("测试集中无有效评估用户（rating>=4），请检查数据划分");
        }

        // 固定随机种子采样，保证评估结果可复现（seed=42）
        int sampleSize = Math.min(500, userIds.size());
        Collections.shuffle(userIds, new Random(42));
        List<Long> sampledUsers = userIds.subList(0, sampleSize);

        double totalPrecision = 0;
        double totalRecall = 0;
        double totalNDCG = 0;
        int validUsers = 0;

        for (Long userId : sampledUsers) {
            try {
                // 【问题 2.2】真实喜欢集合只读测试集，训练阶段不可见
                QueryWrapper<RatingTest> userWrapper = new QueryWrapper<>();
                userWrapper.eq("user_id", userId).eq("is_valid", 1).ge("rating", 4);
                Set<Long> likedGames = ratingTestMapper.selectList(userWrapper).stream()
                        .map(RatingTest::getGameId)
                        .collect(Collectors.toSet());

                if (likedGames.isEmpty()) continue;

                // 获取推荐列表
                List<Map<String, Object>> recommendations = recommender.apply(userId);
                if (recommendations.isEmpty()) continue;

                Set<Long> recommendedGames = recommendations.stream()
                        .limit(topK)
                        .map(m -> (Long) m.get("gameId"))
                        .collect(Collectors.toSet());

                // 命中数
                long hits = recommendedGames.stream().filter(likedGames::contains).count();

                // Precision@K
                double precision = (double) hits / topK;
                totalPrecision += precision;

                // Recall@K
                double recall = (double) hits / likedGames.size();
                totalRecall += recall;

                // NDCG@K
                double dcg = 0;
                double idcg = 0;
                int rank = 1;
                for (Map<String, Object> rec : recommendations) {
                    if (rank > topK) break;
                    Long gameId = (Long) rec.get("gameId");
                    if (likedGames.contains(gameId)) {
                        dcg += 1.0 / (Math.log(rank + 1) / Math.log(2));
                    }
                    if (rank <= likedGames.size()) {
                        idcg += 1.0 / (Math.log(rank + 1) / Math.log(2));
                    }
                    rank++;
                }
                double ndcg = idcg > 0 ? dcg / idcg : 0;
                totalNDCG += ndcg;

                validUsers++;
            } catch (Exception e) {
                log.debug("评估用户{}失败: {}", userId, e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("algorithmName", algorithmName);
        result.put("topK", topK);
        result.put("precision", validUsers > 0 ? totalPrecision / validUsers : 0);
        result.put("recall", validUsers > 0 ? totalRecall / validUsers : 0);
        result.put("f1", validUsers > 0 ?
                computeF1(totalPrecision / validUsers, totalRecall / validUsers) : 0);
        result.put("ndcg", validUsers > 0 ? totalNDCG / validUsers : 0);
        result.put("validUsers", validUsers);

        return result;
    }

    /**
     * 评估所有算法
     *
     * @param topK Top-K值
     * @return 所有算法的评估结果
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> evaluateAll(int topK) {
        log.info("开始评估所有算法, topK={}", topK);
        long startTime = System.currentTimeMillis();

        List<Map<String, Object>> results = new ArrayList<>();

        // User-CF
        Map<String, Object> userCF = evaluateAlgorithm("UserCF",
                uid -> userCFService.recommend(uid, 10, topK * 2), topK);
        results.add(userCF);

        // Item-CF
        Map<String, Object> itemCF = evaluateAlgorithm("ItemCF",
                uid -> itemCFService.recommend(uid, topK * 2), topK);
        results.add(itemCF);

        // Content-Based
        Map<String, Object> cb = evaluateAlgorithm("CB",
                uid -> contentBasedService.recommend(uid, topK * 2), topK);
        results.add(cb);

        // SVD（评估前确保模型已训练：优先从磁盘加载，否则现场训练，保证评估自包含可复现）
        if (!svdService.isTrained()) {
            log.info("SVD 模型未训练，评估前自动训练（factors=50, iterations=20）");
            svdService.train(50, 20, 0.005, 0.02);
        }
        Map<String, Object> svd = evaluateAlgorithm("SVD",
                uid -> svdService.recommend(uid, topK * 2), topK);
        results.add(svd);

        // Popularity
        Map<String, Object> pop = evaluateAlgorithm("Popularity",
                uid -> popularityService.recommend(topK * 2), topK);
        results.add(pop);

        // Hybrid
        Map<String, Object> hybrid = evaluateAlgorithm("Hybrid",
                uid -> hybridService.hybridRecommend(uid, topK * 2), topK);
        results.add(hybrid);

        // 持久化评估结果
        for (Map<String, Object> r : results) {
            EvaluationResult entity = new EvaluationResult();
            entity.setAlgorithmName((String) r.get("algorithmName"));
            entity.setTopK(topK);
            entity.setPrecisionVal((Double) r.get("precision"));
            entity.setRecallVal((Double) r.get("recall"));
            entity.setF1Val((Double) r.get("f1"));
            entity.setNdcgVal((Double) r.get("ndcg"));
            entity.setValidUsers((Integer) r.get("validUsers"));
            evaluationResultMapper.insert(entity);
        }

        // 四舍五入结果
        for (Map<String, Object> r : results) {
            r.put("precision", Math.round((Double) r.get("precision") * 10000.0) / 10000.0);
            r.put("recall", Math.round((Double) r.get("recall") * 10000.0) / 10000.0);
            r.put("f1", Math.round((Double) r.get("f1") * 10000.0) / 10000.0);
            r.put("ndcg", Math.round((Double) r.get("ndcg") * 10000.0) / 10000.0);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("评估完成: 6种算法, topK={}, 耗时{}ms", topK, elapsed);

        return results;
    }

    /**
     * 批量评估多个K值
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> evaluateMultipleK(int[] topKs) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> allResults = new ArrayList<>();

        for (int k : topKs) {
            List<Map<String, Object>> kResults = evaluateAll(k);
            for (Map<String, Object> r : kResults) {
                r.put("topK", k);
            }
            allResults.addAll(kResults);
        }

        result.put("results", allResults);
        result.put("topKs", topKs);
        return result;
    }

    /**
     * 获取历史评估结果
     */
    public List<EvaluationResult> getHistoryResults() {
        QueryWrapper<EvaluationResult> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        return evaluationResultMapper.selectList(wrapper);
    }

    /**
     * 获取指定算法的评估结果
     */
    public List<EvaluationResult> getResultsByAlgorithm(String algorithmName) {
        QueryWrapper<EvaluationResult> wrapper = new QueryWrapper<>();
        wrapper.eq("algorithm_name", algorithmName).orderByDesc("create_time");
        return evaluationResultMapper.selectList(wrapper);
    }

    /**
     * 计算F1值
     */
    private double computeF1(double precision, double recall) {
        if (precision + recall == 0) return 0;
        return 2 * precision * recall / (precision + recall);
    }
}
