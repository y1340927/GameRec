package com.gamerec.gamerecommend.ai;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * AI 聊天请求
 */
@Data
@NoArgsConstructor
public class AiChatRequest {

    /** 用户消息（必填） */
    @NotBlank(message = "消息不能为空")
    private String message;

    /** 多轮历史（可选） */
    private List<HistoryItem> history;

    /** 自定义系统提示词（可选） */
    private String systemPrompt;

    @Data
    @NoArgsConstructor
    public static class HistoryItem {
        private String role;   // "user" | "assistant"
        private String content;
    }
}
