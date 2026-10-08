package com.gamerec.gamerecommend.controller;

import com.gamerec.gamerecommend.dto.Result;
import com.gamerec.gamerecommend.entity.EvaluationResult;
import com.gamerec.gamerecommend.service.EvaluateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 推荐算法评估控制器
 *
 * @author GameRec Team
 */
@RestController
@RequestMapping("/api/evaluate")
public class EvaluateController {

    @Autowired
    private EvaluateService evaluateService;

    /**
     * 评估所有算法（指定K值）
     */
    @PostMapping("/all")
    public Result<List<Map<String, Object>>> evaluateAll(
            @RequestParam(defaultValue = "10") int topK) {
        return Result.success(evaluateService.evaluateAll(topK));
    }

    /**
     * 批量评估多个K值
     */
    @PostMapping("/multiple-k")
    public Result<Map<String, Object>> evaluateMultipleK(
            @RequestBody(required = false) int[] topKs) {
        if (topKs == null || topKs.length == 0) {
            topKs = new int[]{5, 10, 15, 20};
        }
        return Result.success(evaluateService.evaluateMultipleK(topKs));
    }

    /**
     * 获取历史评估结果
     */
    @GetMapping("/history")
    public Result<List<EvaluationResult>> getHistory() {
        return Result.success(evaluateService.getHistoryResults());
    }

    /**
     * 获取指定算法的评估结果
     */
    @GetMapping("/algorithm/{algorithmName}")
    public Result<List<EvaluationResult>> getByAlgorithm(
            @PathVariable String algorithmName) {
        return Result.success(evaluateService.getResultsByAlgorithm(algorithmName));
    }
}
