package com.gamerec.gamerecommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评估结果实体类
 * 对应数据库表 evaluation_result
 *
 * @author GameRec Team
 */
@Data
@TableName("evaluation_result")
public class EvaluationResult {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 算法名称：UserCF/ItemCF/CB/Popularity/SVD/Hybrid */
    @TableField("algorithm_name")
    private String algorithmName;

    /** Top-K值（5/10/15/20） */
    @TableField("top_k")
    private Integer topK;

    /** 准确率 Precision@K */
    @TableField("precision_val")
    private Double precisionVal;

    /** 召回率 Recall@K */
    @TableField("recall_val")
    private Double recallVal;

    /** F1值 F1@K */
    @TableField("f1_val")
    private Double f1Val;

    /** NDCG值 NDCG@K */
    @TableField("ndcg_val")
    private Double ndcgVal;

    /** 有效评估用户数 */
    @TableField("valid_users")
    private Integer validUsers;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;
}
