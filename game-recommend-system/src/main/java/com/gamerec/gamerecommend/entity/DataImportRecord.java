package com.gamerec.gamerecommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据导入记录实体类
 * 对应数据库表 data_import_record
 *
 * @author GameRec Team
 */
@Data
@TableName("data_import_record")
public class DataImportRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 数据类型：USER/GAME/GAME_EXT/RATING/GAME_STATS */
    @TableField("data_type")
    private String dataType;

    /** 导入的源文件名 */
    @TableField("file_name")
    private String fileName;

    /** 读取总条数 */
    @TableField("import_count")
    private Integer importCount;

    /** 新增条数 */
    @TableField("inserted_count")
    private Integer insertedCount;

    /** 更新条数 */
    @TableField("updated_count")
    private Integer updatedCount;

    /** 跳过条数 */
    @TableField("skipped_count")
    private Integer skippedCount;

    /** 失败条数 */
    @TableField("failed_count")
    private Integer failedCount;

    /** 错误信息 */
    @TableField("error_msg")
    private String errorMsg;

    /** 导入开始时间 */
    @TableField("start_time")
    private LocalDateTime startTime;

    /** 导入结束时间 */
    @TableField("end_time")
    private LocalDateTime endTime;

    /** 导入耗时（秒） */
    @TableField("duration_seconds")
    private Double durationSeconds;

    /** 记录创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;
}
