package com.gamerec.gamerecommend.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 智谱 BigModel chat/completions 请求体
 * 兼容 GLM-4 全系列模型
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionRequest {

    /** 调用的模型 ID */
    private String model;

    /** 对话消息列表 */
    private List<Message> messages;

    /** 是否启用流式响应 */
    private Boolean stream = false;

    /** 温度参数 */
    private Double temperature;

    /** 最大输出 token */
    private Integer max_tokens;

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {
        /** 角色: system | user | assistant */
        private String role;
        /** 消息内容 */
        private String content;
    }
}
