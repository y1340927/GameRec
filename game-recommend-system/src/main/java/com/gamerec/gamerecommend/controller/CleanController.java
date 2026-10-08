package com.gamerec.gamerecommend.controller;

import com.gamerec.gamerecommend.dto.Result;
import com.gamerec.gamerecommend.service.DataCleanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据清洗控制器
 *
 * 提供REST接口用于数据清洗操作：
 *   - POST /api/clean/ratings  → 执行完整清洗
 *   - GET  /api/clean/stats    → 获取清洗统计
 *
 * @author GameRec Team
 */
@RestController
@RequestMapping("/api/clean")
public class CleanController {

    @Autowired
    private DataCleanService dataCleanService;

    /**
     * 执行完整数据清洗
     *
     * 处理流程：缺失值 → 异常值 → 重复值
     * 清洗策略：逻辑删除（is_valid=0），保留原始数据
     *
     * @return 清洗统计结果
     */
    @PostMapping("/ratings")
    public Result<Map<String, Object>> cleanRatings() {
        return Result.success(dataCleanService.cleanRatings());
    }

    /**
     * 获取清洗统计（不执行清洗）
     *
     * @return 当前数据状态统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getCleanStats() {
        return Result.success(dataCleanService.getCleanStats());
    }
}
