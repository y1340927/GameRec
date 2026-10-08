package com.gamerec.gamerecommend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gamerec.gamerecommend.dto.Result;
import com.gamerec.gamerecommend.entity.Rating;
import com.gamerec.gamerecommend.mapper.RatingMapper;
import com.gamerec.gamerecommend.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 推荐算法控制器
 *
 * 提供六种推荐算法的REST接口：
 *   - User-CF
 *   - Item-CF
 *   - Content-Based
 *   - Popularity（冷启动）
 *   - SVD
 *   - Hybrid（混合推荐）
 *
 * @author GameRec Team
 */
@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    @Autowired
    private UserCFService userCFService;

    @Autowired
    private ItemCFService itemCFService;

    @Autowired
    private ContentBasedService contentBasedService;

    @Autowired
    private PopularityService popularityService;

    @Autowired
    private SVDService svdService;

    @Autowired
    private HybridService hybridService;

    @Autowired
    private RatingMapper ratingMapper;

    /**
     * User-CF推荐
     */
    @GetMapping("/user-cf/{userId}")
    public Result<List<Map<String, Object>>> userCF(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int topK,
            @RequestParam(defaultValue = "10") int topN) {
        return Result.success(userCFService.recommend(userId, topK, topN));
    }

    /**
     * Item-CF推荐
     */
    @GetMapping("/item-cf/{userId}")
    public Result<List<Map<String, Object>>> itemCF(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int topN) {
        return Result.success(itemCFService.recommend(userId, topN));
    }

    /**
     * 构建Item-CF相似度矩阵
     */
    @PostMapping("/item-cf/build")
    public Result<Map<String, Object>> buildItemCF() {
        return Result.success(itemCFService.buildItemSimilarity());
    }

    /**
     * Content-Based推荐
     */
    @GetMapping("/content-based/{userId}")
    public Result<List<Map<String, Object>>> contentBased(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int topN) {
        return Result.success(contentBasedService.recommend(userId, topN));
    }

    /**
     * 流行度推荐（冷启动）
     */
    @GetMapping("/popular")
    public Result<List<Map<String, Object>>> popular(
            @RequestParam(defaultValue = "20") int topN) {
        return Result.success(popularityService.recommend(topN));
    }

    /**
     * SVD推荐
     */
    @GetMapping("/svd/{userId}")
    public Result<List<Map<String, Object>>> svd(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int topN) {
        return Result.success(svdService.recommend(userId, topN));
    }

    /**
     * 训练SVD模型
     */
    @PostMapping("/svd/train")
    public Result<Map<String, Object>> trainSVD(
            @RequestParam(defaultValue = "50") int factors,
            @RequestParam(defaultValue = "20") int iterations,
            @RequestParam(defaultValue = "0.005") double learningRate,
            @RequestParam(defaultValue = "0.02") double regularization) {
        return Result.success(svdService.train(factors, iterations, learningRate, regularization));
    }

    /**
     * 混合推荐
     */
    @GetMapping("/hybrid/{userId}")
    public Result<List<Map<String, Object>>> hybrid(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int topN) {
        return Result.success(hybridService.hybridRecommend(userId, topN));
    }

    /**
     * 统一推荐入口（自动判断冷启动/个性化）
     *
     * 新用户（无游玩记录）→ 流行度推荐
     * 老用户（有游玩记录）→ 混合推荐
     */
    @GetMapping("/{userId}")
    public Result<List<Map<String, Object>>> recommend(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int topN) {
        // 检查用户是否有评分记录
        long count = ratingMapper.selectCount(
                new QueryWrapper<Rating>().eq("user_id", userId).eq("is_valid", 1));

        if (count == 0) {
            // 冷启动：流行度推荐
            return Result.success(popularityService.recommend(topN));
        } else {
            // 个性化：混合推荐
            return Result.success(hybridService.hybridRecommend(userId, topN));
        }
    }
}
