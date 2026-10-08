package com.gamerec.gamerecommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 接口设置实体类
 * 对应数据库表 ai_settings（单行配置，固定 id=1）
 * <p>
 * 设计目的：API Key 由用户在系统设置页面自行配置并入库保存，
 * 不再硬编码在 application.yml / 源码中，避免密钥泄露。
 * </p>
 *
 * @author GameRec Team
 */
@Data
@TableName("ai_settings")
public class AiSettings {

    /** 主键ID（固定为 1，单行配置） */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 是否启用 AI 功能：1-启用 0-禁用 */
    @TableField("enabled")
    private Integer enabled;

    /** AI 服务 API Key（用户自行填写） */
    @TableField("api_key")
    private String apiKey;

    /** 模型名称（如智谱 GLM-4-Flash-250414） */
    @TableField("model")
    private String model;

    /** API 基础地址（兼容 OpenAI 格式接口） */
    @TableField("base_url")
    private String baseUrl;

    /** 单次请求最大 token 数 */
    @TableField("max_tokens")
    private Integer maxTokens;

    /** 温度参数（0-1，越低越稳定） */
    @TableField("temperature")
    private Double temperature;

    /** 请求超时时间（秒） */
    @TableField("timeout_seconds")
    private Integer timeoutSeconds;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
