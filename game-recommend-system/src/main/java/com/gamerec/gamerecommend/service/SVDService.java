package com.gamerec.gamerecommend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gamerec.gamerecommend.entity.Game;
import com.gamerec.gamerecommend.entity.RatingTrain;
import com.gamerec.gamerecommend.mapper.GameMapper;
import com.gamerec.gamerecommend.mapper.RatingTrainMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SVD 矩阵分解推荐服务（优化版）
 *
 * 将评分矩阵R分解为用户隐向量矩阵P和物品隐向量矩阵Q：
 *   pred(r) = μ + b_u + b_i + p_u · q_i
 *
 * 优化点：
 *   1. 索引数组替代 HashMap（内层循环 O(1) 无 hash 开销）
 *   2. 训练数据预编码为 int[] 三元组，避免每次迭代的装箱/拆箱
 *   3. Early Stopping：连续 3 轮 loss 降幅 < 0.001 提前终止
 *   4. 模型持久化：训练完成后序列化到文件，重启后自动加载
 *
 * 超参数：
 *   k=50（隐因子数量）
 *   lr=0.005（学习率）
 *   reg=0.02（正则化系数）
 *   epochs=20（迭代次数）
 *
 * @author GameRec Team
 */
@Slf4j
@Service
public class SVDService {

    @Autowired
    /** 【问题 2.2】训练阶段只读训练集 */
    private RatingTrainMapper ratingTrainMapper;

    @Autowired
    private GameMapper gameMapper;

    // ============ 索引映射 ============
    /** userId → 矩阵行索引 */
    private Map<Long, Integer> userIndexMap;
    /** gameId → 矩阵列索引 */
    private Map<Long, Integer> gameIndexMap;
    /** 索引 → userId */
    private Long[] indexToUserId;
    /** 索引 → gameId */
    private Long[] indexToGameId;

    // ============ 模型参数（数组形式，O(1) 访问） ============
    /** 用户隐向量矩阵 [userCount][factors] */
    private double[][] userFactors;
    /** 物品隐向量矩阵 [gameCount][factors] */
    private double[][] itemFactors;
    /** 用户偏置 */
    private double[] userBiases;
    /** 物品偏置 */
    private double[] itemBiases;

    /** 全局平均评分 */
    private double globalMean;

    /** 用户/物品总数 */
    private int userCount;
    private int gameCount;

    /** 模型是否已训练 */
    private boolean trained = false;

    /** 模型文件路径 */
    private static final String MODEL_FILE = "svd_model.dat";

    // ============ 训练数据预编码 ============
    /**
     * 预编码的训练数据：每行 [userIdx, gameIdx, rating]
     * 避免每次 epoch 在 Rating 对象和 Map 查询之间反复开销
     */
    private int[][] trainingTuples;

    /**
     * 训练SVD模型
     *
     * @param factors        隐因子数量k
     * @param iterations     迭代次数
     * @param learningRate   学习率
     * @param regularization 正则化系数
     * @return 训练统计
     */
    public Map<String, Object> train(int factors, int iterations,
                                      double learningRate, double regularization) {
        long startTime = System.currentTimeMillis();

        // 尝试从文件加载已训练模型
        if (loadModel()) {
            trained = true;
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "success");
            result.put("loadedFromFile", true);
            result.put("factors", factors);
            result.put("userCount", userCount);
            result.put("gameCount", gameCount);
            result.put("elapsedMs", 0);
            log.info("SVD模型从文件加载完成: {}用户 × {}游戏", userCount, gameCount);
            return result;
        }

        // 查询所有有效评分作为训练数据
        QueryWrapper<RatingTrain> wrapper = new QueryWrapper<>();
        wrapper.eq("is_valid", 1);
        List<RatingTrain> allRatings = ratingTrainMapper.selectList(wrapper);

