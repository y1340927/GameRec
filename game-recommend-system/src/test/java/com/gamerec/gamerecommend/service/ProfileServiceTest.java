package com.gamerec.gamerecommend.service;

import com.gamerec.gamerecommend.entity.Rating;
import com.gamerec.gamerecommend.mapper.GameMapper;
import com.gamerec.gamerecommend.mapper.RatingMapper;
import com.gamerec.gamerecommend.mapper.UserMapper;
import com.gamerec.gamerecommend.mapper.UserProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * ProfileService 单元测试
 *
 * 覆盖核心业务逻辑：
 *   1. 玩家类型分类 (classifyPlayerType)
 *   2. 购买-游玩转化率 (calculatePurchasePlayRatio)
 *   3. RFM 价值分层 (calculateRFM)
 *   4. 时间衰减活跃度 (calculateActivity)
 *
 * 使用 Mockito 隔离数据库依赖，只验证业务逻辑正确性。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService 单元测试")
class ProfileServiceTest {

    @Mock
    private RatingMapper ratingMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private GameMapper gameMapper;

    @Mock
    private UserProfileMapper userProfileMapper;

    @InjectMocks
    private ProfileService profileService;

    // ==================== 测试数据工厂 ====================

    private Rating createRating(Long userId, Long gameId, Integer rating, Double playHours, int daysAgo) {
        Rating r = new Rating();
        r.setUserId(userId);
        r.setGameId(gameId);
        r.setRating(rating);
        r.setPlayHours(playHours);
        r.setIsValid(1);
        r.setCreateTime(LocalDateTime.now().minusDays(daysAgo));
        return r;
    }

    // ==================== 玩家类型分类测试 ====================

    @Nested
    @DisplayName("玩家类型分类 - classifyPlayerType")
    class PlayerTypeClassification {

        @Test
        @DisplayName("无评分记录应返回新用户")
        void shouldReturnNewPlayerWhenNoRatings() {
            when(ratingMapper.selectList(any())).thenReturn(Collections.emptyList());

            String type = profileService.classifyPlayerType(1L);
            assertEquals("新用户", type);
        }

        @Test
        @DisplayName("硬核玩家：总游玩时长 > 500小时")
        void shouldClassifyAsHardcoreWhenPlayHoursExceeds500() {
            List<Rating> ratings = Arrays.asList(
                createRating(1L, 100L, 5, 300.0, 10),
                createRating(1L, 101L, 5, 250.0, 20)
            );
            when(ratingMapper.selectList(any())).thenReturn(ratings);

            String type = profileService.classifyPlayerType(1L);
            assertEquals("硬核玩家", type);
        }

        @Test
        @DisplayName("尝鲜玩家：总时长 < 10h 但游戏数 > 20")
        void shouldClassifyAsExplorerWhenManyGamesButLowHours() {
            List<Rating> ratings = new ArrayList<>();
            for (long i = 0; i < 25; i++) {
                ratings.add(createRating(1L, 100L + i, 3, 0.3, 5));
            }
            when(ratingMapper.selectList(any())).thenReturn(ratings);

            String type = profileService.classifyPlayerType(1L);
            assertEquals("尝鲜玩家", type);
        }

        @Test
        @DisplayName("收藏玩家：购买数 > 游玩数 × 2")
        void shouldClassifyAsCollectorWhenPurchasesDoublePlays() {
            List<Rating> ratings = Arrays.asList(
                createRating(1L, 100L, 3, 10.0, 5),  // played
                createRating(1L, 101L, 1, 0.0, 5),   // not played
                createRating(1L, 102L, 1, 0.0, 5),   // not played
                createRating(1L, 103L, 1, 0.0, 5)    // not played
            );
            when(ratingMapper.selectList(any())).thenReturn(ratings);

            String type = profileService.classifyPlayerType(1L);
            assertEquals("收藏玩家", type);
        }

        @Test
        @DisplayName("休闲玩家：10-500h 游玩时长")
        void shouldClassifyAsCasualForMidRangePlayHours() {
            List<Rating> ratings = Arrays.asList(
                createRating(1L, 100L, 3, 50.0, 10),
                createRating(1L, 101L, 4, 80.0, 30)
            );
            when(ratingMapper.selectList(any())).thenReturn(ratings);

            String type = profileService.classifyPlayerType(1L);
            assertEquals("休闲玩家", type);
        }

