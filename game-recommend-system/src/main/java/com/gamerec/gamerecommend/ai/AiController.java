package com.gamerec.gamerecommend.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamerec.gamerecommend.dto.Result;
import com.gamerec.gamerecommend.entity.Game;
import com.gamerec.gamerecommend.entity.UserProfile;
import com.gamerec.gamerecommend.mapper.GameMapper;
import com.gamerec.gamerecommend.mapper.UserProfileMapper;
import com.gamerec.gamerecommend.service.AiSettingsService;
import com.gamerec.gamerecommend.service.AnalysisService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 助手 REST 接口
 * <p>
 * 完全基于 GameRec 系统内部数据（game / rating / user_profile 表），
 * AI 不会编造任何信息，所有回答都有系统数据库支撑。
 * </p>
 *
 * <pre>
 *   GET  /api/ai/status               检查 AI 是否可用
 *   POST /api/ai/chat                 通用对话（自由提问）
 *   GET  /api/ai/game-info            获取单个游戏 AI 介绍（已废弃，请用 /game-context）
 *   POST /api/ai/game-context         游戏详情页 AI 对话（注入游戏完整数据作为上下文）
 *   POST /api/ai/user-context         玩家画像 AI 对话（注入用户画像作为上下文）
 *   POST /api/ai/system-stats         系统统计问答（注入系统全局统计）
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final AiConfig aiConfig;
    private final AiSettingsService aiSettingsService;
    private final GameMapper gameMapper;
    private final UserProfileMapper userProfileMapper;
    private final AnalysisService analysisService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取当前生效的模型名（数据库设置 > 环境变量 > yml 默认）
     */
    private String currentModel() {
        return aiSettingsService.getEffectiveConfig().getModel();
    }

    /**
     * 检查 AI 服务状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        Map<String, Object> data = new HashMap<>();
        data.put("available", aiService.isAvailable());
        data.put("model", currentModel());
        return Result.success(data);
    }

    /**
     * 通用对话
     */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@Valid @RequestBody AiChatRequest request) {
        List<ChatCompletionRequest.Message> history = toHistory(request.getHistory());
        String reply = aiService.chat(request.getMessage(), history, request.getSystemPrompt());
        Map<String, Object> data = new HashMap<>();
        data.put("reply", reply);
        data.put("model", currentModel());
        return Result.success(data);
    }

    /**
     * 游戏详情页 AI 对话
     * <p>
     * 后端根据 gameId 拉取完整游戏数据（基本信息、评分、统计、热门评价），
     * 注入到 system prompt 作为上下文，确保 AI 回答完全基于系统真实数据。
     * </p>
     */
    @PostMapping("/game-context")
    public Result<Map<String, Object>> gameContext(@Valid @RequestBody GameContextRequest request) {
        if (!aiService.isAvailable()) {
            return Result.success(fallbackReply("AI 助手暂未配置 API Key。"));
        }
        Game game = gameMapper.selectByGameId(request.getGameId());
        if (game == null) {
            Map<String, Object> data = new HashMap<>();
            data.put("reply", "⚠️ 系统数据库中未找到 gameId=" + request.getGameId() + " 的游戏，无法基于系统数据回答。");
            data.put("model", currentModel());
            return Result.success(data);
        }
        String context = buildGameContext(game);
        String userPrompt = "[系统已为你加载游戏「" + (game.getGameNameCn() != null ? game.getGameNameCn() : game.getGameName())
                + "」完整数据]\n\n" + request.getMessage();
        List<ChatCompletionRequest.Message> history = toHistory(request.getHistory());
        String reply = aiService.chat(userPrompt, history, context);
        Map<String, Object> data = new HashMap<>();
        data.put("reply", reply);
        data.put("model", currentModel());
        data.put("gameId", game.getGameId());
        data.put("gameName", game.getGameNameCn() != null ? game.getGameNameCn() : game.getGameName());
        return Result.success(data);
    }

    /**
     * 玩家画像 AI 对话
     * <p>
     * 后端根据 userId 拉取 user_profile 完整画像（RFM/活跃度/兴趣标签），
     * 注入到 system prompt 作为上下文。
     * </p>
     */
    @PostMapping("/user-context")
    public Result<Map<String, Object>> userContext(@Valid @RequestBody UserContextRequest request) {
        if (!aiService.isAvailable()) {
            return Result.success(fallbackReply("AI 助手暂未配置 API Key。"));
        }
        UserProfile profile = userProfileMapper.selectByUserId(request.getUserId());
        if (profile == null) {
            Map<String, Object> data = new HashMap<>();
            data.put("reply", "⚠️ 系统数据库中未找到 userId=" + request.getUserId() + " 的玩家画像，无法基于系统数据回答。");
            data.put("model", currentModel());
            return Result.success(data);
        }
        String context = buildUserContext(profile);
        String userPrompt = "[系统已为你加载玩家 #" + request.getUserId() + " 的完整画像数据]\n\n" + request.getMessage();
        List<ChatCompletionRequest.Message> history = toHistory(request.getHistory());
        String reply = aiService.chat(userPrompt, history, context);
        Map<String, Object> data = new HashMap<>();
        data.put("reply", reply);
        data.put("model", currentModel());
        data.put("userId", request.getUserId());
        return Result.success(data);
    }

    /**
     * 系统统计问答
     * <p>
     * 自动加载系统全局统计（用户数、游戏数、评分数、热门游戏等）作为上下文。
     * </p>
     */
    @PostMapping("/system-stats")
    public Result<Map<String, Object>> systemStats(@Valid @RequestBody AiChatRequest request) {
        if (!aiService.isAvailable()) {
            return Result.success(fallbackReply("AI 助手暂未配置 API Key。"));
        }
        String context = buildSystemContext();
        List<ChatCompletionRequest.Message> history = toHistory(request.getHistory());
        String reply = aiService.chat(request.getMessage(), history, context);
        Map<String, Object> data = new HashMap<>();
        data.put("reply", reply);
        data.put("model", currentModel());
        return Result.success(data);
    }

    /**
     * 兼容老接口：根据游戏名生成 AI 介绍（已不推荐使用，建议用 /game-context）
     */
    @GetMapping("/game-info")
    public Result<Map<String, Object>> gameInfo(@RequestParam("gameName") String gameName) {
        Map<String, Object> data = new HashMap<>();
        data.put("gameName", gameName);
        if (!aiService.isAvailable()) {
            data.put("description", fallbackReply("AI 助手暂未配置 API Key。").get("reply"));
            return Result.success(data);
        }
        // 尝试在系统内查找游戏
        List<Game> games = gameMapper.selectByName(gameName);
        if (games != null && !games.isEmpty()) {
            Game game = games.get(0);
            String context = buildGameContext(game);
            String prompt = "请基于提供的系统游戏数据，输出一段 80-150 字的简洁中文介绍，重点突出玩法亮点、用户评价和适合人群。仅返回介绍正文，不要分点，不要加标题。";
            String reply = aiService.chat(prompt, null, context);
            data.put("description", reply);
            data.put("source", "system-db");
        } else {
            data.put("description", "⚠️ 系统数据库中未找到游戏「" + gameName + "」的记录。");
            data.put("source", "not-found");
        }
        return Result.success(data);
    }

    // ==================== 私有方法 ====================

    private Map<String, Object> fallbackReply(String msg) {
        Map<String, Object> data = new HashMap<>();
        data.put("reply", "⚠️ " + msg);
        data.put("model", currentModel());
        return data;
    }

    private List<ChatCompletionRequest.Message> toHistory(List<AiChatRequest.HistoryItem> items) {
        if (items == null || items.isEmpty()) return null;
        return items.stream().map(h -> {
            ChatCompletionRequest.Message m = new ChatCompletionRequest.Message();
            m.setRole(h.getRole());
            m.setContent(h.getContent());
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * 构建游戏系统上下文（注入到 system prompt）
     */
    private String buildGameContext(Game g) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是 GameRec 系统的游戏详情 AI 助手。当前用户正在查看以下游戏的详情页（所有信息已为你加载到下方【系统数据】段）。\n");
        sb.append("回答规则：\n");
        sb.append("1. 所有回答必须严格基于下方【系统数据】，不得编造未提供的信息；\n");
        sb.append("2. 当用户问游戏简介/玩法/适合人群/评价时，整理下方数据给出简洁有重点的回答；\n");
        sb.append("3. 当用户问『这款游戏怎么玩』、『值不值得买』、『适合什么人』等问题时，结合系统给出的评分/类型/价格/游玩时长综合回答；\n");
        sb.append("4. 数据缺失时直接说明「系统未收录该字段」；\n");
        sb.append("5. 中文回答，结构清晰，不超过 500 字。\n\n");

        sb.append("【系统数据·游戏 ").append(g.getGameId()).append("】\n");
        sb.append("- 名称：").append(nullToUnk(g.getGameName())).append("\n");
        if (g.getGameNameCn() != null) sb.append("- 中文名：").append(g.getGameNameCn()).append("\n");
        if (g.getReleaseDate() != null) sb.append("- 发布日期：").append(g.getReleaseDate()).append("\n");
        if (g.getDeveloper() != null) sb.append("- 开发商：").append(g.getDeveloper()).append("\n");
        if (g.getPublisher() != null) sb.append("- 发行商：").append(g.getPublisher()).append("\n");
        if (g.getPlatformsCn() != null) sb.append("- 支持平台：").append(g.getPlatformsCn()).append("\n");
        else if (g.getPlatforms() != null) sb.append("- 支持平台：").append(g.getPlatforms()).append("\n");
        if (g.getRequiredAge() != null) sb.append("- 年龄限制：").append(g.getRequiredAge()).append("+\n");
        if (g.getGenresCn() != null) sb.append("- 游戏类型：").append(g.getGenresCn()).append("\n");
        else if (g.getGenres() != null) sb.append("- 游戏类型：").append(g.getGenres()).append("\n");
        if (g.getCategoriesCn() != null) sb.append("- 游戏分类：").append(g.getCategoriesCn()).append("\n");
        else if (g.getCategories() != null) sb.append("- 游戏分类：").append(g.getCategories()).append("\n");
        if (g.getSteamspyTagsCn() != null) sb.append("- SteamSpy 标签：").append(g.getSteamspyTagsCn()).append("\n");
        if (g.getAchievements() != null && g.getAchievements() > 0) sb.append("- 成就数量：").append(g.getAchievements()).append("\n");
        sb.append("- 是否免费：").append(g.getIsFree() != null && g.getIsFree() == 1 ? "免费" : ("付费 $" + (g.getPrice() != null ? g.getPrice() : 0))).append("\n");
        if (g.getPriceInitial() != null && g.getPrice() != null && !g.getPriceInitial().equals(g.getPrice())) {
            sb.append("- 初始价格：$").append(g.getPriceInitial()).append("\n");
        }
        if (g.getOwners() != null) sb.append("- 拥有者范围：").append(g.getOwners()).append("\n");
        if (g.getMetacritic() != null) sb.append("- Metacritic 评分：").append(g.getMetacritic()).append("\n");
        if (g.getTotalReviews() != null && g.getTotalReviews() > 0) {
            sb.append("- Steam 评价总数：").append(g.getTotalReviews()).append("\n");
            sb.append("- 好评数：").append(g.getTotalPositive() != null ? g.getTotalPositive() : 0).append("\n");
            sb.append("- 差评数：").append(g.getTotalNegative() != null ? g.getTotalNegative() : 0).append("\n");
            if (g.getPositivePercentual() != null) {
                sb.append("- 好评率：").append(String.format("%.1f%%", g.getPositivePercentual() * 100)).append("\n");
            }
            if (g.getReviewScoreDescCn() != null) {
                sb.append("- 评价描述：").append(g.getReviewScoreDescCn()).append("\n");
            }
        }
        if (g.getPurchaseCount() != null && g.getPurchaseCount() > 0) {
            sb.append("- 系统内购买次数：").append(g.getPurchaseCount()).append("\n");
        }
        if (g.getPlayCount() != null && g.getPlayCount() > 0) {
            sb.append("- 系统内游玩次数：").append(g.getPlayCount()).append("\n");
        }
        if (g.getAvgPlayHours() != null && g.getAvgPlayHours() > 0) {
            sb.append("- 系统内平均游玩时长：").append(String.format("%.1f", g.getAvgPlayHours())).append(" 小时\n");
        }
        if (g.getAveragePlaytime() != null && g.getAveragePlaytime() > 0) {
            sb.append("- Steam 平均游玩时长：").append(formatMinutes(g.getAveragePlaytime())).append("\n");
        }
        if (g.getMedianPlaytime() != null && g.getMedianPlaytime() > 0) {
            sb.append("- Steam 中位游玩时长：").append(formatMinutes(g.getMedianPlaytime())).append("\n");
        }
        if (g.getShortDescription() != null && !g.getShortDescription().isEmpty()) {
            sb.append("\n- 官方短简介：").append(g.getShortDescription()).append("\n");
        }
        if (g.getDetailedDescription() != null && !g.getDetailedDescription().isEmpty()) {
            String detail = g.getDetailedDescription();
            if (detail.length() > 800) detail = detail.substring(0, 800) + "...";
            sb.append("\n- 官方详细描述：").append(detail).append("\n");
        }
        return sb.toString();
    }

    /**
     * 构建玩家画像系统上下文
     */
    private String buildUserContext(UserProfile p) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是 GameRec 系统的玩家画像 AI 助手。当前用户正在查看玩家 #").append(p.getUserId()).append(" 的画像（所有信息已加载到下方【系统数据】段）。\n");
        sb.append("回答规则：\n");
        sb.append("1. 所有回答必须严格基于下方【系统数据】，不得编造；\n");
        sb.append("2. 当用户问玩家类型/消费习惯/活跃度/价值/兴趣时，整合数据给出有洞察的总结；\n");
        sb.append("3. 当用户问「这个玩家喜欢什么游戏/值不值得运营」等问题时，结合兴趣标签和活跃度综合回答；\n");
        sb.append("4. 数据缺失字段直接说「系统未收录」；\n");
        sb.append("5. 中文回答，结构清晰，不超过 500 字。\n\n");

        sb.append("【系统数据·玩家 #").append(p.getUserId()).append("】\n");
        sb.append("- 玩家类型：").append(nullToUnk(p.getPlayerType())).append("\n");
        sb.append("- 总游玩时长：").append(p.getTotalPlayHours() != null ? p.getTotalPlayHours() : 0).append(" 小时\n");
        sb.append("- 购买游戏数：").append(p.getPurchaseCount() != null ? p.getPurchaseCount() : 0).append("\n");
        sb.append("- 游玩游戏数：").append(p.getPlayCount() != null ? p.getPlayCount() : 0).append("\n");
        sb.append("- 购买-游玩转化率：").append(p.getPurchasePlayRatio() != null ? String.format("%.1f%%", p.getPurchasePlayRatio() * 100) : "--").append("\n");

        // RFM 价值分层
        int rfmTotal = (p.getRScore() != null ? p.getRScore() : 0)
                + (p.getFScore() != null ? p.getFScore() : 0)
                + (p.getMScore() != null ? p.getMScore() : 0);
        sb.append("\n【RFM 价值分层】\n");
        sb.append("- 综合价值总分：").append(rfmTotal).append(" / 12\n");
        sb.append("- 价值等级：").append(nullToUnk(p.getValueLevel())).append("\n");
        if (p.getRScore() != null) sb.append("- R（最近游玩）：").append(p.getRScore()).append(" / 4\n");
        if (p.getFScore() != null) sb.append("- F（游玩频率）：").append(p.getFScore()).append(" / 4\n");
        if (p.getMScore() != null) sb.append("- M（平均评分）：").append(p.getMScore()).append(" / 4\n");
        if (p.getRecency() != null) sb.append("- 最近游玩距今：").append(p.getRecency()).append(" 天\n");
        if (p.getFrequency() != null) sb.append("- 游玩频率：").append(p.getFrequency()).append(" 款游戏\n");
        if (p.getMonetary() != null) sb.append("- 平均评分值：").append(p.getMonetary()).append("\n");

        // 活跃度
        if (p.getActivityLevel() != null || p.getActivityNormalized() != null) {
            sb.append("\n【活跃度评估】\n");
            sb.append("- 活跃度等级：").append(nullToUnk(p.getActivityLevel())).append("\n");
            if (p.getActivityScore() != null) {
                sb.append("- 原始得分：").append(String.format("%.2f", p.getActivityScore())).append("\n");
            }
            if (p.getActivityNormalized() != null) {
                sb.append("- 归一化得分：").append(String.format("%.0f%%", p.getActivityNormalized() * 100)).append("\n");
            }
        }

        // 兴趣标签
        if (p.getInterestTags() != null && !p.getInterestTags().isEmpty()) {
            try {
                List<Map<String, Object>> tags = objectMapper.readValue(
                        p.getInterestTags(), new TypeReference<List<Map<String, Object>>>() {});
                sb.append("\n【兴趣标签 TOP").append(tags.size()).append("】\n");
                for (int i = 0; i < tags.size() && i < 15; i++) {
                    Map<String, Object> tag = tags.get(i);
                    String name = String.valueOf(tag.getOrDefault("tag", tag.getOrDefault("name", "未知")));
                    Object count = tag.get("count");
                    Object avg = tag.get("avgScore");
                    sb.append("- ").append(name);
                    if (count != null) sb.append("（").append(count).append("次");
                    if (avg != null) sb.append(" / 均分").append(avg);
                    sb.append("）\n");
                }
            } catch (Exception e) {
                log.debug("兴趣标签 JSON 解析失败", e);
            }
        }
        return sb.toString();
    }

    /**
     * 构建系统统计上下文
     */
    private String buildSystemContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是 GameRec 系统的全局统计 AI 助手。下方【系统数据】是系统当前真实统计。\n");
        sb.append("回答规则：所有回答必须基于下方数据，不得编造。中文回答，结构清晰，不超过 500 字。\n\n");
        try {
            Map<String, Object> overview = analysisService.getOverview();
            if (overview != null) {
                sb.append("【系统数据·全局统计】\n");
                sb.append("- 注册玩家：").append(overview.getOrDefault("userCount", "--")).append("\n");
                sb.append("- 游戏库总数：").append(overview.getOrDefault("gameCount", "--")).append("\n");
                sb.append("- 评分记录数：").append(overview.getOrDefault("ratingCount", "--")).append("\n");
                sb.append("- 平均评分：").append(formatNumber(overview.get("avgRating"))).append("\n");
                if (overview.get("freeGameCount") != null && overview.get("gameCount") != null) {
                    long free = ((Number) overview.get("freeGameCount")).longValue();
                    long total = ((Number) overview.get("gameCount")).longValue();
                    if (total > 0) {
                        sb.append("- 免费游戏数：").append(free).append("（占比 ").append(String.format("%.1f%%", free * 100.0 / total)).append("）\n");
                    }
                }
                if (overview.get("avgPlayHours") != null) {
                    sb.append("- 平均游玩时长：").append(String.format("%.1f", ((Number) overview.get("avgPlayHours")).doubleValue())).append(" 小时\n");
                }
            }
        } catch (Exception e) {
            log.warn("加载系统统计失败", e);
        }
        return sb.toString();
    }

    private String formatNumber(Object o) {
        if (o == null) return "--";
        if (o instanceof Number) {
            return String.format("%.2f", ((Number) o).doubleValue());
        }
        return o.toString();
    }

    private String nullToUnk(String s) {
        return (s == null || s.isEmpty()) ? "未知" : s;
    }

    private String formatMinutes(int minutes) {
        if (minutes < 60) return minutes + " 分钟";
        int h = minutes / 60;
        int m = minutes % 60;
        return m > 0 ? h + "小时" + m + "分钟" : h + " 小时";
    }

    // ==================== 请求 DTO ====================

    @Data
    public static class GameContextRequest {
        @NotNull
        private Long gameId;
        @NotBlank
        private String message;
        private List<AiChatRequest.HistoryItem> history;
    }

    @Data
    public static class UserContextRequest {
        @NotNull
        private Long userId;
        @NotBlank
        private String message;
        private List<AiChatRequest.HistoryItem> history;
    }
}
