package com.gamerec.gamerecommend.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gamerec.gamerecommend.dto.UserProfileDTO;
import com.gamerec.gamerecommend.entity.Game;
import com.gamerec.gamerecommend.entity.Rating;
import com.gamerec.gamerecommend.entity.User;
import com.gamerec.gamerecommend.entity.UserProfile;
import com.gamerec.gamerecommend.mapper.GameMapper;
import com.gamerec.gamerecommend.mapper.RatingMapper;
import com.gamerec.gamerecommend.mapper.UserMapper;
import com.gamerec.gamerecommend.mapper.UserProfileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 玩家画像服务
 *
 * 四个维度：
 *   1. RFM价值分层
 *   2. 时间衰减活跃度
 *   3. 兴趣标签提取
 *   4. 玩家类型分类
 *
 * @author GameRec Team
 */
@Slf4j
@Service
public class ProfileService {

    @Autowired
    private RatingMapper ratingMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GameMapper gameMapper;

    @Autowired
    private UserProfileMapper userProfileMapper;

    /**
     * 将系统用户ID(user.id)转换为SteamID(user.user_id)用于查询rating表
     * rating表使用SteamID作为user_id字段值
     */
    private Long resolveSteamId(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getUserId() : userId;
    }

    // ==================== RFM价值分层 ====================

    /**
     * 计算单个用户的RFM评分
     *
     * @param userId 系统用户ID（对应 user.id）
     * @return RFM评分Map
     */
    public Map<String, Object> calculateRFM(Long userId) {
        // 将系统用户ID转换为SteamID（rating表使用SteamID）
        Long steamId = resolveSteamId(userId);
        // 查询用户所有有效评分
        QueryWrapper<Rating> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", steamId).eq("is_valid", 1);
        List<Rating> ratings = ratingMapper.selectList(wrapper);

        if (ratings.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("recency", 0L);
            empty.put("frequency", 0);
            empty.put("monetary", 0.0);
            empty.put("rScore", 1);
            empty.put("fScore", 1);
            empty.put("mScore", 1);
            empty.put("valueLevel", "沉默用户");
            return empty;
        }

        // R: 最近游玩距今天数
        LocalDateTime latestPlay = ratings.stream()
                .map(Rating::getCreateTime)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
        long recency = ChronoUnit.DAYS.between(latestPlay.toLocalDate(), LocalDateTime.now().toLocalDate());

        // F: 游玩游戏总数
        int frequency = (int) ratings.stream().map(Rating::getGameId).distinct().count();

        // M: 平均伪评分
        double monetary = ratings.stream().mapToInt(Rating::getRating).average().orElse(0);

        // 获取全局RFM数据用于四分位法打分（优先使用批量预计算缓存，兼容单用户调用）
        List<Double> allRecencies = cachedAllRecencies != null ? cachedAllRecencies : getAllRecencies();
        List<Integer> allFrequencies = cachedAllFrequencies != null ? cachedAllFrequencies : getAllFrequencies();
        List<Double> allMonetaries = cachedAllMonetaries != null ? cachedAllMonetaries : getAllMonetaries();

        int rScore = calculateRScore(recency, allRecencies);
        int fScore = calculateFMScore(frequency, allFrequencies);
        int mScore = calculateFMScore(monetary, allMonetaries);
        int totalScore = rScore + fScore + mScore;
        String valueLevel = getValueLevel(totalScore);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recency", recency);
        result.put("frequency", frequency);
        result.put("monetary", Math.round(monetary * 100.0) / 100.0);
        result.put("rScore", rScore);
        result.put("fScore", fScore);
        result.put("mScore", mScore);
        result.put("totalScore", totalScore);
        result.put("valueLevel", valueLevel);

        return result;
    }

