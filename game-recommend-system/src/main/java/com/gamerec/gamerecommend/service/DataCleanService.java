package com.gamerec.gamerecommend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gamerec.gamerecommend.entity.Rating;
import com.gamerec.gamerecommend.mapper.RatingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据清洗服务
 *
 * 处理三种数据质量问题：
 *   1. 缺失值 —— rating/play_hours字段为NULL
 *   2. 异常值 —— 评分不在1-5范围；游玩时长<0或>10000（挂机）
 *   3. 重复值 —— 同一用户对同一游戏多条记录（保留play_hours最大的一条）
 *
 * 清洗策略：逻辑删除（is_valid=0），保留原始数据，可追溯、可恢复。
 *
 * @author GameRec Team
 */
@Slf4j
@Service
public class DataCleanService {

    @Autowired
    private RatingMapper ratingMapper;

    /**
     * 完整清洗流程入口
     * 依次处理：缺失值 → 异常值 → 重复值
     *
     * @return 清洗统计Map
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cleanRatings() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 清洗前统计
        long totalBefore = ratingMapper.selectCount(
                new QueryWrapper<Rating>().eq("is_valid", 1));
        result.put("totalBefore", totalBefore);

        // 1. 处理缺失值
        int missingCleaned = handleMissingValues();
        result.put("missingCleaned", missingCleaned);

        // 2. 处理异常值
        Map<String, Integer> abnormalResult = handleAbnormalValues();
        result.put("abnormalRatingCleaned", abnormalResult.get("abnormalRating"));
        result.put("abnormalHoursCleaned", abnormalResult.get("abnormalHours"));

        // 3. 处理重复值
        int duplicateCleaned = handleDuplicateValues();
        result.put("duplicateCleaned", duplicateCleaned);

        // 清洗后统计
        long totalAfter = ratingMapper.selectCount(
                new QueryWrapper<Rating>().eq("is_valid", 1));
        result.put("totalAfter", totalAfter);

        int totalCleaned = missingCleaned + abnormalResult.get("abnormalRating")
                + abnormalResult.get("abnormalHours") + duplicateCleaned;
        result.put("totalCleaned", totalCleaned);

        log.info("数据清洗完成: 清洗前{}条 → 清洗后{}条, 清理{}条",
                totalBefore, totalAfter, totalCleaned);

        return result;
    }

    /**
     * 处理缺失值
     * 标记 rating 或 play_hours 为 NULL 的记录
     *
     * @return 清理条数
     */
    private int handleMissingValues() {
        QueryWrapper<Rating> wrapper = new QueryWrapper<>();
        wrapper.eq("is_valid", 1)
               .and(w -> w.isNull("rating").or().isNull("play_hours"));

        long count = ratingMapper.selectCount(wrapper);
        if (count > 0) {
            Rating updateEntity = new Rating();
            updateEntity.setIsValid(0);
            ratingMapper.update(updateEntity, wrapper);
            log.info("缺失值清理: {} 条", count);
        } else {
            log.info("缺失值检测: 0 条");
        }
        return (int) count;
    }

    /**
     * 处理异常值
     * - 异常评分：rating < 1 或 rating > 5
     * - 异常游玩时长：play_hours < 0 或 play_hours > 10000
     *
     * @return Map包含两类异常清理数量
     */
    private Map<String, Integer> handleAbnormalValues() {
        Map<String, Integer> result = new HashMap<>();

        // 异常评分
        QueryWrapper<Rating> ratingWrapper = new QueryWrapper<>();
        ratingWrapper.eq("is_valid", 1)
                     .and(w -> w.lt("rating", 1).or().gt("rating", 5));
        long abnormalRating = ratingMapper.selectCount(ratingWrapper);
        if (abnormalRating > 0) {
            Rating updateEntity = new Rating();
            updateEntity.setIsValid(0);
            ratingMapper.update(updateEntity, ratingWrapper);
            log.info("异常评分清理: {} 条", abnormalRating);
        } else {
            log.info("异常评分检测: 0 条");
        }
        result.put("abnormalRating", (int) abnormalRating);

        // 异常游玩时长
        QueryWrapper<Rating> hoursWrapper = new QueryWrapper<>();
        hoursWrapper.eq("is_valid", 1)
                    .and(w -> w.lt("play_hours", 0).or().gt("play_hours", 10000));
        long abnormalHours = ratingMapper.selectCount(hoursWrapper);
        if (abnormalHours > 0) {
            Rating updateEntity = new Rating();
            updateEntity.setIsValid(0);
            ratingMapper.update(updateEntity, hoursWrapper);
            log.info("异常游玩时长清理: {} 条", abnormalHours);
        } else {
            log.info("异常游玩时长检测: 0 条");
        }
        result.put("abnormalHours", (int) abnormalHours);

        return result;
    }

