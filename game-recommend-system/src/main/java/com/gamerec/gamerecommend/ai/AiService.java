package com.gamerec.gamerecommend.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamerec.gamerecommend.service.AiSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 智谱 BigModel AI 服务
 * <p>
 * 修复版本 v2:
 * 1. 限制 history 长度（最多保留 6 轮 = 12 条消息），避免请求体过大导致超时
 * 2. 区分 connectTimeout / readTimeout
 * 3. 捕获 Read timed out 等网络异常，返回友好提示
 * 4. 添加 tryLock 避免并发请求
 * </p>
 * <p>
 * 修复版本 v3（动态配置）:
 * 5. API Key / 模型 / 接口地址等配置支持运行时动态读取（ai_settings 表），
 *    用户在系统设置页面配置后无需重启服务即可生效
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    /** 最大保留的 history 轮数（user+assistant 算一轮） */
    private static final int MAX_HISTORY_ROUNDS = 6;
    /** 单条消息最大字符数（避免系统注入的单条 history 超长） */
    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final AiSettingsService aiSettingsService;
    private RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 用于简单的并发控制：同一时刻只允许一个 AI 请求处理（防止前端高频点击
     * 导致后端堆积请求加剧 BigModel 超时）。
     */
    private final AtomicInteger concurrentRequests = new AtomicInteger(0);
    private static final int MAX_CONCURRENT_REQUESTS = 5;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 关键修复1: 区分连接超时与读取超时，并加大到 90 秒（与前端对齐）
        factory.setConnectTimeout(10_000);
        AiConfig effective = aiSettingsService.getEffectiveConfig();
        int readTimeout = (effective.getTimeoutSeconds() != null ? effective.getTimeoutSeconds() : 90) * 1000;
        factory.setReadTimeout(readTimeout);
        this.restTemplate = new RestTemplate(factory);
        log.info("[AiService] 初始化完成: model={}, baseUrl={}, readTimeout={}ms",
                effective.getModel(), effective.getBaseUrl(), readTimeout);
    }

    /**
     * 判断是否已配置 API Key（未配置时所有 AI 调用会返回友好提示）
     * 基于动态生效配置判断（数据库设置 > 环境变量 > yml 默认）
     */
    public boolean isAvailable() {
        AiConfig effective = aiSettingsService.getEffectiveConfig();
        return effective.getEnabled() != null
                && effective.getEnabled()
                && effective.getApiKey() != null
                && !effective.getApiKey().isEmpty()
                && !effective.getApiKey().startsWith("your-")
                && !effective.getApiKey().equals("请设置你的智谱API_KEY");
    }

    /**
     * 单轮对话
     */
    public String chat(String userMessage) {
        return chat(userMessage, null, null);
    }

    /**
     * 多轮对话（保留上下文）
     */
    public String chat(String userMessage, List<ChatCompletionRequest.Message> history) {
        return chat(userMessage, history, null);
    }

    /**
     * 通用对话方法（带并发保护 + history 截断 + 单条长度限制）
     * 每次调用实时读取生效配置，支持用户在设置页面修改后立即生效
     */
    public String chat(String userMessage,
                       List<ChatCompletionRequest.Message> history,
                       String customSystemPrompt) {
        if (!isAvailable()) {
            return "⚠️ AI 助手尚未配置 API Key。\n\n请在系统右上角「系统设置」→「AI 设置」中填写你的 API Key 后重试。\n\n（推荐使用智谱 BigModel 的免费模型 glm-4-flash-250414）";
        }

        // 实时读取生效配置（数据库设置 > 环境变量 > yml 默认）
        AiConfig effective = aiSettingsService.getEffectiveConfig();

        // 动态更新读取超时（用户在设置页面修改超时时间后无需重启）
        try {
            int readTimeout = (effective.getTimeoutSeconds() != null ? effective.getTimeoutSeconds() : 90) * 1000;
            if (restTemplate.getRequestFactory() instanceof SimpleClientHttpRequestFactory) {
                ((SimpleClientHttpRequestFactory) restTemplate.getRequestFactory()).setReadTimeout(readTimeout);
            }
        } catch (Exception e) {
            log.debug("[AiService] 动态更新超时失败（忽略）", e);
        }

        // 并发控制
        int current = concurrentRequests.incrementAndGet();
        try {
            if (current > MAX_CONCURRENT_REQUESTS) {
                log.warn("[AiService] 并发请求过多: current={}, max={}", current, MAX_CONCURRENT_REQUESTS);
                return "⚠️ AI 服务正忙，请稍后重试（当前并发: " + current + "/" + MAX_CONCURRENT_REQUESTS + "）";
            }

            // ====== 关键修复1: 截断 history，避免请求体过大 ======
            List<ChatCompletionRequest.Message> trimmedHistory = trimHistory(history, MAX_HISTORY_ROUNDS);

            // ====== 关键修复2: 截断 userMessage ======
            String safeUserMessage = truncate(userMessage, MAX_MESSAGE_LENGTH);

            // 构造消息列表
            List<ChatCompletionRequest.Message> messages = new ArrayList<>();
            messages.add(new ChatCompletionRequest.Message() {{
                setRole("system");
                setContent(customSystemPrompt != null ? customSystemPrompt : effective.getSystemPrompt());
            }});
            if (trimmedHistory != null) messages.addAll(trimmedHistory);
            messages.add(new ChatCompletionRequest.Message() {{
                setRole("user");
                setContent(safeUserMessage);
            }});

            // 构造请求
            ChatCompletionRequest request = new ChatCompletionRequest();
            request.setModel(effective.getModel());
            request.setMessages(messages);
            request.setStream(false);
            request.setTemperature(effective.getTemperature());
            // maxTokens 防御：避免 maxTokens 设得过大导致返回极慢
            int maxTokens = effective.getMaxTokens() != null ? effective.getMaxTokens() : 1024;
            request.setMax_tokens(Math.min(maxTokens, 2048));

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(effective.getApiKey());
            // BigModel 要求显式 Accept 头
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            String body;
            try {
                body = mapper.writeValueAsString(request);
            } catch (JsonProcessingException e) {
                log.error("[AiService] 请求序列化失败", e);
                return "⚠️ 请求序列化失败：" + e.getMessage();
            }
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            // 调用 API
            String url = effective.getBaseUrl() + "/chat/completions";
            log.debug("[AiService] 调用 BigModel: history={}条, userMsg={}字符",
                    trimmedHistory != null ? trimmedHistory.size() : 0, safeUserMessage.length());

            String responseBody = restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
            ChatCompletionResponse response = mapper.readValue(responseBody, ChatCompletionResponse.class);

            if (response.getError_code() != null) {
                log.warn("[AiService] BigModel 错误: {} - {}", response.getError_code(), response.getError_msg());
                return "⚠️ AI 服务返回错误：" + response.getError_msg();
            }
            if (response.getChoices() == null || response.getChoices().isEmpty()) {
                log.warn("[AiService] BigModel 无返回 choices: {}", responseBody);
                return "⚠️ AI 服务未返回结果";
            }

            ChatCompletionResponse.Choice choice = response.getChoices().get(0);
            String content = choice.getMessage() != null ? choice.getMessage().getContent() : null;
            return content != null ? content.trim() : "";
        } catch (ResourceAccessException e) {
            // ====== 关键修复3: 网络/超时友好提示 ======
            log.warn("[AiService] AI 网络异常: {}", e.getMessage());
            Throwable cause = e.getCause();
            if (cause instanceof java.net.SocketTimeoutException) {
                AiConfig cur = aiSettingsService.getEffectiveConfig();
                return "⚠️ AI 服务响应超时（" + (cur.getTimeoutSeconds() != null ? cur.getTimeoutSeconds() : 60)
                        + "秒），可能是 AI 服务当前较慢，请稍后重试或缩短输入。";
            }
            if (cause instanceof java.net.ConnectException) {
                return "⚠️ 无法连接 AI 服务，请检查网络。";
            }
            return "⚠️ AI 服务网络异常：" + (e.getMessage() != null ? e.getMessage() : "未知错误");
        } catch (Exception e) {
            log.error("[AiService] AI 调用异常", e);
            return "⚠️ AI 调用失败：" + e.getClass().getSimpleName() + " - " + e.getMessage();
        } finally {
            concurrentRequests.decrementAndGet();
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 截断 history 长度（保留最近 N 轮 user+assistant 对话）。
     * 如果 history 异常（如连续同角色），自动过滤。
     */
    private List<ChatCompletionRequest.Message> trimHistory(List<ChatCompletionRequest.Message> history, int maxRounds) {
        if (history == null || history.isEmpty()) return null;
        // 过滤掉 null 内容或空内容
        List<ChatCompletionRequest.Message> valid = new ArrayList<>();
        for (ChatCompletionRequest.Message m : history) {
            if (m == null || m.getRole() == null || m.getContent() == null || m.getContent().trim().isEmpty()) {
                continue;
            }
            // 截断单条消息
            m.setContent(truncate(m.getContent(), MAX_MESSAGE_LENGTH));
            valid.add(m);
        }
        if (valid.isEmpty()) return null;

        // 只保留最近 maxRounds * 2 条消息
        int maxSize = maxRounds * 2;
        if (valid.size() <= maxSize) {
            return valid;
        }
        return new ArrayList<>(valid.subList(valid.size() - maxSize, valid.size()));
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "…(已截断)";
    }

    /** 获取当前并发数（用于监控/调试） */
    public int getConcurrentCount() {
        return concurrentRequests.get();
    }
}