        if (allRatings.isEmpty()) {
            log.warn("无训练数据");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "failed");
            result.put("message", "无训练数据");
            return result;
        }

        // ---- 步骤1：建立 ID → 索引 映射 ----
        buildIndexMaps(allRatings);
        int nUsers = userCount;
        int nGames = gameCount;

        // 计算全局平均评分
        globalMean = allRatings.stream().mapToInt(RatingTrain::getRating).average().orElse(3.0);

        // ---- 步骤2：初始化模型参数（数组形式） ----
        Random random = new Random(42);
        userFactors = new double[nUsers][factors];
        itemFactors = new double[nGames][factors];
        userBiases = new double[nUsers];
        itemBiases = new double[nGames];

        for (int u = 0; u < nUsers; u++) {
            double[] vec = userFactors[u];
            for (int f = 0; f < factors; f++) {
                vec[f] = (random.nextDouble() - 0.5) * 0.1;
            }
        }
        for (int g = 0; g < nGames; g++) {
            double[] vec = itemFactors[g];
            for (int f = 0; f < factors; f++) {
                vec[f] = (random.nextDouble() - 0.5) * 0.1;
            }
        }

        // ---- 步骤3：预编码训练数据为 int[] 三元组 ----
        trainingTuples = new int[allRatings.size()][3];
        for (int i = 0; i < allRatings.size(); i++) {
            RatingTrain r = allRatings.get(i);
            trainingTuples[i][0] = userIndexMap.get(r.getUserId());   // userIdx
            trainingTuples[i][1] = gameIndexMap.get(r.getGameId());   // gameIdx
            trainingTuples[i][2] = r.getRating();                     // rating
        }

        // 释放原始 Rating List（已编码为 int[]，不再需要）
        allRatings = null;

        // ---- 步骤4：SGD 训练 + Early Stopping ----
        List<Double> losses = new ArrayList<>();
        int noImproveCount = 0;
        final double EARLY_STOP_THRESHOLD = 0.001;
        final int EARLY_STOP_PATIENCE = 3;
        int actualEpochs = iterations;

        for (int epoch = 0; epoch < iterations; epoch++) {
            // shuffle 训练数据（对 int[][] 操作远比 List<Rating> 轻量）
            shuffleTuples(trainingTuples, random);

            double totalLoss = 0;
            for (int[] tuple : trainingTuples) {
                int uIdx = tuple[0];
                int gIdx = tuple[1];
                int rating = tuple[2];
                // 数组 O(1) 访问，无 Map.get() 开销
                double[] pu = userFactors[uIdx];
                double[] qi = itemFactors[gIdx];
                double bu = userBiases[uIdx];
                double bi = itemBiases[gIdx];

                // 预测：pred = mu + bu + bi + dot(pu, qi)
                double dot = 0;
                for (int f = 0; f < factors; f++) {
                    dot += pu[f] * qi[f];
                }
                double pred = globalMean + bu + bi + dot;
                if (pred < 0.5) pred = 0.5;
                if (pred > 5.0) pred = 5.0;

                double error = rating - pred;
                totalLoss += error * error;

                // 梯度更新
                userBiases[uIdx] += learningRate * (error - regularization * bu);
                itemBiases[gIdx] += learningRate * (error - regularization * bi);

                for (int f = 0; f < factors; f++) {
                    double puf = pu[f];
                    double qif = qi[f];
                    pu[f] += learningRate * (error * qif - regularization * puf);
                    qi[f] += learningRate * (error * puf - regularization * qif);
                }
            }

            double avgLoss = totalLoss / trainingTuples.length;
            losses.add(avgLoss);

            // 日志 — 每轮都输出
            log.info("SVD Epoch {}/{}: avg loss = {}", epoch + 1, iterations, String.format("%.4f", avgLoss));

            // ---- Early Stopping 检查 ----
            if (epoch > 0) {
                double improvement = losses.get(epoch - 1) - avgLoss;
                if (improvement < EARLY_STOP_THRESHOLD) {
                    noImproveCount++;
                    if (noImproveCount >= EARLY_STOP_PATIENCE) {
                        actualEpochs = epoch + 1;
                        log.info("SVD Early Stopping at epoch {}: loss improvement={} < {} for {} consecutive epochs",
                                actualEpochs, String.format("%.6f", improvement), EARLY_STOP_THRESHOLD, EARLY_STOP_PATIENCE);
                        break;
                    }
                } else {
                    noImproveCount = 0;
                }
            }
        }

        trained = true;
        long elapsed = System.currentTimeMillis() - startTime;

        // 保存模型到文件
        saveModel();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "success");
        result.put("factors", factors);
        result.put("iterations", actualEpochs);
        result.put("maxIterations", iterations);
        result.put("userCount", nUsers);
        result.put("gameCount", nGames);
        result.put("finalLoss", Math.round(losses.get(losses.size() - 1) * 10000.0) / 10000.0);
        result.put("elapsedMs", elapsed);
        result.put("earlyStopped", actualEpochs < iterations);

        log.info("SVD训练完成: {}用户 × {}游戏, k={}, epochs={}{}, finalLoss={}, 耗时{}ms",
                nUsers, nGames, factors, actualEpochs,
                actualEpochs < iterations ? " (提前终止)" : "",
                result.get("finalLoss"), elapsed);

        return result;
    }

    /**
     * 从评分列表构建 ID↔索引 双向映射
     */
    private void buildIndexMaps(List<RatingTrain> ratings) {
        Set<Long> userIdSet = new LinkedHashSet<>();
        Set<Long> gameIdSet = new LinkedHashSet<>();
        for (RatingTrain r : ratings) {
            userIdSet.add(r.getUserId());
            gameIdSet.add(r.getGameId());
        }

        userCount = userIdSet.size();
        gameCount = gameIdSet.size();

        userIndexMap = new HashMap<>(userCount);
        gameIndexMap = new HashMap<>(gameCount);
        indexToUserId = new Long[userCount];
        indexToGameId = new Long[gameCount];

        int idx = 0;
        for (Long uid : userIdSet) {
            userIndexMap.put(uid, idx);
            indexToUserId[idx] = uid;
            idx++;
        }
        idx = 0;
        for (Long gid : gameIdSet) {
            gameIndexMap.put(gid, idx);
            indexToGameId[idx] = gid;
            idx++;
        }
    }

    /**
     * Fisher-Yates shuffle for int[][] arrays
     */
    private void shuffleTuples(int[][] tuples, Random rnd) {
        for (int i = tuples.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int[] tmp = tuples[i];
            tuples[i] = tuples[j];
            tuples[j] = tmp;
        }
    }

    /**
     * 预测评分（使用索引数组快速访问）
     *
     * @param userId 用户SteamID
     * @param gameId 游戏ID
     * @return 预测评分
     */
    public double predict(Long userId, Long gameId) {
        if (!trained) return 2.5;

        Integer uIdx = userIndexMap.get(userId);
        Integer gIdx = gameIndexMap.get(gameId);
        if (uIdx == null || gIdx == null) return 2.5;

        double[] pu = userFactors[uIdx];
        double[] qi = itemFactors[gIdx];
        double bu = userBiases[uIdx];
        double bi = itemBiases[gIdx];

        double dot = 0;
        for (int f = 0; f < pu.length; f++) {
            dot += pu[f] * qi[f];
        }

        double pred = globalMean + bu + bi + dot;
        if (pred < 0.5) pred = 0.5;
        if (pred > 5.0) pred = 5.0;
        return pred;
    }

    /**
     * SVD推荐（优化版）
     *
     * @param userId 目标用户ID
     * @param topN   推荐结果数
     * @return 推荐游戏列表
     */
    public List<Map<String, Object>> recommend(Long userId, int topN) {
        long startTime = System.currentTimeMillis();

        if (!trained) {
            log.warn("SVD模型未训练");
            return Collections.emptyList();
        }

        Integer uIdx = userIndexMap.get(userId);
        if (uIdx == null) {
            log.warn("用户{}不在训练数据中", userId);
            return Collections.emptyList();
        }

        // 获取用户已玩过的游戏ID集合（训练集）
        QueryWrapper<RatingTrain> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("is_valid", 1);
        Set<Long> playedGames = ratingTrainMapper.selectList(wrapper).stream()
                .map(RatingTrain::getGameId)
                .collect(Collectors.toSet());

        // 使用优先队列（最小堆）维护 Top-N，避免全排序
        // 堆中元素为 [predictedRating, gameId]
        PriorityQueue<double[]> heap = new PriorityQueue<>(
                (a, b) -> Double.compare(a[0], b[0]));

        for (int gIdx = 0; gIdx < gameCount; gIdx++) {
            Long gid = indexToGameId[gIdx];
            if (playedGames.contains(gid)) continue;

            double pred = globalMean + userBiases[uIdx] + itemBiases[gIdx];
            double dot = 0;
            double[] pu = userFactors[uIdx];
            double[] qi = itemFactors[gIdx];
            for (int f = 0; f < pu.length; f++) {
                dot += pu[f] * qi[f];
            }
            pred += dot;
            if (pred < 0.5) pred = 0.5;
            if (pred > 5.0) pred = 5.0;

            heap.offer(new double[]{pred, gid});
            if (heap.size() > topN) {
                heap.poll(); // 移除最小的
            }
        }

        // 收集结果（从堆中降序取出）
        List<Map<String, Object>> results = new ArrayList<>(topN);
        while (!heap.isEmpty()) {
            double[] entry = heap.poll();
            Long gid = (long) entry[1];
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("gameId", gid);
            item.put("predictedRating", Math.round(entry[0] * 100.0) / 100.0);

            Game game = gameMapper.selectOne(
                    new QueryWrapper<Game>().eq("game_id", gid));
            if (game != null) {
                item.put("gameName", game.getGameName());
                item.put("gameNameCn", game.getGameNameCn());
                item.put("genres", game.getGenres());
            }
            results.add(item);
        }
        // 逆序（堆中是从小到大，需反转）
        Collections.reverse(results);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("SVD推荐完成: userId={}, topN={}, 结果{}条, 耗时{}ms",
                userId, topN, results.size(), elapsed);

        return results;
    }

    // ============ 模型持久化 ============

    /**
     * 保存模型到文件
     */
    private void saveModel() {
        if (!trained) return;
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(MODEL_FILE)))) {
            oos.writeObject(userIndexMap);
            oos.writeObject(gameIndexMap);
            oos.writeObject(indexToUserId);
            oos.writeObject(indexToGameId);
            oos.writeObject(userFactors);
            oos.writeObject(itemFactors);
            oos.writeObject(userBiases);
            oos.writeObject(itemBiases);
            oos.writeDouble(globalMean);
            oos.writeInt(userCount);
            oos.writeInt(gameCount);
            log.info("SVD模型已保存到文件: {}", MODEL_FILE);
        } catch (IOException e) {
            log.warn("SVD模型保存失败: {}", e.getMessage());
        }
    }

    /**
     * 从文件加载模型
     *
     * @return 是否成功加载
     */
    @SuppressWarnings("unchecked")
    private boolean loadModel() {
        File file = new File(MODEL_FILE);
        if (!file.exists()) return false;

        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            userIndexMap = (Map<Long, Integer>) ois.readObject();
            gameIndexMap = (Map<Long, Integer>) ois.readObject();
            indexToUserId = (Long[]) ois.readObject();
            indexToGameId = (Long[]) ois.readObject();
            userFactors = (double[][]) ois.readObject();
            itemFactors = (double[][]) ois.readObject();
            userBiases = (double[]) ois.readObject();
            itemBiases = (double[]) ois.readObject();
            globalMean = ois.readDouble();
            userCount = ois.readInt();
            gameCount = ois.readInt();
            trained = true;
            return true;
        } catch (IOException | ClassNotFoundException e) {
            log.warn("SVD模型加载失败，将重新训练: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 清空模型（触发重新训练）
     */
    public void reset() {
        trained = false;
        userFactors = null;
        itemFactors = null;
        userBiases = null;
        itemBiases = null;
        userIndexMap = null;
        gameIndexMap = null;
        trainingTuples = null;
        File file = new File(MODEL_FILE);
        if (file.exists()) {
            file.delete();
        }
        log.info("SVD模型已重置");
    }

    public boolean isTrained() {
        return trained;
    }
}
