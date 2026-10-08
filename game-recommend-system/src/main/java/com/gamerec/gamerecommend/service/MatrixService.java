package com.gamerec.gamerecommend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gamerec.gamerecommend.entity.Rating;
import com.gamerec.gamerecommend.entity.RatingTrain;
import com.gamerec.gamerecommend.entity.RatingTest;
import com.gamerec.gamerecommend.mapper.RatingMapper;
import com.gamerec.gamerecommend.mapper.RatingTrainMapper;
import com.gamerec.gamerecommend.mapper.RatingTestMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评分矩阵服务
 *
 * 数据结构：Map<Long, Map<Long, Integer>>
 *   外层key = userId
 *   内层key = gameId
 *   value = rating（伪评分1-5）
 *
 * 优势：查询O(1)、空间复杂度O(非零元素)
 *
 * @author GameRec Team
 */
@Slf4j
@Service
public class MatrixService {

    /** 全量评分 Mapper：仅用于训练/测试集划分时读取原始数据源 */
    @Autowired
    private RatingMapper ratingMapper;

    /**
     * 训练集 Mapper
     *
     * 【问题 2.2】训练阶段唯一数据源：只读取 rating_train，
     * 不再读取全量 rating，从而与 rating_test 物理隔离、杜绝数据泄露。
     */
    @Autowired
    private RatingTrainMapper ratingTrainMapper;

    /** 测试集 Mapper（仅用于训练/测试集划分落库） */
    @Autowired
    private RatingTestMapper ratingTestMapper;

    /** 评分矩阵：userId -> (gameId -> rating) */
    private Map<Long, Map<Long, Integer>> ratingMatrix;

    /** 反矩阵：gameId -> (userId -> rating)，用于Item-CF */
    private Map<Long, Map<Long, Integer>> invertedMatrix;

    /** 用户数量 */
    private int userCount;

    /** 游戏数量 */
    private int gameCount;

    /** 矩阵是否已构建 */
    private boolean built = false;

    /**
     * 构建评分矩阵（从rating_train表）
     *
     * @return 矩阵统计信息
     */
    @Transactional(readOnly = true)
    public Map<String, Object> buildMatrix() {
        long startTime = System.currentTimeMillis();

        // 【问题 2.2】只查询训练集的有效评分，严禁使用全量 rating
        QueryWrapper<RatingTrain> wrapper = new QueryWrapper<>();
        wrapper.eq("is_valid", 1);
        List<RatingTrain> ratings = ratingTrainMapper.selectList(wrapper);

        ratingMatrix = new HashMap<>();
        invertedMatrix = new HashMap<>();

        Set<Long> users = new HashSet<>();
        Set<Long> games = new HashSet<>();

        for (RatingTrain r : ratings) {
            Long userId = r.getUserId();
            Long gameId = r.getGameId();
            Integer rating = r.getRating();

            // 构建正矩阵
            ratingMatrix.computeIfAbsent(userId, k -> new HashMap<>()).put(gameId, rating);

            // 构建反矩阵
            invertedMatrix.computeIfAbsent(gameId, k -> new HashMap<>()).put(userId, rating);

            users.add(userId);
            games.add(gameId);
        }

        userCount = users.size();
        gameCount = games.size();
        built = true;

        long nonZero = ratings.size();
        long total = (long) userCount * gameCount;
        double sparsity = total > 0 ? (1.0 - (double) nonZero / total) * 100 : 100;

        long elapsed = System.currentTimeMillis() - startTime;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userCount", userCount);
        result.put("gameCount", gameCount);
        result.put("nonZeroEntries", nonZero);
        result.put("sparsity", String.format("%.2f%%", sparsity));
        result.put("elapsedMs", elapsed);

        log.info("评分矩阵构建完成: {}用户 × {}游戏 = {}个非零元素, 稀疏度={}%, 耗时{}ms",
                userCount, gameCount, nonZero, String.format("%.2f%%", sparsity), elapsed);

        return result;
    }