    /**
     * 处理重复值
     *
     * 算法：
     *   1. 查询所有有效评分数据（is_valid=1）
     *   2. 创建Map<String, Long>，key为"userId_gameId"
     *   3. 遍历所有评分：若Map中已存在→比较play_hours，保留更大的
     *   4. 找出所有需要标记为无效的重复记录id
     *   5. 批量更新 is_valid=0
     *
     * 时间复杂度：O(n)
     *
     * @return 清理条数
     */
    private int handleDuplicateValues() {
        // 1. 查询所有有效记录，按play_hours降序
        QueryWrapper<Rating> wrapper = new QueryWrapper<>();
        wrapper.eq("is_valid", 1)
               .orderByDesc("user_id", "game_id", "play_hours");

        List<Rating> allRatings = ratingMapper.selectList(wrapper);

        // 2. 找出需要标记的重复记录
        Map<String, Long> seen = new HashMap<>(); // key -> 保留的id
        List<Long> duplicateIds = new ArrayList<>();

        for (Rating rating : allRatings) {
            String key = rating.getUserId() + "_" + rating.getGameId();

            if (seen.containsKey(key)) {
                // 已存在 → 当前记录是重复的（因为已按play_hours降序排列）
                duplicateIds.add(rating.getId());
            } else {
                seen.put(key, rating.getId());
            }
        }

        // 3. 批量更新重复记录
        if (!duplicateIds.isEmpty()) {
            // 分批更新
            int batchSize = 1000;
            for (int i = 0; i < duplicateIds.size(); i += batchSize) {
                int end = Math.min(i + batchSize, duplicateIds.size());
                List<Long> batch = duplicateIds.subList(i, end);

                Rating updateEntity = new Rating();
                updateEntity.setIsValid(0);

                QueryWrapper<Rating> updateWrapper = new QueryWrapper<>();
                updateWrapper.in("id", batch);

                ratingMapper.update(updateEntity, updateWrapper);
            }
            log.info("重复值清理: {} 条", duplicateIds.size());
        } else {
            log.info("重复值检测: 0 条");
        }

        return duplicateIds.size();
    }

    /**
     * 获取清洗统计（不执行清洗）
     *
     * @return 清洗统计Map
     */
    public Map<String, Object> getCleanStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long totalAll = ratingMapper.selectCount(null);
        long totalValid = ratingMapper.selectCount(
                new QueryWrapper<Rating>().eq("is_valid", 1));
        long totalInvalid = ratingMapper.selectCount(
                new QueryWrapper<Rating>().eq("is_valid", 0));

        stats.put("totalAll", totalAll);
        stats.put("totalValid", totalValid);
        stats.put("totalInvalid", totalInvalid);

        // 缺失值统计
        long missingCount = ratingMapper.selectCount(
                new QueryWrapper<Rating>().eq("is_valid", 1)
                        .and(w -> w.isNull("rating").or().isNull("play_hours")));
        stats.put("missingCount", missingCount);

        // 异常值统计
        long abnormalRatingCount = ratingMapper.selectCount(
                new QueryWrapper<Rating>().eq("is_valid", 1)
                        .and(w -> w.lt("rating", 1).or().gt("rating", 5)));
        stats.put("abnormalRatingCount", abnormalRatingCount);

        long abnormalHoursCount = ratingMapper.selectCount(
                new QueryWrapper<Rating>().eq("is_valid", 1)
                        .and(w -> w.lt("play_hours", 0).or().gt("play_hours", 10000)));
        stats.put("abnormalHoursCount", abnormalHoursCount);

        return stats;
    }
}
