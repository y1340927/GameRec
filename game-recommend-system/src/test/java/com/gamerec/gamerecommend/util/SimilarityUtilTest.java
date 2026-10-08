package com.gamerec.gamerecommend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SimilarityUtil 单元测试
 *
 * 覆盖余弦相似度(Double/Int/String)、皮尔逊相关系数、Top-K选择
 */
@DisplayName("SimilarityUtil 单元测试")
class SimilarityUtilTest {

    // ==================== 余弦相似度 (Double) ====================

    @Nested
    @DisplayName("余弦相似度 — Double版本")
    class CosineSimilarityDouble {

        @Test
        @DisplayName("完全相同向量应返回1.0")
        void identicalVectorsShouldReturnOne() {
            Map<Long, Double> v = new HashMap<>();
            v.put(1L, 3.0);
            v.put(2L, 4.0);
            assertEquals(1.0, SimilarityUtil.cosineSimilarity(v, v), 0.0001);
        }

        @Test
        @DisplayName("正交向量应返回0")
        void orthogonalVectorsShouldReturnZero() {
            Map<Long, Double> v1 = new HashMap<>();
            v1.put(1L, 1.0);
            Map<Long, Double> v2 = new HashMap<>();
            v2.put(2L, 1.0);
            assertEquals(0.0, SimilarityUtil.cosineSimilarity(v1, v2), 0.0001);
        }

        @Test
        @DisplayName("部分重叠向量应计算正确")
        void partiallyOverlappingVectors() {
            Map<Long, Double> v1 = new HashMap<>();
            v1.put(1L, 1.0);
            v1.put(2L, 2.0);
            v1.put(3L, 3.0);
            // |v1| = sqrt(1+4+9) = sqrt(14) ≈ 3.742

            Map<Long, Double> v2 = new HashMap<>();
            v2.put(1L, 1.0);
            v2.put(2L, 2.0);
            v2.put(4L, 4.0);
            // |v2| = sqrt(1+4+16) = sqrt(21) ≈ 4.583

            // dot = 1*1 + 2*2 = 5
            // cos = 5 / (3.742 * 4.583) ≈ 0.292

            double sim = SimilarityUtil.cosineSimilarity(v1, v2);
            assertEquals(0.2917, sim, 0.001);
        }

        @Test
        @DisplayName("空向量应返回0")
        void emptyVectorsShouldReturnZero() {
            Map<Long, Double> v1 = new HashMap<>();
            Map<Long, Double> v2 = new HashMap<>();
            assertEquals(0.0, SimilarityUtil.cosineSimilarity(v1, v2));

            v2.put(1L, 1.0);
            assertEquals(0.0, SimilarityUtil.cosineSimilarity(v1, v2));
        }

        @Test
        @DisplayName("null输入应返回0")
        void nullInputsShouldReturnZero() {
            assertEquals(0.0, SimilarityUtil.cosineSimilarity(null, new HashMap<>()));
            assertEquals(0.0, SimilarityUtil.cosineSimilarity(new HashMap<>(), null));
            assertEquals(0.0, SimilarityUtil.cosineSimilarity(null, null));
        }

        @Test
        @DisplayName("全零向量应返回0（除零保护）")
        void allZeroVectorShouldReturnZero() {
            Map<Long, Double> v1 = new HashMap<>();
            v1.put(1L, 0.0);
            Map<Long, Double> v2 = new HashMap<>();
            v2.put(1L, 0.0);
            assertEquals(0.0, SimilarityUtil.cosineSimilarity(v1, v2));
        }

        @Test
        @DisplayName("大向量优化：遍历较小的向量")
        void shouldTraverseSmallerVector() {
            Map<Long, Double> small = new HashMap<>();
            small.put(1L, 1.0);
            Map<Long, Double> large = new HashMap<>();
            large.put(1L, 1.0);
            for (long i = 10; i < 100; i++) large.put(i, 1.0);

            double sim = SimilarityUtil.cosineSimilarity(small, large);
            assertTrue(sim > 0);
            assertTrue(sim < 1);
        }
    }

    // ==================== 余弦相似度 (Int) ====================

    @Nested
    @DisplayName("余弦相似度 — Int版本")
    class CosineSimilarityInt {

        @Test
        @DisplayName("应与Double版本结果一致")
        void shouldMatchDoubleVersion() {
            Map<Long, Integer> vi1 = new HashMap<>();
            vi1.put(1L, 3);
            vi1.put(2L, 4);
            Map<Long, Integer> vi2 = new HashMap<>();
            vi2.put(1L, 3);
            vi2.put(2L, 4);

            double sim = SimilarityUtil.cosineSimilarityInt(vi1, vi2);
            assertEquals(1.0, sim, 0.0001);
        }
    }

    // ==================== 余弦相似度 (String) ====================

    @Nested
    @DisplayName("余弦相似度 — String版本")
    class CosineSimilarityStr {

