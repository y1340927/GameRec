package com.gamerec.gamerecommend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RatingConvertUtil 单元测试
 *
 * 覆盖所有分箱边界、异常值处理、评分验证
 */
@DisplayName("RatingConvertUtil 单元测试")
class RatingConvertUtilTest {

    // ==================== playHoursToRating ====================

    @Nested
    @DisplayName("游玩时长 → 伪评分 (playHoursToRating)")
    class PlayHoursToRating {

        @ParameterizedTest
        @CsvSource({
            "0,       1",    // 买了没玩
            "0.1,     2",
            "5,       2",    // 边界: =5
            "5.1,     3",
            "20,      3",    // 边界: =20
            "20.1,    4",
            "50,      4",    // 边界: =50
            "50.1,    5",
            "100,     5",
            "500,     5",
            "2000,    5"     // 极端沉迷
        })
        @DisplayName("正常分箱")
        void shouldMapPlayHoursToCorrectRating(double playHours, int expectedRating) {
            assertEquals(expectedRating, RatingConvertUtil.playHoursToRating(playHours),
                "playHours=" + playHours);
        }

        @Test
        @DisplayName("负数应返回1分（异常值兜底）")
        void negativeHoursShouldReturnMinRating() {
            assertEquals(1, RatingConvertUtil.playHoursToRating(-1));
            assertEquals(1, RatingConvertUtil.playHoursToRating(-100));
        }

        @Test
        @DisplayName("所有返回值应在1-5范围")
        void allResultsShouldBeInValidRange() {
            double[] testValues = {-1, 0, 0.5, 3, 5, 10, 20, 35, 50, 100, 500, 2000, 9999};
            for (double hours : testValues) {
                int rating = RatingConvertUtil.playHoursToRating(hours);
                assertTrue(rating >= 1 && rating <= 5,
                    "Rating out of range: " + rating + " for hours=" + hours);
            }
        }
    }

    // ==================== getRatingMeaning ====================

    @Nested
    @DisplayName("评分含义描述 (getRatingMeaning)")
    class RatingMeaning {

        @Test
        @DisplayName("1-5分各有对应描述")
        void shouldReturnCorrectDescription() {
            assertEquals("不感兴趣（买了没玩）", RatingConvertUtil.getRatingMeaning(1));
            assertEquals("尝试一下（0~5h）", RatingConvertUtil.getRatingMeaning(2));
            assertEquals("一般喜欢（5~20h）", RatingConvertUtil.getRatingMeaning(3));
            assertEquals("比较喜欢（20~50h）", RatingConvertUtil.getRatingMeaning(4));
            assertEquals("非常喜欢（>50h）", RatingConvertUtil.getRatingMeaning(5));
        }

        @Test
        @DisplayName("越界评分应返回'未知'")
        void outOfRangeShouldReturnUnknown() {
            assertEquals("未知", RatingConvertUtil.getRatingMeaning(0));
            assertEquals("未知", RatingConvertUtil.getRatingMeaning(6));
            assertEquals("未知", RatingConvertUtil.getRatingMeaning(-1));
            assertEquals("未知", RatingConvertUtil.getRatingMeaning(99));
        }
    }

    // ==================== isValidRating ====================

    @Nested
    @DisplayName("评分有效性检查 (isValidRating)")
    class RatingValidation {

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3, 4, 5})
        @DisplayName("1-5应为有效评分")
        void validRatingsShouldReturnTrue(int rating) {
            assertTrue(RatingConvertUtil.isValidRating(rating));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 6, -1, 100})
        @DisplayName("越界评分应无效")
        void outOfRangeRatingsShouldReturnFalse(int rating) {
            assertFalse(RatingConvertUtil.isValidRating(rating));
        }
    }

    // ==================== isAbnormalPlayHours ====================

    @Nested
    @DisplayName("异常游玩时长检查 (isAbnormalPlayHours)")
    class AbnormalPlayHours {

        @Test
        @DisplayName("正常时长应返回false")
        void normalHoursShouldNotBeAbnormal() {
            assertFalse(RatingConvertUtil.isAbnormalPlayHours(0));
            assertFalse(RatingConvertUtil.isAbnormalPlayHours(100));
            assertFalse(RatingConvertUtil.isAbnormalPlayHours(5000));
            assertFalse(RatingConvertUtil.isAbnormalPlayHours(10000));
        }

        @Test
        @DisplayName("负数和超大值应视为异常")
        void extremeValuesShouldBeAbnormal() {
            assertTrue(RatingConvertUtil.isAbnormalPlayHours(-0.1));
            assertTrue(RatingConvertUtil.isAbnormalPlayHours(-500));
            assertTrue(RatingConvertUtil.isAbnormalPlayHours(10000.1));
            assertTrue(RatingConvertUtil.isAbnormalPlayHours(99999));
        }
    }
}
