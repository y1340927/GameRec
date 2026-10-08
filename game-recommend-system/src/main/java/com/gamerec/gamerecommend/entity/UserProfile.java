package com.gamerec.gamerecommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家画像实体类
 * 对应数据库表 user_profile
 *
 * 包含四个维度：
 *   1. RFM价值分层
 *   2. 时间衰减活跃度
 *   3. 兴趣标签（JSON）
 *   4. 玩家类型（硬核/休闲/尝鲜/收藏）
 *
 * @author GameRec Team
 */
@Data
@TableName("user_profile")
public class UserProfile {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    // ===== RFM相关 =====

    /** 最近游玩距今天数（R） */
    @TableField("recency")
    private Long recency;

    /** 游玩游戏总次数（F） */
    @TableField("frequency")
    private Integer frequency;

    /** 平均伪评分（M） */
    @TableField("monetary")
    private Double monetary;

    /** R评分（1-4分，四分位法） */
    @TableField("r_score")
    private Integer rScore;

    /** F评分（1-4分，四分位法） */
    @TableField("f_score")
    private Integer fScore;

    /** M评分（1-4分，四分位法） */
    @TableField("m_score")
    private Integer mScore;

    /** 价值等级：高价值/中价值/低价值/沉默 */
    @TableField("value_level")
    private String valueLevel;

    // ===== 活跃度相关 =====

    /** 活跃度原始得分（时间衰减加权） */
    @TableField("activity_score")
    private Double activityScore;

    /** 活跃度归一化得分（0-1） */
    @TableField("activity_normalized")
    private Double activityNormalized;

    /** 活跃度等级：高热/中热/低热/冰封 */
    @TableField("activity_level")
    private String activityLevel;

    // ===== 兴趣标签（JSON格式） =====

    /** 兴趣标签（JSON数组，含标签名、平均分、频次） */
    @TableField("interest_tags")
    private String interestTags;

    // ===== 创新维度：玩家类型 =====

    /** 玩家类型：硬核/休闲/尝鲜/收藏 */
    @TableField("player_type")
    private String playerType;

    /** 总游玩时长（小时） */
    @TableField("total_play_hours")
    private Double totalPlayHours;

    /** 平均每游戏游玩时长（小时） */
    @TableField("avg_play_hours")
    private Double avgPlayHours;

    /** 购买游戏总数 */
    @TableField("purchase_count")
    private Integer purchaseCount;

    /** 游玩游戏总数 */
    @TableField("play_count")
    private Integer playCount;

    /** 购买-游玩转化率（游玩数/购买数） */
    @TableField("purchase_play_ratio")
    private Double purchasePlayRatio;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
