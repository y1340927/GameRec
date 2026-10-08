package com.gamerec.gamerecommend.controller;

import com.gamerec.gamerecommend.dto.Result;
import com.gamerec.gamerecommend.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据分析控制器
 *
 * @author GameRec Team
 */
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    @Autowired
    private AnalysisService analysisService;

    /**
     * 综合概览
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        return Result.success(analysisService.getOverview());
    }

    /**
     * 伪评分分布
     */
    @GetMapping("/rating-distribution")
    public Result<Map<String, Object>> getRatingDistribution() {
        return Result.success(analysisService.getRatingDistribution());
    }

    /**
     * 最活跃玩家Top-N
     */
    @GetMapping("/top-active-users")
    public Result<List<Map<String, Object>>> getTopActiveUsers(
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(analysisService.getTopActiveUsers(limit));
    }

    /**
     * 热门游戏Top-N
     */
    @GetMapping("/top-hot-games")
    public Result<List<Map<String, Object>>> getTopHotGames(
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(analysisService.getTopHotGames(limit));
    }

    /**
     * 高分游戏Top-N
     */
    @GetMapping("/top-rated-games")
    public Result<List<Map<String, Object>>> getTopRatedGames(
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(analysisService.getTopRatedGames(limit));
    }

    /**
     * 游玩时长分布（创新）
     */
    @GetMapping("/play-hours-distribution")
    public Result<Map<String, Object>> getPlayHoursDistribution() {
        return Result.success(analysisService.getPlayHoursDistribution());
    }

    /**
     * 购买-游玩转化率分析（创新）
     */
    @GetMapping("/purchase-play-ratio")
    public Result<Map<String, Object>> getPurchasePlayRatio() {
        return Result.success(analysisService.getPurchasePlayRatio());
    }

    // ==================== 新增：游戏评价分析 ====================

    /**
     * 游戏评价分布分析
     */
    @GetMapping("/review-distribution")
    public Result<Map<String, Object>> getReviewDistribution() {
        return Result.success(analysisService.getReviewDistribution());
    }

    /**
     * 最高评分游戏（Metacritic）
     */
    @GetMapping("/top-critic-games")
    public Result<List<Map<String, Object>>> getTopCriticGames(
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(analysisService.getTopCriticGames(limit));
    }

    /**
     * 最多评价游戏
     */
    @GetMapping("/top-reviewed-games")
    public Result<List<Map<String, Object>>> getTopReviewedGames(
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(analysisService.getTopReviewedGames(limit));
    }

    // ==================== 新增：开发商/发行商分析 ====================

    /**
     * 顶级开发商
     */
    @GetMapping("/top-developers")
    public Result<List<Map<String, Object>>> getTopDevelopers(
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(analysisService.getTopDevelopers(limit));
    }

    /**
     * 顶级发行商
     */
    @GetMapping("/top-publishers")
    public Result<List<Map<String, Object>>> getTopPublishers(
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(analysisService.getTopPublishers(limit));
    }

    /**
     * 平台分布
     */
    @GetMapping("/platform-distribution")
    public Result<Map<String, Object>> getPlatformDistribution() {
        return Result.success(analysisService.getPlatformDistribution());
    }

    /**
     * 价格分布
     */
    @GetMapping("/price-distribution")
    public Result<Map<String, Object>> getPriceDistribution() {
        return Result.success(analysisService.getPriceDistribution());
    }

    // ==================== 新增：词云与标签分布 ====================

    /**
     * 游戏标签词云数据（基于 steamspy_tags_cn、genres_cn 等）
     */
    @GetMapping("/tag-cloud")
    public Result<List<Map<String, Object>>> getTagCloud() {
        return Result.success(analysisService.getTagCloudData());
    }

    // ==================== 新增：数据质量指标 ====================

    /**
     * 数据质量雷达图指标（6 维度 0-100）
     */
    @GetMapping("/radar-metrics")
    public Result<Map<String, Object>> getRadarMetrics() {
        return Result.success(analysisService.getRadarMetrics());
    }

    /**
     * 按发行年份聚合的趋势数据（替代时间序列）
     */
    @GetMapping("/release-year-trends")
    public Result<Map<String, Object>> getReleaseYearTrends(
            @RequestParam(defaultValue = "20") int periods) {
        return Result.success(analysisService.getReleaseYearTrends(periods));
    }

    /**
     * 数据质量监控统计
     */
    @GetMapping("/quality-stats")
    public Result<Map<String, Object>> getQualityStats() {
        return Result.success(analysisService.getQualityStats());
    }

    /**
     * KPI 迷你趋势（4 组 × N 点，供首页使用）
     */
    @GetMapping("/kpi-trends")
    public Result<Map<String, Object>> getKpiTrends(
            @RequestParam(defaultValue = "20") int points) {
        return Result.success(analysisService.getKpiTrends(points));
    }

    /**
     * 游戏类型发展走势（6 类型 × N 点，供分析页类型卡片使用）
     */
    @GetMapping("/genre-trends")
    public Result<Map<String, Object>> getGenreTrends(
            @RequestParam(defaultValue = "30") int points) {
        return Result.success(analysisService.getGenreTrends(points));
    }
}
