package com.gamerec.gamerecommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测试集评分实体
 * 对应数据库表 rating_test
 *
 * 【问题 2.2 数据泄露修复】
 * 离线评估阶段"用户实际喜欢的游戏集合"只允许从本表读取，
 * 与训练侧使用的 rating_train 物理隔离，杜绝模型提前见过测试答案。
 * 本表由 scripts/split_train_test.py 按用户随机 80/20 划分生成（seed=42）。
 *
 * @author GameRec Team
 */
@Data
@TableName("rating_test")
public class RatingTest {

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

    /** 伪评分（1-5分） */
    @TableField("rating")
    private Integer rating;

    /** 原始游玩时长（小时） */
    @TableField("play_hours")
    private Double playHours;

    /** 行为发生时间 */
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
