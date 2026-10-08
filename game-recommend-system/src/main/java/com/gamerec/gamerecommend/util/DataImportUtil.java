package com.gamerec.gamerecommend.util;

import com.gamerec.gamerecommend.entity.Game;
import com.gamerec.gamerecommend.entity.Rating;
import com.gamerec.gamerecommend.entity.User;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

/**
 * 数据导入工具类
 *
 * 功能：
 *   - 读取CSV文件并解析
 *   - 批量构建实体对象
 *   - 分批调用Mapper批量插入
 *
 * 支持的数据文件：
 *   1. steam.csv        → Game实体
 *   2. steam_games.csv  → Game实体（扩展信息更新）
 *   3. simulated_user_behaviors.csv → User实体 + Rating实体（含伪评分转化）
 *
 * @author GameRec Team
 */
@Slf4j
public class DataImportUtil {

    /** 批量插入大小 */
    private static final int BATCH_SIZE = 1000;

    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 解析CSV行（处理引号内的逗号）
     *
     * @param line CSV行
     * @return 字段数组
     */
    public static String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentField = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString().trim());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        fields.add(currentField.toString().trim());
        return fields.toArray(new String[0]);
    }

    /**
     * 通用CSV读取方法
     *
     * @param filePath   CSV文件路径
     * @param skipHeader 是否跳过表头
     * @param processor  行处理器（Consumer模式）
     * @return 读取的总行数
     */
    public static int readCsv(String filePath, boolean skipHeader, Consumer<String[]> processor) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line;
            if (skipHeader) {
                reader.readLine(); // 跳过表头
            }
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] fields = parseCsvLine(line);
                processor.accept(fields);
                count++;
            }
        } catch (Exception e) {
            log.error("读取CSV文件失败: {}", filePath, e);
            throw new RuntimeException("读取CSV文件失败: " + filePath, e);
        }
        return count;
    }

    /**
     * 安全解析整数
     */
    public static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 安全解析Long
     */
    public static long parseLong(String value, long defaultValue) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 安全解析Double
     */
    public static double parseDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 安全解析日期
     */
    public static LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty() || "Not Released".equalsIgnoreCase(value.trim())) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 安全截断字符串
     */
    public static String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    /**
     * 批量执行插入
     *
     * @param list     实体列表
     * @param batchInserter 批量插入函数
     * @param <T>      实体类型
     * @return 插入条数
     */
    public static <T> int executeBatch(List<T> list, Consumer<List<T>> batchInserter) {
        int total = 0;
        for (int i = 0; i < list.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, list.size());
            List<T> batch = list.subList(i, end);
            batchInserter.accept(batch);
            total += batch.size();
            if (total % 10000 == 0) {
                log.info("  已导入: {} / {}", total, list.size());
            }
        }
        return total;
    }

    /**
     * 解析 simulated_user_behaviors.csv 中的一行，构建 Rating 实体
     *
     * 字段顺序：user_id, game_id, game_name, purchase, play_hours
     *
     * @param fields CSV行字段数组
     * @return Rating实体（含伪评分转化）
     */
    public static Rating parseRating(String[] fields) {
        Rating rating = new Rating();
        rating.setUserId(parseLong(fields[0], 0));
        rating.setGameId(parseLong(fields[1], 0));
        rating.setGameName(truncate(fields[2], 500));
        double playHours = parseDouble(fields[4], 0);
        rating.setPlayHours(playHours);
        // 核心：隐式反馈→伪评分转化
        rating.setRating(RatingConvertUtil.playHoursToRating(playHours));
        rating.setIsValid(1);
        return rating;
    }

    /**
     * 解析 simulated_user_behaviors.csv，构建 User 实体（按user_id聚合）
     *
     * @param ratings 所有Rating记录
     * @return User实体列表
     */
    public static List<User> buildUsers(List<Rating> ratings) {
        // 按user_id分组统计
        Map<Long, List<Rating>> userRatings = new HashMap<>();
        for (Rating r : ratings) {
            userRatings.computeIfAbsent(r.getUserId(), k -> new ArrayList<>()).add(r);
        }

        List<User> users = new ArrayList<>();
        for (Map.Entry<Long, List<Rating>> entry : userRatings.entrySet()) {
            Long userId = entry.getKey();
            List<Rating> userRatingList = entry.getValue();

            int purchaseCount = userRatingList.size();
            int playCount = 0;
            double totalPlayHours = 0;

            for (Rating r : userRatingList) {
                if (r.getPlayHours() > 0) {
                    playCount++;
                }
                totalPlayHours += r.getPlayHours();
            }

            User user = new User();
            user.setUserId(userId);
            user.setPurchaseCount(purchaseCount);
            user.setPlayCount(playCount);
            user.setTotalPlayHours(totalPlayHours);
            user.setAvgPlayHours(playCount > 0 ? Math.round(totalPlayHours / playCount * 100.0) / 100.0 : 0);
            user.setPurchasePlayRatio(purchaseCount > 0 ? Math.round((double) playCount / purchaseCount * 10000.0) / 10000.0 : 0);
            users.add(user);
        }
        return users;
    }
}
