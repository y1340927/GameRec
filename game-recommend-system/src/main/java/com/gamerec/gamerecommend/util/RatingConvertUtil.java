package com.gamerec.gamerecommend.util;

/**
 * 隐式反馈→显式评分转化工具类（核心创新）
 *
 * 将Steam数据集的游玩时长（隐式反馈）转化为伪评分（1-5分显式评分），
 * 使其适配后续所有依赖评分矩阵的推荐算法（User-CF、Item-CF、CB、SVD等）。
 *
 * 分箱策略：
 *   - 0小时（买了没玩）   → 1分（不感兴趣）
 *   - 0 < hours <= 5      → 2分（尝试一下）
 *   - 5 < hours <= 20     → 3分（一般喜欢）
 *   - 20 < hours <= 50    → 4分（比较喜欢）
 *   - hours > 50          → 5分（非常喜欢/沉迷）
 *
 * 设计理由：游玩时长是比显式评分更真实的偏好信号（用户"用脚投票"）。
 * 通过分箱转化，既保留了原始行为信息，又使其适配全部推荐算法。
 *
 * @author GameRec Team
 */
public class RatingConvertUtil {

    /** 评分下限 */
    public static final int MIN_RATING = 1;

    /** 评分上限 */
    public static final int MAX_RATING = 5;

    /**
     * 游玩时长分箱→伪评分（1-5分）
     *
     * @param playHours 游玩时长（小时）
     * @return 伪评分（1-5分）
     */
    public static int playHoursToRating(double playHours) {
        if (playHours < 0) {
            // 异常值：默认返回1分
            return MIN_RATING;
        }
        if (playHours == 0) {
            // 买了没玩 → 不感兴趣
            return 1;
        } else if (playHours <= 5) {
            // 尝试一下
            return 2;
        } else if (playHours <= 20) {
            // 一般喜欢
            return 3;
        } else if (playHours <= 50) {
            // 比较喜欢
            return 4;
        } else {
            // 非常喜欢（沉迷）
            return 5;
        }
    }

    /**
     * 获取评分对应的含义描述
     *
     * @param rating 伪评分（1-5）
     * @return 含义描述
     */
    public static String getRatingMeaning(int rating) {
        switch (rating) {
            case 1: return "不感兴趣（买了没玩）";
            case 2: return "尝试一下（0~5h）";
            case 3: return "一般喜欢（5~20h）";
            case 4: return "比较喜欢（20~50h）";
            case 5: return "非常喜欢（>50h）";
            default: return "未知";
        }
    }

    /**
     * 检查评分是否在有效范围
     *
     * @param rating 评分
     * @return 是否有效
     */
    public static boolean isValidRating(int rating) {
        return rating >= MIN_RATING && rating <= MAX_RATING;
    }

    /**
     * 检查游玩时长是否为异常值
     *
     * @param playHours 游玩时长（小时）
     * @return 是否异常（<0 或 >10000 视为挂机/异常数据）
     */
    public static boolean isAbnormalPlayHours(double playHours) {
        return playHours < 0 || playHours > 10000;
    }
}
