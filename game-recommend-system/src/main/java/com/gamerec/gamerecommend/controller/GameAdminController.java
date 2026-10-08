package com.gamerec.gamerecommend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gamerec.gamerecommend.dto.Result;
import com.gamerec.gamerecommend.entity.Game;
import com.gamerec.gamerecommend.mapper.GameMapper;
import com.gamerec.gamerecommend.mapper.RatingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 游戏管理控制器
 *
 * @author GameRec Team
 */
@RestController
@RequestMapping("/api/admin/games")
public class GameAdminController {

    @Autowired
    private GameMapper gameMapper;

    @Autowired
    private RatingMapper ratingMapper;

    /**
     * 转义 LIKE 查询中的特殊字符
     * 防止 % 和 _ 被 SQL LIKE 解释为通配符
     */
    private String escapeLikeKeyword(String keyword) {
        if (keyword == null || keyword.isEmpty()) return keyword;
        // 注意转义顺序：先转义反斜杠自身，再转义 % 和 _
        // 利用 MySQL 默认转义字符 \ 实现
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * 分页查询游戏列表（智能搜索）
     * 支持：特殊字符自动转义、多字段模糊匹配、前端排序参数、多标签组合筛选
     */
    @GetMapping
    public Result<Map<String, Object>> listGames(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String tags) {
        QueryWrapper<Game> wrapper = new QueryWrapper<>();

        // 多标签组合筛选（用逗号分隔，支持 genres_cn/steamspy_tags_cn 匹配）
        // 多标签之间为 AND 关系，即游戏必须匹配所有选中的标签
        if (tags != null && !tags.isEmpty()) {
            String[] tagList = tags.split(",");
            for (String tag : tagList) {
                String t = tag.trim();
                if (t.isEmpty()) continue;
                // 每个标签在 genres_cn 或 steamspy_tags_cn 或 categories_cn 中匹配
                // AND 条件：所有标签都必须匹配
                wrapper.and(sub -> sub
                        .like("genres_cn", t)
                        .or().like("steamspy_tags_cn", t)
                        .or().like("categories_cn", t));
            }
        }
        if (keyword != null && !keyword.isEmpty()) {
            // 转义 LIKE 通配符，避免 CS:GO 等含特殊字符的搜索异常
            String escaped = escapeLikeKeyword(keyword);
            // 多字段智能模糊匹配：游戏名（中英文）、标签中文、类型中文、开发商、发行商、别名
            wrapper.and(w -> w
                    .like("game_name", escaped)
                    .or().like("game_name_cn", escaped)
                    .or().like("steamspy_tags_cn", escaped)
                    .or().like("genres_cn", escaped)
                    .or().like("developer", escaped)
                    .or().like("publisher", escaped)
                    // 别名匹配：game_alias 表（支持 CS:GO、GTA、EU4 等缩写搜索）
                    .or().apply("EXISTS (SELECT 1 FROM game_alias a WHERE a.game_id = game.game_id AND a.alias LIKE {0})",
                            "%" + escaped + "%")
            );
        }

        // 排序处理：支持前端传入排序参数
        if (sortBy != null && !sortBy.isEmpty()) {
            switch (sortBy) {
                case "name": wrapper.orderByAsc("game_name"); break;
                case "rating": wrapper.orderByDesc("positive_ratings"); break;
                case "price": wrapper.orderByAsc("price"); break;
                case "releaseDate": wrapper.orderByDesc("release_date"); break;
                case "totalReviews": wrapper.orderByDesc("total_reviews"); break;
                default: wrapper.orderByDesc("total_reviews"); break;
            }
        } else {
            wrapper.orderByDesc("total_reviews");
        }

        Page<Game> pageResult = gameMapper.selectPage(new Page<>(page, size), wrapper);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    /**
     * 搜索建议接口：快速返回前 8 条匹配结果，用于前端输入提示
     */
    @GetMapping("/suggestions")
    public Result<List<Map<String, Object>>> getSuggestions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "8") int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.success(List.of());
        }
        String escaped = escapeLikeKeyword(keyword.trim());
        QueryWrapper<Game> wrapper = new QueryWrapper<>();
        wrapper.and(w -> w
                .like("game_name", escaped)
                .or().like("game_name_cn", escaped)
                // 别名匹配
                .or().apply("EXISTS (SELECT 1 FROM game_alias a WHERE a.game_id = game.game_id AND a.alias LIKE {0})",
                        "%" + escaped + "%")
        );
        wrapper.orderByDesc("total_reviews");
        wrapper.last("LIMIT " + limit);