        @Test
        @DisplayName("收藏玩家优先级高于其他分类")
        void collectorShouldTakePriorityOverOtherTypes() {
            List<Rating> ratings = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                ratings.add(createRating(1L, (long) (100 + i), 1, 0.0, 5));   // not played
            }
            for (int i = 0; i < 3; i++) {
                ratings.add(createRating(1L, (long) (200 + i), 5, 200.0, 10)); // played, total=600h
            }
            when(ratingMapper.selectList(any())).thenReturn(ratings);

            String type = profileService.classifyPlayerType(1L);
            assertEquals("收藏玩家", type); // 购买11 > 游玩3*2=6, 优先判定为收藏玩家
        }
    }

    // ==================== 购买-游玩转化率测试 ====================

    @Nested
    @DisplayName("购买-游玩转化率 - calculatePurchasePlayRatio")
    class PurchasePlayRatio {

        @Test
        @DisplayName("正常转化率计算")
        void shouldCalculateNormalConversionRatio() {
            List<Rating> ratings = Arrays.asList(
                createRating(1L, 100L, 5, 100.0, 5),
                createRating(1L, 101L, 3, 50.0, 10),
                createRating(1L, 102L, 4, 30.0, 15),
                createRating(1L, 103L, 1, 0.0, 7)   // bought, not played
            );
            when(ratingMapper.selectList(any())).thenReturn(ratings);

            Map<String, Object> result = profileService.calculatePurchasePlayRatio(1L);

            assertEquals(4, result.get("purchaseCount"));
            assertEquals(3, result.get("playCount"));
            assertEquals(0.75, result.get("ratio"));
            assertEquals(180.0, result.get("totalPlayHours"));
            assertEquals(60.0, result.get("avgPlayHours"));
        }

        @Test
        @DisplayName("无评分记录时应返回零值")
        void shouldReturnZerosWhenNoRatings() {
            when(ratingMapper.selectList(any())).thenReturn(Collections.emptyList());

            Map<String, Object> result = profileService.calculatePurchasePlayRatio(1L);

            assertEquals(0, result.get("purchaseCount"));
            assertEquals(0.0, result.get("ratio"));
            assertEquals(0.0, result.get("totalPlayHours"));
        }

        @Test
        @DisplayName("全部未游玩应返回0转化率")
        void shouldReturnZeroRatioWhenAllUnplayed() {
            List<Rating> ratings = Arrays.asList(
                createRating(1L, 100L, 1, 0.0, 5),
                createRating(1L, 101L, 1, 0.0, 10)
            );
            when(ratingMapper.selectList(any())).thenReturn(ratings);

            Map<String, Object> result = profileService.calculatePurchasePlayRatio(1L);

            assertEquals(2, result.get("purchaseCount"));
            assertEquals(0, result.get("playCount"));
            assertEquals(0.0, result.get("ratio"));
        }
    }

    // ==================== RFM 价值分层测试 ====================

    @Nested
    @DisplayName("RFM价值分层 - calculateRFM")
    class RFMCalculation {

        @Test
        @DisplayName("无评分记录应返回沉默用户")
        void shouldReturnSilentUserWhenNoRatings() {
            when(ratingMapper.selectList(any())).thenReturn(Collections.emptyList());

            Map<String, Object> rfm = profileService.calculateRFM(1L);

            assertEquals("沉默用户", rfm.get("valueLevel"));
            assertEquals(0, rfm.get("frequency"));
            assertEquals(0.0, rfm.get("monetary"));
        }

        @Test
        @DisplayName("正常计算R/F/M和评分")
        void shouldCalculateRFMForActiveUser() {
            List<Rating> ratings = Arrays.asList(
                createRating(1L, 100L, 5, 300.0, 0),   // today
                createRating(1L, 101L, 3, 50.0, 1),    // yesterday
                createRating(1L, 102L, 4, 30.0, 3)     // 3 days ago
            );
            when(ratingMapper.selectList(any())).thenReturn(ratings);

            // Mock global RFM data
            when(ratingMapper.selectMaps(any()))
                .thenReturn(Collections.emptyList()); // Will use empty list for simple testing

            Map<String, Object> rfm = profileService.calculateRFM(1L);

            assertEquals(0L, rfm.get("recency")); // 0 days since last play
            assertEquals(3, rfm.get("frequency"));
            assertNotNull(rfm.get("valueLevel"));
        }
    }

    // ==================== 时间衰减活跃度测试 ====================

    @Nested
    @DisplayName("时间衰减活跃度 - calculateActivity")
    class ActivityCalculation {

        @Test
        @DisplayName("无评分记录应返回冰封用户")
        void shouldReturnFrozenUserWhenNoRatings() {
            when(ratingMapper.selectList(any())).thenReturn(Collections.emptyList());

            Map<String, Object> activity = profileService.calculateActivity(1L, 0.01);

            assertEquals("冰封用户", activity.get("activityLevel"));
            assertEquals(0.0, activity.get("rawScore"));
            assertEquals(0, activity.get("ratingCount"));
        }

        @Test
        @DisplayName("最近评分权重高于旧评分")
        void shouldGiveHigherWeightToRecentRatings() {
            List<Rating> ratings = Arrays.asList(
                createRating(1L, 100L, 5, 100.0, 0),    // today, weight = e^(-0) = 1.0
                createRating(1L, 101L, 5, 100.0, 100)   // 100 days ago, weight = e^(-1) ≈ 0.368
            );
            when(ratingMapper.selectList(any())).thenReturn(ratings);

            Map<String, Object> activity = profileService.calculateActivity(1L, 0.01);

            double rawScore = (Double) activity.get("rawScore");
            // Recent: 5 * 1.0 = 5.0, Old: 5 * 0.368 = 1.84, Total ≈ 6.84
            assertTrue(rawScore > 5.0, "Raw score should be > 5.0 due to decay weighting");
            assertTrue(rawScore < 7.0, "Raw score should be < 7.0");
        }

        @Test
        @DisplayName("高衰减率使旧评分贡献更小")
        void highDecayRateShouldReduceOldRatingContribution() {
            List<Rating> oldRating = Collections.singletonList(
                createRating(1L, 100L, 5, 100.0, 100) // 100 days ago
            );
            when(ratingMapper.selectList(any())).thenReturn(oldRating);

            double lowDecayScore = (Double) profileService.calculateActivity(1L, 0.01).get("rawScore");
            double highDecayScore = (Double) profileService.calculateActivity(1L, 0.05).get("rawScore");

            assertTrue(highDecayScore < lowDecayScore,
                "Higher decay rate should produce lower score for old ratings");
        }
    }

    // ==================== 完整画像整合测试 ====================

    @Nested
    @DisplayName("完整画像整合 - getFullProfile")
    class FullProfileIntegration {

        @Test
        @DisplayName("getFullProfile应正确组装四个维度")
        void shouldAssembleAllFourDimensions() {
            List<Rating> ratings = Arrays.asList(
                createRating(1L, 100L, 5, 200.0, 5),
                createRating(1L, 101L, 3, 100.0, 10)
            );
            when(ratingMapper.selectList(any())).thenReturn(ratings);
            when(ratingMapper.selectMaps(any())).thenReturn(Collections.emptyList());

            var profile = profileService.getFullProfile(1L);

            assertNotNull(profile.getRfm(), "RFM should not be null");
            assertNotNull(profile.getActivity(), "Activity should not be null");
            assertNotNull(profile.getInterestTags(), "Interest tags should not be null");
            assertNotNull(profile.getPlayerType(), "Player type should not be null");
            assertEquals(1L, profile.getUserId());
        }
    }

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCases {

        @Test
        @DisplayName("游玩时长正好500小时应为休闲玩家，非硬核")
        void exactly500HoursShouldBeCasual() {
            List<Rating> ratings = Arrays.asList(
                createRating(1L, 100L, 5, 500.0, 10)
            );
            when(ratingMapper.selectList(any())).thenReturn(ratings);

            String type = profileService.classifyPlayerType(1L);
            assertEquals("休闲玩家", type); // > 500 not >= 500
        }

        @Test
        @DisplayName("游玩时长正好10小时且游戏数正好20应为休闲玩家")
        void boundaryConditionsForExplorer() {
            List<Rating> ratings = new ArrayList<>();
            for (long i = 0; i < 20; i++) {
                ratings.add(createRating(1L, 100L + i, 3, 0.5, 5));
            }
            when(ratingMapper.selectList(any())).thenReturn(ratings);

            String type = profileService.classifyPlayerType(1L);
            assertEquals("休闲玩家", type); // total=10 not < 10, play=20 not > 20
        }

        @Test
        @DisplayName("购买刚好等于游玩×2不应判定为收藏玩家")
        void exactDoublePurchaseNotCollector() {
            List<Rating> ratings = Arrays.asList(
                createRating(1L, 100L, 3, 10.0, 5),
                createRating(1L, 101L, 3, 10.0, 5),
                createRating(1L, 102L, 1, 0.0, 5),
                createRating(1L, 103L, 1, 0.0, 5)
            );
            when(ratingMapper.selectList(any())).thenReturn(ratings);

            String type = profileService.classifyPlayerType(1L);
            assertEquals("休闲玩家", type); // 购买4, 游玩2: 4 > 2*2=4 is false
        }
    }
}
