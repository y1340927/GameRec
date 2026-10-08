package com.gamerec.gamerecommend.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 智谱 BigModel chat/completions 响应体
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionResponse {

    /** 任务 ID */
    private String id;

    /** 创建时间戳（秒） */
    private Long created;

    /** 模型名 */
    private String model;

    /** 请求 ID */
    private String request_id;

    /** 任务类型 */
    private String task_status;

    /** token 用量（对象，含 prompt_tokens/completion_tokens/total_tokens） */
    private Object usage;

    /** 错误码（如有） */
    private String error_code;

    /** 错误信息（如有） */
    private String error_msg;

    /** 选择列表（通常只有 1 个） */
    private List<Choice> choices;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Choice {
        /** 序号 */
        private Integer index;

        /** 结束原因：stop / length / tool_calls */
        private String finish_reason;

        /** 消息对象 */
        private Message message;

        /** 模型思考过程（仅 GLM-4.5 系列支持） */
        private String reasoning_content;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {
        private String role;
        private String content;
    }
}
