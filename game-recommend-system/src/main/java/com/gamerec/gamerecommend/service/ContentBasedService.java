package com.gamerec.gamerecommend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gamerec.gamerecommend.entity.Game;
import com.gamerec.gamerecommend.entity.RatingTrain;
import com.gamerec.gamerecommend.mapper.GameMapper;
import com.gamerec.gamerecommend.mapper.RatingTrainMapper;
import com.gamerec.gamerecommend.util.SimilarityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Content-Based 基于内容的推荐服务
 *
 * 核心思想："推荐与用户过去喜欢的游戏内容相似的物品"
 *
 * 基于游戏名称关键词构建特征向量，通过向量空间模型（VSM）
 * 计算用户偏好向量与候选游戏向量的余弦相似度。
 *
 * @author GameRec Team
 */
@Slf4j
@Service
public class ContentBasedService {

    /** 【问题 2.2】训练阶段只读训练集 */
    @Autowired
    private RatingTrainMapper ratingTrainMapper;

    @Autowired
    private GameMapper gameMapper;

    /**
     * 将游戏名称转化为特征向量（基于游戏类型关键词）
     *
     * @param genres 游戏类型（分号分隔）
     * @return 特征向量 Map<特征词, 1.0>
     */
    private Map<String, Double> gameToVector(String genres) {
        Map<String, Double> vector = new HashMap<>();
        if (genres == null || genres.isEmpty()) return vector;

        String[] parts = genres.split(";");
        for (String part : parts) {
            String keyword = part.trim().toLowerCase();
            if (!keyword.isEmpty()) {
                vector.put(keyword, 1.0);
            }
        }
        return vector;
    }

    /**
     * 计算用户偏好向量
     *
     * 用户游玩过的所有游戏向量的加权平均（权重=伪评分）
     *
     * @param userId 用户ID
     * @return 偏好向量
     */
    private Map<String, Double> getUserPreferenceVector(Long userId) {
        QueryWrapper<RatingTrain> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("is_valid", 1);
        List<RatingTrain> ratings = ratingTrainMapper.selectList(wrapper);

        Map<String, Double> preferenceVector = new HashMap<>();
        double totalWeight = 0;

        for (RatingTrain r : ratings) {
            Game game = gameMapper.selectOne(
                    new QueryWrapper<Game>().eq("game_id", r.getGameId()));
            if (game == null || game.getGenres() == null) continue;

            Map<String, Double> gameVector = gameToVector(game.getGenres());
            double weight = r.getRating(); // 伪评分作为权重

            for (Map.Entry<String, Double> entry : gameVector.entrySet()) {
                preferenceVector.merge(entry.getKey(), entry.getValue() * weight, Double::sum);
            }
            totalWeight += weight;
        }

        // 归一化：除以总权重
        if (totalWeight > 0) {
            for (String key : preferenceVector.keySet()) {
                preferenceVector.put(key, preferenceVector.get(key) / totalWeight);
            }
        }

        return preferenceVector;
    }

    /**
     * Content-Based推荐
     *
     * @param userId 目标用户ID
     * @param topN   推荐结果数
     * @return 推荐游戏列表
     */
    public List<Map<String, Object>> recommend(Long userId, int topN) {
        long startTime = System.currentTimeMillis();

        // 1. 计算用户偏好向量
        Map<String, Double> userPreference = getUserPreferenceVector(userId);
        if (userPreference.isEmpty()) {
            log.warn("用户{}无偏好向量，无法进行CB推荐", userId);
            return Collections.emptyList();
        }

        // 2. 获取用户已玩过的游戏
        QueryWrapper<RatingTrain> ratingWrapper = new QueryWrapper<>();
        ratingWrapper.eq("user_id", userId).eq("is_valid", 1);
        Set<Long> playedGames = ratingTrainMapper.selectList(ratingWrapper).stream()
                .map(RatingTrain::getGameId)
                .collect(Collectors.toSet());

        // 3. 遍历所有未玩过的游戏，计算与偏好向量的相似度
        List<Game> allGames = gameMapper.selectList(null);
        Map<Long, Double> candidateScores = new HashMap<>();

        for (Game game : allGames) {
            if (playedGames.contains(game.getGameId())) continue;
            if (game.getGenres() == null || game.getGenres().isEmpty()) continue;

            Map<String, Double> gameVector = gameToVector(game.getGenres());
            if (gameVector.isEmpty()) continue;

            double similarity = SimilarityUtil.cosineSimilarityStr(userPreference, gameVector);
            if (similarity > 0) {
                candidateScores.put(game.getGameId(), similarity);
            }
        }

        // 4. 按相似度降序，取Top-N
        List<Map<String, Object>> results = candidateScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("gameId", entry.getKey());
                    item.put("score", Math.round(entry.getValue() * 10000.0) / 10000.0);

                    Game game = gameMapper.selectOne(new QueryWrapper<Game>().eq("game_id", entry.getKey()));
                    if (game != null) {
                        item.put("gameName", game.getGameName());
                        item.put("gameNameCn", game.getGameNameCn());
                        item.put("genres", game.getGenres());
                    }
                    return item;
                })
                .collect(Collectors.toList());

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Content-Based推荐完成: userId={}, topN={}, 结果{}条, 耗时{}ms",
                userId, topN, results.size(), elapsed);

        return results;
    }
}