        @Test
        @DisplayName("相同关键词应返回1.0")
        void sameKeywordsShouldReturnOne() {
            Map<String, Double> v1 = new HashMap<>();
            v1.put("action", 1.0);
            v1.put("rpg", 1.0);
            assertEquals(1.0, SimilarityUtil.cosineSimilarityStr(v1, v1), 0.0001);
        }

        @Test
        @DisplayName("无重叠关键词应返回0")
        void noOverlapShouldReturnZero() {
            Map<String, Double> v1 = new HashMap<>();
            v1.put("action", 1.0);
            Map<String, Double> v2 = new HashMap<>();
            v2.put("puzzle", 1.0);
            assertEquals(0.0, SimilarityUtil.cosineSimilarityStr(v1, v2), 0.0001);
        }

        @Test
        @DisplayName("部分重叠应计算正确")
        void partialOverlapShouldBeBetweenZeroAndOne() {
            Map<String, Double> v1 = new HashMap<>();
            v1.put("action", 1.0);
            v1.put("rpg", 1.0);
            v1.put("strategy", 1.0);
            // |v1| = sqrt(3)

            Map<String, Double> v2 = new HashMap<>();
            v2.put("rpg", 1.0);
            v2.put("strategy", 1.0);
            v2.put("sports", 1.0);
            // |v2| = sqrt(3), dot = 2, cos = 2/3

            double sim = SimilarityUtil.cosineSimilarityStr(v1, v2);
            assertEquals(2.0 / 3.0, sim, 0.001);
        }
    }

    // ==================== 皮尔逊相关系数 ====================

    @Nested
    @DisplayName("皮尔逊相关系数")
    class PearsonCorrelation {

        @Test
        @DisplayName("完全正相关应返回1.0")
        void perfectPositiveCorrelation() {
            Map<Long, Double> v1 = new HashMap<>();
            v1.put(1L, 1.0);
            v1.put(2L, 2.0);
            v1.put(3L, 3.0);
            Map<Long, Double> v2 = new HashMap<>();
            v2.put(1L, 2.0);
            v2.put(2L, 4.0);
            v2.put(3L, 6.0);
            assertEquals(1.0, SimilarityUtil.pearsonCorrelation(v1, v2), 0.0001);
        }

        @Test
        @DisplayName("少于2个共同元素应返回0")
        void lessThanTwoCommonReturnsZero() {
            Map<Long, Double> v1 = new HashMap<>();
            v1.put(1L, 1.0);
            Map<Long, Double> v2 = new HashMap<>();
            v2.put(1L, 1.0);
            assertEquals(0.0, SimilarityUtil.pearsonCorrelation(v1, v2));
        }

        @Test
        @DisplayName("空向量应返回0")
        void emptyVectorsReturnZero() {
            assertEquals(0.0, SimilarityUtil.pearsonCorrelation(new HashMap<>(), new HashMap<>()));
        }
    }

    // ==================== Top-K 选择 ====================

    @Nested
    @DisplayName("Top-K相似项选择")
    class TopKSimilar {

        @Test
        @DisplayName("应返回按相似度降序的Top-K项")
        void shouldReturnTopKBySimilarityDescending() {
            Map<Long, Double> sims = new HashMap<>();
            sims.put(2L, 0.9);
            sims.put(3L, 0.3);
            sims.put(4L, 0.7);
            sims.put(5L, 0.5);

            List<Map.Entry<Long, Double>> top2 = SimilarityUtil.getTopKSimilar(1L, sims, 2);

            assertEquals(2, top2.size());
            assertEquals(2L, top2.get(0).getKey());
            assertEquals(0.9, top2.get(0).getValue(), 0.001);
            assertEquals(4L, top2.get(1).getKey());
            assertEquals(0.7, top2.get(1).getValue(), 0.001);
        }

        @Test
        @DisplayName("应排除目标项自身")
        void shouldExcludeTargetItem() {
            Map<Long, Double> sims = new HashMap<>();
            sims.put(1L, 1.0); // self
            sims.put(2L, 0.5);

            List<Map.Entry<Long, Double>> result = SimilarityUtil.getTopKSimilar(1L, sims, 10);

            assertEquals(1, result.size());
            assertEquals(2L, result.get(0).getKey());
        }

        @Test
        @DisplayName("应排除相似度≤0的项")
        void shouldExcludeNonPositiveSimilarity() {
            Map<Long, Double> sims = new HashMap<>();
            sims.put(2L, 0.0);
            sims.put(3L, -0.1);

            List<Map.Entry<Long, Double>> result = SimilarityUtil.getTopKSimilar(1L, sims, 10);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("K超过总数时应返回全部有效结果")
        void shouldCapAtAvailableCount() {
            Map<Long, Double> sims = new HashMap<>();
            sims.put(2L, 0.8);
            sims.put(3L, 0.6);

            List<Map.Entry<Long, Double>> result = SimilarityUtil.getTopKSimilar(1L, sims, 100);

            assertEquals(2, result.size());
        }
    }
}
