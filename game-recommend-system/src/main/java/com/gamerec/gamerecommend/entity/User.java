package com.gamerec.gamerecommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 玩家实体类
 * 对应数据库表 user
 *
 * @author GameRec Team
 */
@Data
@TableName("user")
public class User {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 原始用户ID（1~10000） */
    @NotNull(message = "用户ID不能为空")
    @Min(value = 1, message = "用户ID最小为1")
    @TableField("user_id")
    private Long userId;

    /** 购买游戏总数 */
    @TableField("purchase_count")
    private Integer purchaseCount;

    /** 游玩游戏总数（play_hours>0） */
    @TableField("play_count")
    private Integer playCount;

    /** 总游玩时长（小时） */
    @TableField("total_play_hours")
    private Double totalPlayHours;

    /** 平均游玩时长（小时） */
    @TableField("avg_play_hours")
    private Double avgPlayHours;

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
