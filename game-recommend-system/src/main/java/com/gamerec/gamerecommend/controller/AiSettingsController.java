package com.gamerec.gamerecommend.controller;

import com.gamerec.gamerecommend.ai.AiConfig;
import com.gamerec.gamerecommend.ai.AiService;
import com.gamerec.gamerecommend.dto.Result;
import com.gamerec.gamerecommend.entity.AiSettings;
import com.gamerec.gamerecommend.service.AiSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 接口设置 REST 接口
 * <p>
 * 允许用户在系统设置页面自行配置 AI API（Key / 模型 / 接口地址等），
 * 配置持久化到 ai_settings 表，无需修改配置文件或重启服务。
 * </p>
 *
 * <pre>
 *   GET  /api/ai/settings   获取当前设置（API Key 脱敏展示）
 *   POST /api/ai/settings   保存设置（apiKey 留空则保留原值）
 *   POST /api/ai/test       使用当前生效配置测试连通性
 * </pre>
 *
 * @author GameRec Team
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiSettingsController {

    private final AiSettingsService aiSettingsService;
    private final AiService aiService;

    /**
     * 获取当前 AI 设置（apiKey 已脱敏，不会回传完整 Key）
     */
    @GetMapping("/settings")
    public Result<Map<String, Object>> getSettings() {
        AiSettings s = aiSettingsService.getSettings();
        AiConfig effective = aiSettingsService.getEffectiveConfig();
        Map<String, Object> data = new HashMap<>();
        boolean configured = s != null && aiSettingsService.isValidKey(s.getApiKey());
        data.put("configured", configured);
        data.put("enabled", effective.getEnabled());
        data.put("apiKeyMasked", s == null ? "" : aiSettingsService.maskKey(s.getApiKey()));
        data.put("model", effective.getModel());
        data.put("baseUrl", effective.getBaseUrl());
        data.put("maxTokens", effective.getMaxTokens());
        data.put("temperature", effective.getTemperature());
        data.put("timeoutSeconds", effective.getTimeoutSeconds());
        data.put("available", aiService.isAvailable());
        return Result.success(data);
    }

    /**
     * 保存 AI 设置
     * <p>
     * 安全约定：前端提交的 apiKey 若为空 / 占位符 / 脱敏值（****），
     * 后端保留数据库原值，避免误清空已配置的 Key。
     * </p>
     */
    @PostMapping("/settings")
    public Result<Map<String, Object>> saveSettings(@RequestBody @Valid AiSettings settings) {
        AiSettings saved = aiSettingsService.saveSettings(settings);
        Map<String, Object> data = new HashMap<>();
        data.put("configured", saved != null && aiSettingsService.isValidKey(saved.getApiKey()));
        data.put("apiKeyMasked", saved == null ? "" : aiSettingsService.maskKey(saved.getApiKey()));
        data.put("model", saved != null ? saved.getModel() : null);
        data.put("baseUrl", saved != null ? saved.getBaseUrl() : null);
        return Result.success("AI 设置已保存", data);
    }

    /**
     * 使用当前生效配置测试 AI 接口连通性
     */
    @PostMapping("/test")
    public Result<Map<String, Object>> testConnection() {
        Map<String, Object> data = new HashMap<>();
        if (!aiService.isAvailable()) {
            data.put("success", false);
            data.put("message", "尚未配置有效的 API Key，请在系统设置中填写后再测试。");
            return Result.success(data);
        }
        long start = System.currentTimeMillis();
        String reply = aiService.chat("请回复「连接成功」四个字，不要输出其他内容。");
        long cost = System.currentTimeMillis() - start;
        boolean success = reply != null
                && !reply.startsWith("⚠️")
                && reply.contains("连接成功");
        data.put("success", success);
        data.put("reply", reply);
        data.put("costMs", cost);
        data.put("model", aiSettingsService.getEffectiveConfig().getModel());
        return Result.success(data);
    }
}
