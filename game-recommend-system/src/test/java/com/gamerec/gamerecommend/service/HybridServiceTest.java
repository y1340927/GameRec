package com.gamerec.gamerecommend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * HybridService 单元测试
 *
 * 验证混合推荐权重融合、排序逻辑、异常降级策略
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HybridService 单元测试")
class HybridServiceTest {

    @Mock
    private UserCFService userCFService;

    @Mock
    private ItemCFService itemCFService;

    @Mock
    private ContentBasedService contentBasedService;

    @Mock
    private SVDService svdService;

    @Mock
    private PopularityService popularityService;

    @InjectMocks
    private HybridService hybridService;

    private static List<Map<String, Object>> makeResult(Long... gameIds) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Long gameId : gameIds) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("gameId", gameId);
            item.put("gameName", "Game_" + gameId);
            results.add(item);
        }
        return results;
    }

    @Nested
    @DisplayName("默认权重融合")
    class DefaultWeightBlending {

        @BeforeEach
        void setup() {
            // 每个算法返回不同的游戏，用于验证融合排序
            when(userCFService.recommend(anyLong(), anyInt(), anyInt()))
                .thenReturn(makeResult(101L, 102L, 103L));
            when(itemCFService.recommend(anyLong(), anyInt()))
                .thenReturn(makeResult(201L, 202L));
            when(contentBasedService.recommend(anyLong(), anyInt()))
                .thenReturn(makeResult(301L));
            when(svdService.recommend(anyLong(), anyInt()))
                .thenReturn(makeResult(401L, 402L, 403L, 404L));
            when(popularityService.recommend(anyInt()))
                .thenReturn(makeResult(501L, 502L));
        }

        @Test
        @DisplayName("应融合5种算法结果并返回Top-N")
        void shouldBlendAllFiveAlgorithms() {
            List<Map<String, Object>> results = hybridService.hybridRecommend(1L, 5);

            assertNotNull(results);
            assertFalse(results.isEmpty());
            assertTrue(results.size() <= 5, "Should not exceed topN");
        }

        @Test
        @DisplayName("结果应按hybridScore降序排列")
        void resultsShouldBeSortedByHybridScoreDescending() {
            List<Map<String, Object>> results = hybridService.hybridRecommend(1L, 20);

            for (int i = 0; i < results.size() - 1; i++) {
                double current = (Double) results.get(i).get("hybridScore");
                double next = (Double) results.get(i + 1).get("hybridScore");
                assertTrue(current >= next,
                    "Score at " + i + " should be >= score at " + (i + 1) +
                    " (" + current + " vs " + next + ")");
            }
        }

        @Test
        @DisplayName("每个结果应包含hybridScore和gameName")
        void eachResultShouldHaveRequiredFields() {
            List<Map<String, Object>> results = hybridService.hybridRecommend(1L, 3);

            for (Map<String, Object> r : results) {
                assertNotNull(r.get("gameId"));
                assertNotNull(r.get("hybridScore"));
                assertTrue((Double) r.get("hybridScore") > 0);
            }
        }
    }

    @Nested
    @DisplayName("自定义权重")
    class CustomWeights {

        @Test
        @DisplayName("自定义权重数组应被正确应用")
        void shouldUseCustomWeights() {
            when(userCFService.recommend(anyLong(), anyInt(), anyInt()))
                .thenReturn(makeResult(101L));
            when(itemCFService.recommend(anyLong(), anyInt()))
                .thenReturn(makeResult(201L));
            when(contentBasedService.recommend(anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());
            when(svdService.recommend(anyLong(), anyInt()))
                .thenReturn(makeResult(401L));
            when(popularityService.recommend(anyInt()))
                .thenReturn(Collections.emptyList());

            // 全部分给SVD，应只有SVD的结果
            double[] svdOnly = {0.0, 0.0, 0.0, 1.0, 0.0};
            List<Map<String, Object>> results = hybridService.hybridRecommend(1L, 10, svdOnly);

            assertFalse(results.isEmpty());
            assertEquals(401L, results.get(0).get("gameId"));
        }

        @Test
        @DisplayName("null权重应使用默认权重")
        void nullWeightsShouldFallbackToDefault() {
            when(userCFService.recommend(anyLong(), anyInt(), anyInt()))
                .thenReturn(makeResult(101L));
            when(itemCFService.recommend(anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());
            when(contentBasedService.recommend(anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());
            when(svdService.recommend(anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());
            when(popularityService.recommend(anyInt()))
                .thenReturn(Collections.emptyList());

            List<Map<String, Object>> results = hybridService.hybridRecommend(1L, 5, null);

            assertNotNull(results);
            assertFalse(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("降级策略 — 某算法异常不影响全局")
    class FaultTolerance {

        @Test
        @DisplayName("UserCF异常时应继续融合其他算法")
        void userCFExceptionShouldNotBreakBlending() {
            when(userCFService.recommend(anyLong(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("UserCF failed"));
            when(itemCFService.recommend(anyLong(), anyInt()))
                .thenReturn(makeResult(201L, 202L));
            when(contentBasedService.recommend(anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());
            when(svdService.recommend(anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());
            when(popularityService.recommend(anyInt()))
                .thenReturn(Collections.emptyList());

            List<Map<String, Object>> results = hybridService.hybridRecommend(1L, 5);

            assertNotNull(results);
            assertFalse(results.isEmpty());
            assertEquals(201L, results.get(0).get("gameId"));
        }

        @Test
        @DisplayName("全部算法异常时应返回空列表")
        void allAlgorithmsExceptionShouldReturnEmpty() {
            when(userCFService.recommend(anyLong(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("All failed"));
            when(itemCFService.recommend(anyLong(), anyInt()))
                .thenThrow(new RuntimeException("All failed"));
            when(contentBasedService.recommend(anyLong(), anyInt()))
                .thenThrow(new RuntimeException("All failed"));
            when(svdService.recommend(anyLong(), anyInt()))
                .thenThrow(new RuntimeException("All failed"));
            when(popularityService.recommend(anyInt()))
                .thenThrow(new RuntimeException("All failed"));

            List<Map<String, Object>> results = hybridService.hybridRecommend(1L, 5);

            assertNotNull(results);
            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("排名得分公式验证")
    class RankScoreFormula {

        @Test
        @DisplayName("排名越靠前得分越高 (1/rank)")
        void higherRankShouldGetHigherScore() {
            when(userCFService.recommend(anyLong(), anyInt(), anyInt()))
                .thenReturn(makeResult(101L)); // rank 1: score = weight*1.0
            when(itemCFService.recommend(anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());
            when(contentBasedService.recommend(anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());
            when(svdService.recommend(anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());
            when(popularityService.recommend(anyInt()))
                .thenReturn(makeResult(501L, 502L)); // rank 1,2: weights*1.0, weights*0.5

            List<Map<String, Object>> results = hybridService.hybridRecommend(1L, 10);

            // 101L has rank 1 from UserCF (weight=0.25): score = 0.25*1.0 = 0.25
            // 501L has rank 1 from Popularity (weight=0.05): score = 0.05*1.0 = 0.05
            // 502L has rank 2 from Popularity (weight=0.05): score = 0.05*0.5 = 0.025
            // 101 should be first

            assertEquals(101L, results.get(0).get("gameId"));
        }
    }
}
