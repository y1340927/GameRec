package com.gamerec.gamerecommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 评分实体类
 * 对应数据库表 rating
 *
 * 核心创新：rating字段存储的是游玩时长分箱后的伪评分（1-5分）
 * play_hours保留原始游玩时长，is_valid用于逻辑删除
 *
 * @author GameRec Team
 */
@Data
@TableName("rating")
public class Rating {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    @NotNull(message = "用户ID不能为空")
    @TableField("user_id")
    private Long userId;

    /** 游戏ID */
    @NotNull(message = "游戏ID不能为空")
    @TableField("game_id")
    private Long gameId;

    /** 游戏名称（冗余，便于查询） */
    @TableField("game_name")
    private String gameName;

    /** 伪评分（1-5分，由游玩时长分箱转化） */
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    @TableField("rating")
    private Integer rating;

    /** 原始游玩时长（小时） */
    @Min(value = 0, message = "游玩时长不能为负数")
    @TableField("play_hours")
    private Double playHours;

    /**
     * 行为发生时间
     *
     * 【问题 2.3】RFM 的 R 值与活跃度时间衰减均基于本字段计算真实天数差，
     * 不再使用记录 id 反推时间。由 scripts/rebuild_dataset.py 生成
     * （365 天窗口内、同一用户严格递增的真实时间序列）。
     */
    @TableField("behavior_time")
    private LocalDateTime behaviorTime;

    /** 是否有效：1-有效 0-无效（逻辑删除） */
    @TableField("is_valid")
    @TableLogic(value = "1", delval = "0")
    private Integer isValid;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;
}
