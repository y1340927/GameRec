package com.gamerec.gamerecommend.controller;

import com.gamerec.gamerecommend.dto.*;
import com.gamerec.gamerecommend.entity.DataImportRecord;
import com.gamerec.gamerecommend.service.DataImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据导入控制器
 *
 * 提供数据导入的REST接口，支持：
 *   - 用户数据导入
 *   - 游戏数据导入
 *   - 评分数据导入（含自动联动更新画像）
 *
 * @author GameRec Team
 */
@RestController
@RequestMapping("/api/data-import")
public class DataImportController {

    @Autowired
    private DataImportService dataImportService;

    /**
     * 导入用户数据
     */
    @PostMapping("/users")
    public Result<Map<String, Object>> importUsers(
            @RequestBody List<UserImportDTO> users) {
        return Result.success(dataImportService.importUsers(users));
    }

    /**
     * 导入游戏数据
     */
    @PostMapping("/games")
    public Result<Map<String, Object>> importGames(
            @RequestBody List<GameImportDTO> games) {
        return Result.success(dataImportService.importGames(games));
    }

    /**
     * 导入评分数据
     */
    @PostMapping("/ratings")
    public Result<Map<String, Object>> importRatings(
            @RequestBody List<RatingImportDTO> ratings) {
        return Result.success(dataImportService.importRatings(ratings));
    }

    /**
     * 获取导入记录列表
     */
    @GetMapping("/records")
    public Result<List<DataImportRecord>> getImportRecords() {
        return Result.success(dataImportService.getImportRecords());
    }
}