    /**
     * 获取所有用户的R值列表
     */
    private List<Double> getAllRecencies() {
        QueryWrapper<Rating> wrapper = new QueryWrapper<>();
        wrapper.eq("is_valid", 1)
                .select("user_id", "max(create_time) as latest")
                .groupBy("user_id");
        List<Map<String, Object>> list = ratingMapper.selectMaps(wrapper);

        return list.stream()
                .map(m -> {
                    LocalDateTime latest = (LocalDateTime) m.get("latest");
                    return (double) ChronoUnit.DAYS.between(
                            latest.toLocalDate(), LocalDateTime.now().toLocalDate());
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取所有用户的F值列表
     */
    private List<Integer> getAllFrequencies() {
        QueryWrapper<Rating> wrapper = new QueryWrapper<>();
        wrapper.eq("is_valid", 1)
                .select("user_id", "count(distinct game_id) as freq")
                .groupBy("user_id");
        List<Map<String, Object>> list = ratingMapper.selectMaps(wrapper);

        return list.stream()
                .map(m -> ((Number) m.get("freq")).intValue())
                .collect(Collectors.toList());
    }

    /**
     * 获取所有用户的M值列表
     */
    private List<Double> getAllMonetaries() {
        QueryWrapper<Rating> wrapper = new QueryWrapper<>();
        wrapper.eq("is_valid", 1)
                .select("user_id", "avg(rating) as avg_r")
                .groupBy("user_id");
        List<Map<String, Object>> list = ratingMapper.selectMaps(wrapper);

        return list.stream()
                .map(m -> ((Number) m.get("avg_r")).doubleValue())
                .collect(Collectors.toList());
    }

    /**
     * 四分位法计算R分数（天数越少分数越高）
     */
    private int calculateRScore(double recency, List<Double> allRecencies) {
        if (allRecencies.isEmpty()) return 1;
        Collections.sort(allRecencies);
        double q1 = getPercentile(allRecencies, 25);
        double q3 = getPercentile(allRecencies, 75);

        if (recency <= q1) return 4;
        else if (recency <= q3) return 3;
        else if (recency <= q3 * 2) return 2;
        else return 1;
    }

    /**
     * 四分位法计算F/M分数（值越大分数越高）
     */
    private int calculateFMScore(double value, List<? extends Number> allValues) {
        if (allValues.isEmpty()) return 1;
        List<Double> sorted = allValues.stream()
                .map(Number::doubleValue)
                .sorted()
                .collect(Collectors.toList());
        double q1 = getPercentile(sorted, 25);
        double q3 = getPercentile(sorted, 75);

        if (value >= q3) return 4;
        else if (value >= q1) return 3;
        else if (value >= q1 / 2) return 2;
        else return 1;
    }

    /**
     * 获取排序列表的第p百分位值
     */
    private double getPercentile(List<Double> sorted, double p) {
        if (sorted.isEmpty()) return 0;
        int index = (int) Math.ceil(sorted.size() * p / 100.0) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }

    /**
     * 根据RFM总分确定价值等级
     */
    private String getValueLevel(int totalScore) {
        if (totalScore >= 10) return "高价值用户";
        else if (totalScore >= 7) return "中价值用户";
        else if (totalScore >= 4) return "低价值用户";
        else return "沉默用户";
    }

    // ==================== 时间衰减活跃度 ====================

    /**
     * 计算用户活跃度（时间衰减加权）
     *
     * 公式：weight = e^(-λ × daysAgo)
     * 加权评分 = rating × weight
     *
     * @param userId    用户ID
     * @param decayRate 衰减率λ（默认0.01）
     * @return 活跃度Map
     */
    public Map<String, Object> calculateActivity(Long userId, double decayRate) {
        Long steamId = resolveSteamId(userId);
        QueryWrapper<Rating> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", steamId).eq("is_valid", 1);
        List<Rating> ratings = ratingMapper.selectList(wrapper);

        if (ratings.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("rawScore", 0.0);
            empty.put("normalizedScore", 0.0);
            empty.put("ratingCount", 0);
            empty.put("activityLevel", "冰封用户");
            return empty;
        }

        LocalDateTime now = LocalDateTime.now();
        double rawScore = 0;

        for (Rating r : ratings) {
            long daysAgo = ChronoUnit.DAYS.between(r.getCreateTime().toLocalDate(), now.toLocalDate());
            double weight = Math.exp(-decayRate * daysAgo);
            rawScore += r.getRating() * weight;
        }

        // 获取全局最大得分用于归一化
        double maxScore = getGlobalMaxActivityScore(decayRate);
        double normalizedScore = maxScore > 0 ? rawScore / maxScore : 0;

        String activityLevel;
        if (normalizedScore >= 0.4) activityLevel = "高热用户";
        else if (normalizedScore >= 0.1) activityLevel = "中热用户";
        else if (normalizedScore >= 0.02) activityLevel = "低热用户";
        else activityLevel = "冰封用户";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rawScore", Math.round(rawScore * 100.0) / 100.0);
        result.put("normalizedScore", Math.round(normalizedScore * 10000.0) / 10000.0);
        result.put("ratingCount", ratings.size());
        result.put("activityLevel", activityLevel);

        return result;
    }

    /**
     * 获取全局最大活跃度得分（用于归一化）- 带缓存提升性能
     */
    private Double cachedMaxActivityScore = null;
    private static final double DEFAULT_DECAY_RATE = 0.01;

    /** 预计算的全局 RFM 统计数据缓存（saveAllProfiles 批量操作时使用，避免 56,789 次循环内重复全表 GROUP BY） */
    private List<Double> cachedAllRecencies;
    private List<Integer> cachedAllFrequencies;
    private List<Double> cachedAllMonetaries;

    private double getGlobalMaxActivityScore(double decayRate) {
        // 使用缓存避免每次计算都重复遍历全量数据
        if (cachedMaxActivityScore != null) {
            return cachedMaxActivityScore;
        }

        double maxScore = 0;
        LocalDateTime now = LocalDateTime.now();

        QueryWrapper<Rating> wrapper = new QueryWrapper<>();
        wrapper.eq("is_valid", 1);
        List<Rating> allRatings = ratingMapper.selectList(wrapper);

        // 按用户分组计算活跃度
        Map<Long, List<Rating>> userGroups = allRatings.stream()
                .collect(Collectors.groupingBy(Rating::getUserId));

        for (Map.Entry<Long, List<Rating>> entry : userGroups.entrySet()) {
            double score = 0;
            for (Rating r : entry.getValue()) {
                long daysAgo = ChronoUnit.DAYS.between(r.getCreateTime().toLocalDate(), now.toLocalDate());
                score += r.getRating() * Math.exp(-decayRate * daysAgo);
            }
            maxScore = Math.max(maxScore, score);
        }

        cachedMaxActivityScore = maxScore;
        return maxScore;
    }

    // ==================== 兴趣标签提取 ====================

    /**
     * 提取用户兴趣标签
     *
     * 基于用户游玩的游戏类型（genres）进行聚合统计
     *
     * @param userId 用户ID
     * @return 兴趣标签列表
     */
    public List<Map<String, Object>> calculateInterestTags(Long userId) {
        Long steamId = resolveSteamId(userId);
        // 查询用户游玩过的游戏
        QueryWrapper<Rating> ratingWrapper = new QueryWrapper<>();
        ratingWrapper.eq("user_id", steamId).eq("is_valid", 1);
        List<Rating> ratings = ratingMapper.selectList(ratingWrapper);

        if (ratings.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取所有涉及的游戏
        Set<Long> gameIds = ratings.stream().map(Rating::getGameId).collect(Collectors.toSet());
        List<Game> games = gameMapper.selectBatchIds(gameIds);

        // 按游戏类型聚合
        Map<String, List<Integer>> genreRatings = new HashMap<>();
        for (Rating r : ratings) {
            Game game = games.stream()
                    .filter(g -> g.getGameId().equals(r.getGameId()))
                    .findFirst().orElse(null);
            if (game == null || game.getGenres() == null) continue;

            String[] genres = game.getGenres().split(";");
            for (String genre : genres) {
                genre = genre.trim();
                if (genre.isEmpty()) continue;
                genreRatings.computeIfAbsent(genre, k -> new ArrayList<>()).add(r.getRating());
            }
        }

        // 计算每个类型的平均分和频次
        List<Map<String, Object>> tags = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> entry : genreRatings.entrySet()) {
            Map<String, Object> tag = new LinkedHashMap<>();
            tag.put("tag", entry.getKey());
            double avgScore = entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0);
            tag.put("avgScore", Math.round(avgScore * 100.0) / 100.0);
            tag.put("count", entry.getValue().size());
            tags.add(tag);
        }

        // 按频次×平均分排序，取Top-10
        tags.sort((a, b) -> {
            double scoreA = (double) a.get("count") * (double) a.get("avgScore");
            double scoreB = (double) b.get("count") * (double) b.get("avgScore");
            return Double.compare(scoreB, scoreA);
        });

        return tags.size() > 10 ? tags.subList(0, 10) : tags;
    }

    // ==================== 玩家类型分类 ====================

    /**
     * 分类玩家类型（创新）
     *
     * 四类玩家：
     *   - 硬核玩家：总游玩时长 > 500小时
     *   - 休闲玩家：总游玩时长 10-500小时
     *   - 尝鲜玩家：总时长 < 10小时 但游玩游戏数 > 20
     *   - 收藏玩家：购买数 > 游玩数 × 2
     *
     * @param userId 用户ID
     * @return 玩家类型
     */
    public String classifyPlayerType(Long userId) {
        Long steamId = resolveSteamId(userId);
        QueryWrapper<Rating> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", steamId).eq("is_valid", 1);
        List<Rating> ratings = ratingMapper.selectList(wrapper);

        if (ratings.isEmpty()) {
            return "新用户";
        }

        int purchaseCount = ratings.size();
        int playCount = (int) ratings.stream().filter(r -> r.getPlayHours() > 0).count();
        double totalPlayHours = ratings.stream().mapToDouble(Rating::getPlayHours).sum();

        // 收藏玩家：购买多但游玩少
        if (purchaseCount > playCount * 2) {
            return "收藏玩家";
        }

        // 尝鲜玩家：游玩时间短但游戏数量多
        if (totalPlayHours < 10 && playCount > 20) {
            return "尝鲜玩家";
        }

        // 硬核玩家：深度投入
        if (totalPlayHours > 500) {
            return "硬核玩家";
        }

        // 默认休闲玩家
        return "休闲玩家";
    }

    // ==================== 购买-游玩转化率 ====================

    /**
     * 计算购买-游玩转化率
     *
     * @param userId 用户ID
     * @return 转化率数据
     */
    public Map<String, Object> calculatePurchasePlayRatio(Long userId) {
        Long steamId = resolveSteamId(userId);
        QueryWrapper<Rating> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", steamId).eq("is_valid", 1);
        List<Rating> ratings = ratingMapper.selectList(wrapper);

        int purchaseCount = ratings.size();
        int playCount = (int) ratings.stream().filter(r -> r.getPlayHours() > 0).count();
        double ratio = purchaseCount > 0 ? (double) playCount / purchaseCount : 0;
        double totalPlayHours = ratings.stream().mapToDouble(Rating::getPlayHours).sum();
        double avgPlayHours = playCount > 0 ? totalPlayHours / playCount : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("purchaseCount", purchaseCount);
        result.put("playCount", playCount);
        result.put("ratio", Math.round(ratio * 10000.0) / 10000.0);
        result.put("ratioPercent", String.format("%.1f%%", ratio * 100));
        result.put("totalPlayHours", Math.round(totalPlayHours * 100.0) / 100.0);
        result.put("avgPlayHours", Math.round(avgPlayHours * 100.0) / 100.0);

        return result;
    }

    // ==================== 完整画像整合 ====================

    /**
     * 获取完整玩家画像（优先从缓存表读取，回退动态计算）
     *
     * @param userId 用户ID（对应 user.id 自增主键）
     * @return 完整画像DTO
     */
    public UserProfileDTO getFullProfile(Long userId) {
        // ---- 优先从 user_profile 表读取预计算数据 ----
        QueryWrapper<UserProfile> profileWrapper = new QueryWrapper<>();
        profileWrapper.eq("user_id", userId);
        UserProfile stored = userProfileMapper.selectOne(profileWrapper);

        if (stored != null) {
            return buildDTOFromStored(stored);
        }

        // ---- 回退：无预计算数据时动态计算 ----
        log.info("用户 {} 无预计算画像数据，启用动态计算", userId);
        return buildDTOFromDynamic(userId);
    }

    /**
     * 从预计算数据构建DTO（高性能）
     */
    private UserProfileDTO buildDTOFromStored(UserProfile p) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setUserId(p.getUserId());

        // RFM
        Map<String, Object> rfm = new LinkedHashMap<>();
        rfm.put("recency", p.getRecency());
        rfm.put("frequency", p.getFrequency());
        rfm.put("monetary", p.getMonetary());
        rfm.put("rScore", p.getRScore());
        rfm.put("fScore", p.getFScore());
        rfm.put("mScore", p.getMScore());
        rfm.put("totalScore", p.getRScore() + p.getFScore() + p.getMScore());
        rfm.put("valueLevel", p.getValueLevel());
        dto.setRfm(rfm);

        // 活跃度
        Map<String, Object> activity = new LinkedHashMap<>();
        activity.put("rawScore", p.getActivityScore());
        activity.put("normalizedScore", p.getActivityNormalized());
        activity.put("activityLevel", p.getActivityLevel());
        activity.put("ratingCount", p.getPlayCount());  // 评分记录数 ≈ 游玩数
        dto.setActivity(activity);

        // 兴趣标签
        if (p.getInterestTags() != null && !p.getInterestTags().isEmpty()) {
            List<Map<String, Object>> tags = (List) JSON.parseArray(p.getInterestTags(), Map.class);
            dto.setInterestTags(tags);
        } else {
            dto.setInterestTags(new ArrayList<>());
        }

        // 玩家类型及统计
        dto.setPlayerType(p.getPlayerType());
        dto.setTotalPlayHours(p.getTotalPlayHours());
        dto.setAvgPlayHours(p.getAvgPlayHours());
        dto.setPurchaseCount(p.getPurchaseCount());
        dto.setPlayCount(p.getPlayCount());
        dto.setPurchasePlayRatio(p.getPurchasePlayRatio());

        return dto;
    }

    /**
     * 动态计算画像DTO（备选路径）
     */
    private UserProfileDTO buildDTOFromDynamic(Long userId) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setUserId(userId);

        // RFM
        dto.setRfm(calculateRFM(userId));

        // 活跃度
        dto.setActivity(calculateActivity(userId, 0.01));

        // 兴趣标签
        dto.setInterestTags(calculateInterestTags(userId));

        // 玩家类型
        dto.setPlayerType(classifyPlayerType(userId));

        // 转化率相关
        Map<String, Object> ratio = calculatePurchasePlayRatio(userId);
        dto.setTotalPlayHours((Double) ratio.get("totalPlayHours"));
        dto.setAvgPlayHours((Double) ratio.get("avgPlayHours"));
        dto.setPurchaseCount((Integer) ratio.get("purchaseCount"));
        dto.setPlayCount((Integer) ratio.get("playCount"));
        dto.setPurchasePlayRatio((Double) ratio.get("ratio"));

        return dto;
    }

