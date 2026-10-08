package com.gamerec.gamerecommend.service;

import com.gamerec.gamerecommend.ai.AiConfig;
import com.gamerec.gamerecommend.entity.AiSettings;
import com.gamerec.gamerecommend.mapper.AiSettingsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 接口设置服务
 * <p>
 * 支持用户在系统设置页面动态配置 API（Key/模型/接口地址等），
 * 配置持久化到 ai_settings 表（单行，固定 id=1）。
 * </p>
 * <p>
 * 配置优先级（从高到低）：
 * <pre>
 *   1. 数据库 ai_settings 表中的配置（用户在页面填写，无需重启）
 *   2. 环境变量（AI_API_KEY 等，可覆盖 yml 默认值）
 *   3. application.yml 中的 game-rec.ai 默认值
 * </pre>
 * </p>
 *
 * @author GameRec Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSettingsService {

    /** 单行配置固定主键 */
    private static final Long SETTINGS_ID = 1L;

    /** 常见占位符：未配置 key 时视为无效 */
    private static final String[] PLACEHOLDER_KEYS = {
            "your-", "your_key_here", "请设置你的", "set-your", "xxx", "***"
    };

    private final AiSettingsMapper aiSettingsMapper;
    private final AiConfig aiConfig;

    /**
     * 获取数据库中的设置行；不存在则返回 null
     */
    public AiSettings getSettings() {
        return aiSettingsMapper.selectById(SETTINGS_ID);
    }

    /**
     * 保存/更新数据库设置
     * <p>
     * 注意：若传入的 apiKey 为空、为占位符或被前端脱敏（****），
     * 则保留数据库中的原值，避免误清空。
     * </p>
     *
     * @param settings 前端提交的设置（id 可为空）
     * @return 保存后的完整设置
     */
    public AiSettings saveSettings(AiSettings settings) {
        AiSettings exist = getSettings();
        if (exist == null) {
            exist = new AiSettings();
            exist.setId(SETTINGS_ID);
            if (settings.getId() != null) {
                exist.setId(settings.getId());
            }
            exist.setEnabled(settings.getEnabled() != null ? settings.getEnabled() : 1);
            exist.setApiKey(normalizeKey(settings.getApiKey(), null));
            exist.setModel(settings.getModel());
            exist.setBaseUrl(settings.getBaseUrl());
            exist.setMaxTokens(settings.getMaxTokens());
            exist.setTemperature(settings.getTemperature());
            exist.setTimeoutSeconds(settings.getTimeoutSeconds());
            aiSettingsMapper.insert(exist);
        } else {
            if (settings.getEnabled() != null) exist.setEnabled(settings.getEnabled());
            // apiKey 为空 / 占位符 / 脱敏值时不覆盖原值
            String newKey = normalizeKey(settings.getApiKey(), exist.getApiKey());
            exist.setApiKey(newKey);
            if (settings.getModel() != null && !settings.getModel().trim().isEmpty()) {
                exist.setModel(settings.getModel().trim());
            }
            if (settings.getBaseUrl() != null && !settings.getBaseUrl().trim().isEmpty()) {
                exist.setBaseUrl(settings.getBaseUrl().trim());
            }
            if (settings.getMaxTokens() != null) exist.setMaxTokens(settings.getMaxTokens());
            if (settings.getTemperature() != null) exist.setTemperature(settings.getTemperature());
            if (settings.getTimeoutSeconds() != null) exist.setTimeoutSeconds(settings.getTimeoutSeconds());
            aiSettingsMapper.updateById(exist);
        }
        log.info("[AiSettings] 已更新 AI 设置: enabled={}, model={}, baseUrl={}",
                exist.getEnabled(), exist.getModel(), exist.getBaseUrl());
        return getSettings();
    }

    /**
     * 计算「生效配置」：数据库优先，yml/环境变量兜底
     *
     * @return 一份合并后的 AiConfig（不会修改原始配置对象）
     */
    public AiConfig getEffectiveConfig() {
        AiConfig effective = new AiConfig();
        // 1. 复制 yml 默认值（环境变量可覆盖）
        effective.setEnabled(aiConfig.getEnabled());
        effective.setApiKey(aiConfig.getApiKey());
        effective.setModel(aiConfig.getModel());
        effective.setBaseUrl(aiConfig.getBaseUrl());
        effective.setMaxTokens(aiConfig.getMaxTokens());
        effective.setTemperature(aiConfig.getTemperature());
        effective.setTimeoutSeconds(aiConfig.getTimeoutSeconds());
        effective.setSystemPrompt(aiConfig.getSystemPrompt());

        // 2. 数据库配置覆盖
        AiSettings s = getSettings();
        if (s != null) {
            if (s.getEnabled() != null) {
                effective.setEnabled(s.getEnabled() == 1);
            }
            if (isValidKey(s.getApiKey())) {
                effective.setApiKey(s.getApiKey());
            }
            if (s.getModel() != null && !s.getModel().trim().isEmpty()) {
                effective.setModel(s.getModel().trim());
            }
            if (s.getBaseUrl() != null && !s.getBaseUrl().trim().isEmpty()) {
                effective.setBaseUrl(s.getBaseUrl().trim());
            }
            if (s.getMaxTokens() != null && s.getMaxTokens() > 0) {
                effective.setMaxTokens(s.getMaxTokens());
            }
            if (s.getTemperature() != null) {
                effective.setTemperature(s.getTemperature());
            }
            if (s.getTimeoutSeconds() != null && s.getTimeoutSeconds() > 0) {
                effective.setTimeoutSeconds(s.getTimeoutSeconds());
            }
        }
        return effective;
    }

    /**
     * 数据库中的 API Key 是否有效（非空、非占位符、非脱敏值）
     */
    public boolean isValidKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) return false;
        String key = apiKey.trim();
        if (key.startsWith("****") || key.contains("****")) return false;
        if (key.length() < 8) return false;
        for (String p : PLACEHOLDER_KEYS) {
            if (key.toLowerCase().contains(p.toLowerCase())) return false;
        }
        return true;
    }

    /**
     * API Key 脱敏展示：仅保留前 4 位与后 4 位，中间用 * 替代
     */
    public String maskKey(String apiKey) {
        if (!isValidKey(apiKey)) return "";
        if (apiKey.length() <= 8) return "****";
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * 规范化 key：空/占位符/脱敏值返回 fallback（通常为原库值），否则返回修剪后的新值
     */
    private String normalizeKey(String newKey, String fallback) {
        if (newKey == null || newKey.trim().isEmpty()) return fallback;
        String trimmed = newKey.trim();
        if (!isValidKey(trimmed)) return fallback;
        return trimmed;
    }
}
