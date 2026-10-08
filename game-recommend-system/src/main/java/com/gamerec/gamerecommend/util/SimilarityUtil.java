package com.gamerec.gamerecommend.util;

import java.util.*;

/**
 * 相似度计算工具类
 *
 * 提供推荐算法所需的相似度计算方法：
 *   1. 余弦相似度 —— User-CF、Content-Based
 *   2. 皮尔逊相关系数 —— 备选相似度
 *
 * @author GameRec Team
 */
public class SimilarityUtil {

    /**
     * 余弦相似度
     *
     * 公式：cos(A,B) = (A·B) / (||A|| × ||B||)
     * 范围：[-1, 1]，值越大越相似
     *
     * 优化策略：遍历较小的向量，减少循环次数
     *
     * @param v1 向量1（Map<Long, Double>，key为维度ID，value为权重）
     * @param v2 向量2
     * @return 余弦相似度 [0, 1]，0表示完全不同
     */
    public static double cosineSimilarity(Map<Long, Double> v1, Map<Long, Double> v2) {
        if (v1 == null || v2 == null || v1.isEmpty() || v2.isEmpty()) {
            return 0.0;
        }

        // 遍历较小的向量以优化性能
        Map<Long, Double> smaller = v1.size() <= v2.size() ? v1 : v2;
        Map<Long, Double> larger = v1.size() <= v2.size() ? v2 : v1;

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        // 计算点积（只累加两向量共有的key）
        for (Map.Entry<Long, Double> entry : smaller.entrySet()) {
            Long key = entry.getKey();
            Double val1 = entry.getValue();
            Double val2 = larger.get(key);

            if (val2 != null) {
                dotProduct += val1 * val2;
            }
            norm1 += val1 * val1;
        }

        // 计算模长
        for (Double val : larger.values()) {
            norm2 += val * val;
        }

        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        if (denominator == 0) {
            return 0.0;
        }

        return dotProduct / denominator;
    }

    /**
     * 余弦相似度（int类型评分版本）
     *
     * @param v1 评分向量1
     * @param v2 评分向量2
     * @return 余弦相似度 [0, 1]
     */
    public static double cosineSimilarityInt(Map<Long, Integer> v1, Map<Long, Integer> v2) {
        if (v1 == null || v2 == null || v1.isEmpty() || v2.isEmpty()) {
            return 0.0;
        }

        Map<Long, Double> dv1 = new HashMap<>();
        Map<Long, Double> dv2 = new HashMap<>();
        for (Map.Entry<Long, Integer> e : v1.entrySet()) dv1.put(e.getKey(), e.getValue().doubleValue());
        for (Map.Entry<Long, Integer> e : v2.entrySet()) dv2.put(e.getKey(), e.getValue().doubleValue());

        return cosineSimilarity(dv1, dv2);
    }

    /**
     * 皮尔逊相关系数（备选）
     *
     * 公式：r = Σ[(xi-x̄)(yi-ȳ)] / √(Σ(xi-x̄)² · Σ(yi-ȳ)²)
     *
     * @param v1 向量1
     * @param v2 向量2
     * @return 相关系数 [-1, 1]
     */
    public static double pearsonCorrelation(Map<Long, Double> v1, Map<Long, Double> v2) {
        if (v1 == null || v2 == null || v1.isEmpty() || v2.isEmpty()) {
            return 0.0;
        }

        // 找出共同元素
        List<Long> commonKeys = new ArrayList<>();
        for (Long key : v1.keySet()) {
            if (v2.containsKey(key)) {
                commonKeys.add(key);
            }
        }

        int n = commonKeys.size();
        if (n < 2) {
            return 0.0;
        }

        // 计算均值
        double sum1 = 0, sum2 = 0;
        for (Long key : commonKeys) {
            sum1 += v1.get(key);
            sum2 += v2.get(key);
        }
        double mean1 = sum1 / n;
        double mean2 = sum2 / n;

        // 计算相关系数
        double numerator = 0, denom1 = 0, denom2 = 0;
        for (Long key : commonKeys) {
            double diff1 = v1.get(key) - mean1;
            double diff2 = v2.get(key) - mean2;
            numerator += diff1 * diff2;
            denom1 += diff1 * diff1;
            denom2 += diff2 * diff2;
        }

        double denominator = Math.sqrt(denom1) * Math.sqrt(denom2);
        if (denominator == 0) {
            return 0.0;
        }

        return numerator / denominator;
    }

    /**
     * 余弦相似度（String类型key版本，用于Content-Based）
     *
     * @param v1 向量1（Map<String, Double>）
     * @param v2 向量2（Map<String, Double>）
     * @return 余弦相似度 [0, 1]
     */
    public static double cosineSimilarityStr(Map<String, Double> v1, Map<String, Double> v2) {
        if (v1 == null || v2 == null || v1.isEmpty() || v2.isEmpty()) {
            return 0.0;
        }

        Map<String, Double> smaller = v1.size() <= v2.size() ? v1 : v2;
        Map<String, Double> larger = v1.size() <= v2.size() ? v2 : v1;

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (Map.Entry<String, Double> entry : smaller.entrySet()) {
            String key = entry.getKey();
            Double val1 = entry.getValue();
            Double val2 = larger.get(key);

            if (val2 != null) {
                dotProduct += val1 * val2;
            }
            norm1 += val1 * val1;
        }

        for (Double val : larger.values()) {
            norm2 += val * val;
        }

        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        if (denominator == 0) {
            return 0.0;
        }

        return dotProduct / denominator;
    }

    /**
     * 获取Top-K个最相似项
     *
     * @param targetId    目标ID
     * @param similarities Map<ID, 相似度>
     * @param topK        Top-K值
     * @return 按相似度降序排列的Top-K项
     */
    public static List<Map.Entry<Long, Double>> getTopKSimilar(
            Long targetId, Map<Long, Double> similarities, int topK) {
        return similarities.entrySet().stream()
                .filter(e -> !e.getKey().equals(targetId))
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .collect(java.util.stream.Collectors.toList());
    }
}
