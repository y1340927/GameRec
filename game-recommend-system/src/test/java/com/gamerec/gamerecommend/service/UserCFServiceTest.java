package com.gamerec.gamerecommend.service;

import com.gamerec.gamerecommend.entity.Game;
import com.gamerec.gamerecommend.mapper.GameMapper;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * UserCFService 单元测试
 *
 * 验证协同过滤核心逻辑：相似用户筛选、候选游戏打分、Top-N推荐
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserCFService 单元测试")
class UserCFServiceTest {

    @Mock
    private MatrixService matrixService;

    @Mock
    private GameMapper gameMapper;

    @InjectMocks
    private UserCFService userCFService;

    // ==================== 空状态处理 ====================

    @Test
    @DisplayName("用户无评分记录应返回空列表")
    void shouldReturnEmptyWhenNoRatings() {
        when(matrixService.getUserRatings(anyLong())).thenReturn(Collections.emptyMap());

        List<Map<String, Object>> results = userCFService.recommend(1L, 10, 10);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("仅有一个用户时无相似用户应返回空列表")
    void shouldReturnEmptyWhenOnlyOneUser() {
        Map<Long, Integer> targetRatings = new HashMap<>();
        targetRatings.put(100L, 5);
        targetRatings.put(101L, 4);

        when(matrixService.getUserRatings(1L)).thenReturn(targetRatings);

        // 评分矩阵只包含目标用户自己
        Map<Long, Map<Long, Integer>> ratingMatrix = new HashMap<>();
        ratingMatrix.put(1L, targetRatings);
        when(matrixService.getRatingMatrix()).thenReturn(ratingMatrix);

        List<Map<String, Object>> results = userCFService.recommend(1L, 10, 10);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ==================== 正常推荐 ====================

    @Nested
    @DisplayName("正常推荐场景")
    class NormalRecommendation {

        @BeforeEach
        void setup() {
            // 目标用户：玩过游戏 100(5分), 101(4分)
            Map<Long, Integer> targetRatings = new LinkedHashMap<>();
            targetRatings.put(100L, 5);
            targetRatings.put(101L, 4);

            // 相似用户2：玩过 100(5), 102(5) — 与目标用户共享100(5)
            Map<Long, Integer> user2Ratings = new LinkedHashMap<>();
            user2Ratings.put(100L, 5);
            user2Ratings.put(102L, 5);

            // 用户3：玩过 103(3) — 与目标用户无交集
            Map<Long, Integer> user3Ratings = new LinkedHashMap<>();
            user3Ratings.put(103L, 3);

            // 用户4：玩过 101(4), 104(5) — 与目标用户共享101(4)
            Map<Long, Integer> user4Ratings = new LinkedHashMap<>();
            user4Ratings.put(101L, 4);
            user4Ratings.put(104L, 5);

            Map<Long, Map<Long, Integer>> matrix = new HashMap<>();
            matrix.put(1L, targetRatings);
            matrix.put(2L, user2Ratings);
            matrix.put(3L, user3Ratings);
            matrix.put(4L, user4Ratings);

            when(matrixService.getUserRatings(1L)).thenReturn(targetRatings);
            when(matrixService.getRatingMatrix()).thenReturn(matrix);

            // Mock gameMapper to return basic game info
            when(gameMapper.selectById(anyLong())).thenAnswer(inv -> {
                Long id = inv.getArgument(0);
                Game g = new Game();
                g.setGameId(id);
                g.setGameName("Game_" + id);
                return g;
            });
        }

        @Test
        @DisplayName("应排除已玩过游戏，只推荐新游戏")
        void shouldExcludePlayedGames() {
            List<Map<String, Object>> results = userCFService.recommend(1L, 5, 5);

            assertFalse(results.isEmpty());
            Set<Long> resultIds = new HashSet<>();
            for (Map<String, Object> r : results) {
                resultIds.add((Long) r.get("gameId"));
            }
            assertFalse(resultIds.contains(100L), "Should not contain game 100 (already played)");
            assertFalse(resultIds.contains(101L), "Should not contain game 101 (already played)");
        }

        @Test
        @DisplayName("推荐结果应按得分降序排列")
        void resultsShouldBeSortedByScoreDescending() {
            List<Map<String, Object>> results = userCFService.recommend(1L, 10, 20);

            for (int i = 0; i < results.size() - 1; i++) {
                double current = (Double) results.get(i).get("score");
                double next = (Double) results.get(i + 1).get("score");
                assertTrue(current >= next,
                    "Expected descending: " + current + " >= " + next);
            }
        }

        @Test
        @DisplayName("Top-K应限制返回数量")
        void topKShouldLimitResults() {
            List<Map<String, Object>> results = userCFService.recommend(1L, 5, 3);
            assertTrue(results.size() <= 3, "Should not exceed topN");
        }

        @Test
        @DisplayName("结果应包含必要字段")
        void resultsShouldContainRequiredFields() {
            List<Map<String, Object>> results = userCFService.recommend(1L, 5, 5);

            for (Map<String, Object> r : results) {
                assertNotNull(r.get("gameId"));
                assertNotNull(r.get("score"));
                assertNotNull(r.get("gameName"));
                assertTrue(((String) r.get("gameName")).startsWith("Game_"));
            }
        }
    }

    // ==================== 底层评分问题 ====================

    @Test
    @DisplayName("低分游戏(<4)不应作为候选")
    void shouldFilterOutLowRatedGames() {
        Map<Long, Integer> targetRatings = new HashMap<>();
        targetRatings.put(1L, 5);

        // 相似用户只对候选游戏打3分（低于4分阈值）
        Map<Long, Integer> otherRatings = new HashMap<>();
        otherRatings.put(1L, 5);
        otherRatings.put(200L, 3); // 候选游戏只有3分，不应该被推荐

        Map<Long, Map<Long, Integer>> matrix = new HashMap<>();
        matrix.put(1L, targetRatings);
        matrix.put(2L, otherRatings);

        when(matrixService.getUserRatings(1L)).thenReturn(targetRatings);
        when(matrixService.getRatingMatrix()).thenReturn(matrix);

        List<Map<String, Object>> results = userCFService.recommend(1L, 10, 10);

        for (Map<String, Object> r : results) {
            assertNotEquals(200L, r.get("gameId"),
                "Game with rating < 4 should not be recommended");
        }
    }
}
