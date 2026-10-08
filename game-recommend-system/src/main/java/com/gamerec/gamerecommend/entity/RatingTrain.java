package com.gamerec.gamerecommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 训练集评分实体
 * 对应数据库表 rating_train
 *
 * 【问题 2.2 数据泄露修复】
 * 推荐算法的训练阶段只允许读取本表，严禁读取 rating 全量表或 rating_test，
 * 以保证离线评估时测试集对模型不可见。
 * 本表由 scripts/split_train_test.py 按用户随机 80/20 划分生成（seed=42）。
 *
 * @author GameRec Team
 */
@Data
@TableName("rating_train")
public class RatingTrain {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 游戏ID */
    @TableField("game_id")
    private Long gameId;

    /** 游戏名称（冗余，便于查询） */
    @TableField("game_name")
    private String gameName;

    /** 伪评分（1-5分，由游玩时长对数变换后等频分箱转化） */
    @TableField("rating")
    private Integer rating;

    /** 原始游玩时长（小时） */
    @TableField("play_hours")
    private Double playHours;

    /** 行为发生时间（真实时间序列，用于 RFM 与活跃度衰减计算） */
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