    /**
     * 划分训练集和测试集并物理落库
     *
     * 【问题 2.2 修复】
     * 旧实现仅计算数量、不落库，且按记录 id 顺序切前 80%/后 20%，
     * 既非随机划分，也未真正隔离训练与测试数据。
     *
     * 新实现：逐个用户独立随机划分（固定 seed=42 保证可复现），
     * 每个用户 80% 行为入 rating_train、20% 入 rating_test，
     * 并保证每用户测试集至少 1 条、训练集至少 1 条。
     * 与 scripts/split_train_test.py 逻辑一致，二者结果等价。
     *
     * @return 划分统计
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> splitTrainTest() {
        long startTime = System.currentTimeMillis();

        // 从全量 rating 读取原始数据源，按 (user_id, id) 排序保证分组顺序确定 → 可复现
        QueryWrapper<Rating> srcWrapper = new QueryWrapper<>();
        srcWrapper.eq("is_valid", 1).orderByAsc("user_id").orderByAsc("id");
        List<Rating> allRatings = ratingMapper.selectList(srcWrapper);

        int total = allRatings.size();

        // 按用户分组
        Map<Long, List<Rating>> userGroups = new LinkedHashMap<>();
        for (Rating r : allRatings) {
            userGroups.computeIfAbsent(r.getUserId(), k -> new ArrayList<>()).add(r);
        }

        List<RatingTrain> trainData = new ArrayList<>();
        List<RatingTest> testData = new ArrayList<>();
        Random rng = new Random(42);

        for (List<Rating> items : userGroups.values()) {
            List<Rating> shuffled = new ArrayList<>(items);
            Collections.shuffle(shuffled, rng);
            int nTest = Math.max(1, (int) Math.round(shuffled.size() * 0.2));
            nTest = Math.min(nTest, shuffled.size() - 1);

            for (int i = 0; i < shuffled.size(); i++) {
                Rating src = shuffled.get(i);
                if (i < nTest) {
                    RatingTest t = new RatingTest();
                    t.setUserId(src.getUserId());
                    t.setGameId(src.getGameId());
                    t.setGameName(src.getGameName());
                    t.setRating(src.getRating());
                    t.setPlayHours(src.getPlayHours());
                    t.setBehaviorTime(src.getBehaviorTime());
                    t.setIsValid(1);
                    testData.add(t);
                } else {
                    RatingTrain tr = new RatingTrain();
                    tr.setUserId(src.getUserId());
                    tr.setGameId(src.getGameId());
                    tr.setGameName(src.getGameName());
                    tr.setRating(src.getRating());
                    tr.setPlayHours(src.getPlayHours());
                    tr.setBehaviorTime(src.getBehaviorTime());
                    tr.setIsValid(1);
                    trainData.add(tr);
                }
            }
        }

        // 物理清空后重新写入
        ratingTrainMapper.truncate();
        ratingTestMapper.truncate();

        int trainBatch = 1000;
        for (int i = 0; i < trainData.size(); i += trainBatch) {
            ratingTrainMapper.insertBatch(trainData.subList(i, Math.min(i + trainBatch, trainData.size())));
        }
        for (int i = 0; i < testData.size(); i += trainBatch) {
            ratingTestMapper.insertBatch(testData.subList(i, Math.min(i + trainBatch, testData.size())));
        }

        // 重建矩阵
        built = false;
        buildMatrix();

        long elapsed = System.currentTimeMillis() - startTime;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("trainSize", trainData.size());
        result.put("testSize", testData.size());
        result.put("trainRatio", "80%");
        result.put("testRatio", "20%");
        result.put("seed", 42);
        result.put("elapsedMs", elapsed);

        log.info("训练/测试集划分完成: 总{}条, 训练{}条, 测试{}条",
                total, trainData.size(), testData.size());

        return result;
    }

    /**
     * 获取矩阵统计信息
     */
    public Map<String, Object> getMatrixStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("built", built);
        stats.put("userCount", userCount);
        stats.put("gameCount", gameCount);

        if (built && ratingMatrix != null) {
            long nonZero = ratingMatrix.values().stream().mapToLong(Map::size).sum();
            long total = (long) userCount * gameCount;
            double sparsity = total > 0 ? (1.0 - (double) nonZero / total) * 100 : 100;
            stats.put("nonZeroEntries", nonZero);
            stats.put("sparsity", String.format("%.2f%%", sparsity));
        }

        return stats;
    }

    /**
     * 获取用户的评分向量
     *
     * @param userId 用户ID
     * @return gameId -> rating
     */
    public Map<Long, Integer> getUserRatings(Long userId) {
        ensureBuilt();
        return ratingMatrix.getOrDefault(userId, Collections.emptyMap());
    }

    /**
     * 获取游戏的评分向量（所有用户对该游戏的评分）
     *
     * @param gameId 游戏ID
     * @return userId -> rating
     */
    public Map<Long, Integer> getGameRatings(Long gameId) {
        ensureBuilt();
        return invertedMatrix.getOrDefault(gameId, Collections.emptyMap());
    }

    /**
     * 获取评分矩阵
     */
    public Map<Long, Map<Long, Integer>> getRatingMatrix() {
        ensureBuilt();
        return ratingMatrix;
    }

    /**
     * 获取反矩阵
     */
    public Map<Long, Map<Long, Integer>> getInvertedMatrix() {
        ensureBuilt();
        return invertedMatrix;
    }

    public int getUserCount() {
        return userCount;
    }

    public int getGameCount() {
        return gameCount;
    }

    public boolean isBuilt() {
        return built;
    }

    private void ensureBuilt() {
        if (!built) {
            buildMatrix();
        }
    }
}