    // ==================== 画像持久化 ====================

    /**
     * 保存单个用户画像
     *
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveProfile(Long userId) {
        UserProfileDTO dto = getFullProfile(userId);

        // 构建实体
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);

        // RFM
        Map<String, Object> rfm = dto.getRfm();
        profile.setRecency((Long) rfm.get("recency"));
        profile.setFrequency((Integer) rfm.get("frequency"));
        profile.setMonetary((Double) rfm.get("monetary"));
        profile.setRScore((Integer) rfm.get("rScore"));
        profile.setFScore((Integer) rfm.get("fScore"));
        profile.setMScore((Integer) rfm.get("mScore"));
        profile.setValueLevel((String) rfm.get("valueLevel"));

        // 活跃度
        Map<String, Object> activity = dto.getActivity();
        profile.setActivityScore((Double) activity.get("rawScore"));
        profile.setActivityNormalized((Double) activity.get("normalizedScore"));
        profile.setActivityLevel((String) activity.get("activityLevel"));

        // 兴趣标签
        profile.setInterestTags(JSON.toJSONString(dto.getInterestTags()));

        // 玩家类型
        profile.setPlayerType(dto.getPlayerType());
        profile.setTotalPlayHours(dto.getTotalPlayHours());
        profile.setAvgPlayHours(dto.getAvgPlayHours());
        profile.setPurchaseCount(dto.getPurchaseCount());
        profile.setPlayCount(dto.getPlayCount());
        profile.setPurchasePlayRatio(dto.getPurchasePlayRatio());

        // 检查是否存在
        QueryWrapper<UserProfile> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        UserProfile existing = userProfileMapper.selectOne(wrapper);

        if (existing != null) {
            profile.setId(existing.getId());
            userProfileMapper.updateById(profile);
        } else {
            userProfileMapper.insert(profile);
        }
    }

    /**
     * 批量保存所有用户画像
     *
     * @return 保存数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int saveAllProfiles() {
        // 从 user 表遍历系统用户ID（user.id），而非从 rating 表获取SteamID
        QueryWrapper<User> userWrapper = new QueryWrapper<>();
        userWrapper.select("id").orderByAsc("id");
        List<Map<String, Object>> userIds = userMapper.selectMaps(userWrapper);

        // ---- 预计算全局 RFM 统计数据（仅执行一次，而非 per-user 循环内 56,789 次） ----
        log.info("预计算全局 RFM 统计数据...");
        long preStart = System.currentTimeMillis();
        cachedAllRecencies = getAllRecencies();
        cachedAllFrequencies = getAllFrequencies();
        cachedAllMonetaries = getAllMonetaries();
        long preElapsed = System.currentTimeMillis() - preStart;
        log.info("全局 RFM 统计计算完成: R={}条, F={}条, M={}条, 耗时{}ms",
                cachedAllRecencies.size(), cachedAllFrequencies.size(),
                cachedAllMonetaries.size(), preElapsed);

        int count = 0;
        try {
            for (Map<String, Object> row : userIds) {
                Long userId = (Long) row.get("id");
                try {
                    saveProfile(userId);
                    count++;
                    if (count % 5000 == 0) {
                        log.info("画像保存进度: {} / {}", count, userIds.size());
                    }
                } catch (Exception e) {
                    log.error("保存用户{}画像失败: {}", userId, e.getMessage());
                }
            }
        } finally {
            // 清理缓存，避免影响后续单用户调用或并发请求
            cachedAllRecencies = null;
            cachedAllFrequencies = null;
            cachedAllMonetaries = null;
            cachedMaxActivityScore = null;
        }

        log.info("画像批量保存完成: {} 条", count);
        return count;
    }

    /**
     * 获取所有用户的画像列表（分页）
     */
    public List<UserProfile> listProfiles(int page, int size) {
        int offset = (page - 1) * size;
        QueryWrapper<UserProfile> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("update_time").last("LIMIT " + offset + "," + size);
        return userProfileMapper.selectList(wrapper);
    }

    /**
     * 获取画像总数
     */
    public long getProfileCount() {
        return userProfileMapper.selectCount(null);
    }
}