        List<Game> games = gameMapper.selectList(wrapper);

        List<Map<String, Object>> suggestions = games.stream().map(g -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("gameId", g.getGameId());
            item.put("gameName", g.getGameName());
            item.put("gameNameCn", g.getGameNameCn());
            item.put("genres", g.getGenres());
            item.put("genresCn", g.getGenresCn());
            if (g.getTotalReviews() != null) {
                item.put("totalReviews", g.getTotalReviews());
            }
            return item;
        }).collect(Collectors.toList());

        return Result.success(suggestions);
    }

    /**
     * 根据ID查询游戏详情（含实时统计 purchase_count / play_count / avg_play_hours）
     */
    @GetMapping("/{gameId}")
    public Result<Game> getGameById(@PathVariable Long gameId) {
        QueryWrapper<Game> wrapper = new QueryWrapper<>();
        wrapper.eq("game_id", gameId);
        Game game = gameMapper.selectOne(wrapper);
        if (game == null) {
            return Result.error("游戏不存在");
        }

        // 从 rating 表实时统计购买数、游玩数和平均时长（避免依赖 game 表静态字段默认值 0）
        try {
            QueryWrapper<com.gamerec.gamerecommend.entity.Rating> ratingWrapper = new QueryWrapper<>();
            ratingWrapper.eq("game_id", gameId).eq("is_valid", 1);
            long purchaseCount = ratingMapper.selectCount(ratingWrapper);

            ratingWrapper = new QueryWrapper<>();
            ratingWrapper.eq("game_id", gameId).eq("is_valid", 1).gt("play_hours", 0);
            long playCount = ratingMapper.selectCount(ratingWrapper);

            game.setPurchaseCount((int) purchaseCount);
            game.setPlayCount((int) playCount);

            // 同时更新平均游玩时长
            if (playCount > 0) {
                ratingWrapper = new QueryWrapper<>();
                ratingWrapper.select("AVG(play_hours) as avgHours")
                        .eq("game_id", gameId).eq("is_valid", 1).gt("play_hours", 0);
                // 用 MyBatis-Plus selectMaps 取聚合结果
                List<Map<String, Object>> maps = ratingMapper.selectMaps(ratingWrapper);
                if (maps != null && !maps.isEmpty() && maps.get(0).get("avgHours") != null) {
                    game.setAvgPlayHours(((Number) maps.get(0).get("avgHours")).doubleValue());
                }
            }
        } catch (Exception e) {
            // 如果统计查询失败，保持 game 表原有值
        }

        return Result.success(game);
    }

    /**
     * 新增游戏
     */
    @PostMapping
    public Result<String> addGame(@RequestBody Game game) {
        // 检查是否已存在
        QueryWrapper<Game> wrapper = new QueryWrapper<>();
        wrapper.eq("game_id", game.getGameId());
        if (gameMapper.selectCount(wrapper) > 0) {
            return Result.error("游戏ID已存在");
        }
        gameMapper.insert(game);
        return Result.success("新增成功");
    }

    /**
     * 更新游戏信息
     */
    @PutMapping("/{gameId}")
    public Result<String> updateGame(@PathVariable Long gameId, @RequestBody Game game) {
        QueryWrapper<Game> wrapper = new QueryWrapper<>();
        wrapper.eq("game_id", gameId);
        Game existing = gameMapper.selectOne(wrapper);
        if (existing == null) {
            return Result.error("游戏不存在");
        }
        game.setId(existing.getId());
        game.setGameId(gameId);
        gameMapper.updateById(game);
        return Result.success("更新成功");
    }

    /**
     * 删除游戏
     */
    @DeleteMapping("/{gameId}")
    public Result<String> deleteGame(@PathVariable Long gameId) {
        QueryWrapper<Game> wrapper = new QueryWrapper<>();
        wrapper.eq("game_id", gameId);
        Game existing = gameMapper.selectOne(wrapper);
        if (existing == null) {
            return Result.error("游戏不存在");
        }
        gameMapper.deleteById(existing.getId());
        return Result.success("删除成功");
    }

    /**
     * 获取游戏统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getGameStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalGames", gameMapper.selectCount(null));
        return Result.success(stats);
    }
}
