package com.gamerec.gamerecommend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gamerec.gamerecommend.dto.Result;
import com.gamerec.gamerecommend.entity.Rating;
import com.gamerec.gamerecommend.mapper.RatingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 评分管理控制器
 *
 * @author GameRec Team
 */
@RestController
@RequestMapping("/api/admin/ratings")
public class RatingAdminController {

    @Autowired
    private RatingMapper ratingMapper;

    /**
     * 分页查询评分列表
     */
    @GetMapping
    public Result<Map<String, Object>> listRatings(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId) {
        QueryWrapper<Rating> wrapper = new QueryWrapper<>();
        wrapper.eq("is_valid", 1);
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        wrapper.orderByDesc("create_time");

        Page<Rating> pageResult = ratingMapper.selectPage(new Page<>(page, size), wrapper);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    /**
     * 删除评分（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteRating(@PathVariable Long id) {
        Rating rating = ratingMapper.selectById(id);
        if (rating == null) {
            return Result.error("评分记录不存在");
        }
        rating.setIsValid(0);
        ratingMapper.updateById(rating);
        return Result.success("删除成功");
    }

    /**
     * 获取评分统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getRatingStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalRatings", ratingMapper.selectCount(null));
        stats.put("validRatings", ratingMapper.selectCount(
                new QueryWrapper<Rating>().eq("is_valid", 1)));
        stats.put("invalidRatings", ratingMapper.selectCount(
                new QueryWrapper<Rating>().eq("is_valid", 0)));
        return Result.success(stats);
    }
}
