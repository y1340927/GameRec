package com.gamerec.gamerecommend.controller;

import com.gamerec.gamerecommend.dto.Result;
import com.gamerec.gamerecommend.service.MatrixService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 评分矩阵控制器
 *
 * @author GameRec Team
 */
@RestController
@RequestMapping("/api/matrix")
public class MatrixController {

    @Autowired
    private MatrixService matrixService;

    /**
     * 构建评分矩阵
     */
    @PostMapping("/build")
    public Result<Map<String, Object>> buildMatrix() {
        return Result.success(matrixService.buildMatrix());
    }

    /**
     * 划分训练/测试集
     */
    @PostMapping("/split")
    public Result<Map<String, Object>> splitTrainTest() {
        return Result.success(matrixService.splitTrainTest());
    }

    /**
     * 获取矩阵统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getMatrixStats() {
        return Result.success(matrixService.getMatrixStats());
    }
}
