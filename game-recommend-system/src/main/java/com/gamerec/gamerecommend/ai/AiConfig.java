package com.gamerec.gamerecommend.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 智谱 BigModel AI 配置
 * <p>
 * 通过 application.yml 中的 game-rec.ai 段配置（默认值兜底）
 * 运行时优先级：数据库 ai_settings 表（系统设置页面） > 环境变量 > 本配置
 * 默认使用 GLM-4-Flash-250414（免费模型，无思考模式，响应速度极快）
 * </p>
 *
 * <pre>
 * game-rec:
 *   ai:
 *     enabled: true
 *     api-key: ${AI_API_KEY:}
 *     model: glm-4-flash-250414
 * </pre>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "game-rec.ai")
public class AiConfig {

    /** 是否启用 AI 功能 */
    private Boolean enabled = true;

    /** 智谱 BigModel API Key（未设置则 AI 接口返回友好提示；推荐在系统设置页面配置） */
    private String apiKey;

    /** 调用的模型，默认 glm-4-flash-250414（免费模型，响应速度极快） */
    private String model = "glm-4-flash-250414";

    /** API 基础地址 */
    private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";

    /** 单次请求最大 token 数（Flash 模型无思考开销，1024 即可） */
    private Integer maxTokens = 1024;

    /** 温度参数 (0-1) */
    private Double temperature = 0.5;

    /** 请求超时（秒）— 加大到 90 秒，覆盖智谱 BigModel 慢时段 */
    private Integer timeoutSeconds = 90;

    /**
     * 系统提示词模板：GameRec 系统专属 AI 助手
     * 严格要求：所有回答必须基于系统内部数据，不编造游戏/玩家信息
     */
    private String systemPrompt = "你是 GameRec 系统的内置 AI 助手，所有回答必须严格基于系统内部数据（game/ rating/ user_profile 表），不得编造任何游戏名、玩家信息或数据。\n" +
            "\n" +
            "回答规则：\n" +
            "1. 【数据准确性】当系统提供游戏/玩家/评价数据时，直接引用；如数据缺失则明确说明「该数据系统未收录」；\n" +
            "2. 【来源标注】关键数据需标注「来源：系统数据库」；\n" +
            "3. 【专业简洁】中文回答，结构清晰，不超过 500 字；\n" +
            "4. 【游戏推荐】基于系统已有游戏库推荐，给出游戏名+亮点+类型+评分；\n" +
            "5. 【玩家分析】基于 user_profile 表的 RFM/活跃度/兴趣标签等真实数据；\n" +
            "6. 【明确边界】不知道就说不清楚，不要凭空生成信息。";
}
