package com.gamerec.gamerecommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 游戏实体类
 * 对应数据库表 game（37个字段，含中文全覆盖）
 * 数据来源：steam.csv（基础信息） + steam_games.csv（扩展信息，含评价数据）
 *
 * @author GameRec Team
 */
@Data
@TableName("game")
public class Game {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 游戏ID（对应steam.csv的appid） */
    @NotNull(message = "游戏ID不能为空")
    @TableField("game_id")
    private Long gameId;

    /** 游戏名称（英文，来自steam.csv） */
    @NotBlank(message = "游戏名称不能为空")
    @TableField("game_name")
    private String gameName;

    /** 游戏中文名（来自steam_games.csv匹配） */
    @TableField("game_name_cn")
    private String gameNameCn;

    /** 发布日期 */
    @TableField("release_date")
    private LocalDate releaseDate;

    /** 开发商（来自steam.csv） */
    @TableField("developer")
    private String developer;

    /** 发行商（来自steam.csv） */
    @TableField("publisher")
    private String publisher;

    /** 开发商列表（JSON数组，来自steam_games.csv） */
    @TableField("developers_json")
    private String developersJson;

    /** 发行商列表（JSON数组，来自steam_games.csv） */
    @TableField("publishers_json")
    private String publishersJson;

    /** 支持平台（英文，分号分隔） */
    @TableField("platforms")
    private String platforms;

    /** 支持平台（中文，分号分隔） */
    @TableField("platforms_cn")
    private String platformsCn;

    /** 年龄限制 */
    @TableField("required_age")
    private Integer requiredAge;

    /** 游戏分类（英文，分号分隔） */
    @TableField("categories")
    private String categories;

    /** 游戏分类（中文，分号分隔） */
    @TableField("categories_cn")
    private String categoriesCn;

    /** 游戏类型（英文，分号分隔，来自steam.csv） */
    @TableField("genres")
    private String genres;

    /** 游戏类型（中文，分号分隔） */
    @TableField("genres_cn")
    private String genresCn;

    /** 游戏类型（JSON数组，来自steam_games.csv） */
    @TableField("genres_json")
    private String genresJson;

    /** SteamSpy标签（英文，分号分隔） */
    @TableField("steamspy_tags")
    private String steamspyTags;

    /** SteamSpy标签（中文，分号分隔） */
    @TableField("steamspy_tags_cn")
    private String steamspyTagsCn;

    /** 成就数量 */
    @TableField("achievements")
    private Integer achievements;

    /** 好评数（来自steam.csv） */
    @TableField("positive_ratings")
    private Integer positiveRatings;

    /** 差评数（来自steam.csv） */
    @TableField("negative_ratings")
    private Integer negativeRatings;

    /** 平均游玩时长-全局（分钟） */
    @TableField("average_playtime")
    private Integer averagePlaytime;

    /** 中位游玩时长-全局（分钟） */
    @TableField("median_playtime")
    private Integer medianPlaytime;

    /** 拥有者数量范围 */
    @TableField("owners")
    private String owners;

    /** 价格（美元） */
    @TableField("price")
    private Double price;

    /** 初始价格（美元，来自steam_games.csv） */
    @TableField("price_initial")
    private Double priceInitial;

    /** 是否免费：0-否 1-是 */
    @TableField("is_free")
    private Integer isFree;

    /** 总评价数（来自steam_games.csv） */
    @TableField("total_reviews")
    private Integer totalReviews;

    /** 总好评数（来自steam_games.csv） */
    @TableField("total_positive")
    private Integer totalPositive;

    /** 总差评数（来自steam_games.csv） */
    @TableField("total_negative")
    private Integer totalNegative;

    /** 评价分数（来自steam_games.csv） */
    @TableField("review_score")
    private Double reviewScore;

    /** 评价描述（英文） */
    @TableField("review_score_desc")
    private String reviewScoreDesc;

    /** 评价描述（中文） */
    @TableField("review_score_desc_cn")
    private String reviewScoreDescCn;

    /** 好评率 */
    @TableField("positive_percentual")
    private Double positivePercentual;

    /** Metacritic评分 */
    @TableField("metacritic")
    private Integer metacritic;

    /** 游戏简介 */
    @TableField("short_description")
    private String shortDescription;

    /** 详细描述 */
    @TableField("detailed_description")
    private String detailedDescription;

    /** 被购买总次数（从行为数据统计） */
    @TableField("purchase_count")
    private Integer purchaseCount;

    /** 被游玩总次数（从行为数据统计） */
    @TableField("play_count")
    private Integer playCount;

    /** 平均游玩时长-本系统（小时，从行为数据统计） */
    @TableField("avg_play_hours")
    private Double avgPlayHours;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;

    // ==================== 计算属性（非数据库字段） ====================

    /**
     * 计算好评率（百分比字符串）
     * 优先使用 positive_percentual 字段，其次计算
     */
    public String getPositiveRateStr() {
        if (positivePercentual != null && positivePercentual > 0) {
            return String.format("%.0f%%", positivePercentual);
        }
        long pos = positiveRatings != null ? positiveRatings : 0;
        long neg = negativeRatings != null ? negativeRatings : 0;
        long total = pos + neg;
        if (total > 0) {
            return String.format("%.0f%%", pos * 100.0 / total);
        }
        return "暂无";
    }

    /**
     * 获取评价描述中文
     */
    public String getReviewDescCn() {
        if (reviewScoreDescCn != null && !reviewScoreDescCn.isEmpty()) {
            return reviewScoreDescCn;
        }
        if (reviewScoreDesc == null) return "暂无评价";
        switch (reviewScoreDesc) {
            case "Overwhelmingly Positive": return "好评如潮";
            case "Very Positive": return "特别好评";
            case "Positive": return "好评";
            case "Mostly Positive": return "多半好评";
            case "Mixed": return "褒贬不一";
            case "Mostly Negative": return "多半差评";
            case "Negative": return "差评";
            case "Very Negative": return "特别差评";
            case "Overwhelmingly Negative": return "差评如潮";
            case "No user reviews": return "暂无评价";
            default: return reviewScoreDesc;
        }
    }

    /**
     * 获取 Metacritic 评分颜色等级
     */
    public String getMetacriticLevel() {
        if (metacritic == null) return "none";
        if (metacritic >= 90) return "must-play";
        if (metacritic >= 75) return "positive";
        if (metacritic >= 50) return "mixed";
        return "negative";
    }
}
