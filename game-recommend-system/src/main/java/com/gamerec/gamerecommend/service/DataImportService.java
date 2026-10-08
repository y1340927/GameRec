package com.gamerec.gamerecommend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gamerec.gamerecommend.dto.GameImportDTO;
import com.gamerec.gamerecommend.dto.RatingImportDTO;
import com.gamerec.gamerecommend.dto.UserImportDTO;
import com.gamerec.gamerecommend.entity.*;
import com.gamerec.gamerecommend.mapper.*;
import com.gamerec.gamerecommend.util.RatingConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据导入服务
 *
 * 支持三种数据类型的导入：
 *   1. 用户数据导入
 *   2. 游戏数据导入
 *   3. 评分数据导入（含自动联动更新画像）
 *
 * @author GameRec Team
 */
@Slf4j
@Service
public class DataImportService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GameMapper gameMapper;

    @Autowired
    private RatingMapper ratingMapper;

    @Autowired
    private DataImportRecordMapper importRecordMapper;

    @Autowired
    private ProfileService profileService;

    /**
     * 导入用户数据
     *
     * @param users 用户DTO列表
     * @return 导入统计
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importUsers(List<UserImportDTO> users) {
        int inserted = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();

        for (UserImportDTO dto : users) {
            try {
                if (dto.getUserId() == null) {
                    errors.add("用户ID不能为空");
                    continue;
                }

                QueryWrapper<User> wrapper = new QueryWrapper<>();
                wrapper.eq("user_id", dto.getUserId());
                User existing = userMapper.selectOne(wrapper);

                User user = new User();
                user.setUserId(dto.getUserId());
                user.setPurchaseCount(dto.getPurchaseCount() != null ? dto.getPurchaseCount() : 0);
                user.setPlayCount(dto.getPlayCount() != null ? dto.getPlayCount() : 0);
                user.setTotalPlayHours(dto.getTotalPlayHours() != null ? dto.getTotalPlayHours() : 0.0);
                user.setAvgPlayHours(dto.getAvgPlayHours() != null ? dto.getAvgPlayHours() : 0.0);
                user.setPurchasePlayRatio(dto.getPurchasePlayRatio() != null ? dto.getPurchasePlayRatio() : 0.0);

                if (existing != null) {
                    user.setId(existing.getId());
                    userMapper.updateById(user);
                    updated++;
                } else {
                    userMapper.insert(user);
                    inserted++;
                }
            } catch (Exception e) {
                errors.add("用户" + dto.getUserId() + "导入失败: " + e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inserted", inserted);
        result.put("updated", updated);
        result.put("errors", errors);
        result.put("total", users.size());
        result.put("success", inserted + updated);

        return result;
    }

    /**
     * 导入游戏数据
     *
     * @param games 游戏DTO列表
     * @return 导入统计
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importGames(List<GameImportDTO> games) {
        int inserted = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();

        for (GameImportDTO dto : games) {
            try {
                if (dto.getGameId() == null) {
                    errors.add("游戏ID不能为空");
                    continue;
                }

                QueryWrapper<Game> wrapper = new QueryWrapper<>();
                wrapper.eq("game_id", dto.getGameId());
                Game existing = gameMapper.selectOne(wrapper);

                Game game = new Game();
                game.setGameId(dto.getGameId());
                game.setGameName(dto.getGameName());
                game.setGameNameCn(dto.getGameNameCn());
                game.setDeveloper(dto.getDeveloper());
                game.setPublisher(dto.getPublisher());
                game.setGenres(dto.getGenres());
                game.setPrice(dto.getPrice());
                game.setIsFree(dto.getIsFree() != null ? dto.getIsFree() : 0);

                if (existing != null) {
                    game.setId(existing.getId());
                    gameMapper.updateById(game);
                    updated++;
                } else {
                    gameMapper.insert(game);
                    inserted++;
                }
            } catch (Exception e) {
                errors.add("游戏" + dto.getGameId() + "导入失败: " + e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inserted", inserted);
        result.put("updated", updated);
        result.put("errors", errors);
        result.put("total", games.size());
        result.put("success", inserted + updated);

        return result;
    }

    /**
     * 导入评分数据（含自动联动更新画像）
     *
     * @param ratings 评分DTO列表
     * @return 导入统计
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importRatings(List<RatingImportDTO> ratings) {
        int inserted = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();
        Set<Long> affectedUsers = new HashSet<>();

        for (RatingImportDTO dto : ratings) {
            try {
                if (dto.getUserId() == null || dto.getGameId() == null) {
                    errors.add("用户ID和游戏ID不能为空");
                    continue;
                }

                // 校验用户是否存在
                QueryWrapper<User> userWrapper = new QueryWrapper<>();
                userWrapper.eq("user_id", dto.getUserId());
                if (userMapper.selectCount(userWrapper) == 0) {
                    errors.add("用户" + dto.getUserId() + "不存在");
                    continue;
                }

                // 校验游戏是否存在
                QueryWrapper<Game> gameWrapper = new QueryWrapper<>();
                gameWrapper.eq("game_id", dto.getGameId());
                if (gameMapper.selectCount(gameWrapper) == 0) {
                    errors.add("游戏" + dto.getGameId() + "不存在");
                    continue;
                }

                // 检查是否已有该用户对该游戏的评分
                QueryWrapper<Rating> ratingWrapper = new QueryWrapper<>();
                ratingWrapper.eq("user_id", dto.getUserId())
                        .eq("game_id", dto.getGameId());
                Rating existing = ratingMapper.selectOne(ratingWrapper);

                double playHours = dto.getPlayHours() != null ? dto.getPlayHours() : 0;
                int pseudoRating = RatingConvertUtil.playHoursToRating(playHours);

                Rating rating = new Rating();
                rating.setUserId(dto.getUserId());
                rating.setGameId(dto.getGameId());
                rating.setGameName(dto.getGameName());
                rating.setRating(pseudoRating);
                rating.setPlayHours(playHours);
                rating.setIsValid(1);

                if (existing != null) {
                    rating.setId(existing.getId());
                    ratingMapper.updateById(rating);
                    updated++;
                } else {
                    ratingMapper.insert(rating);
                    inserted++;
                }

                affectedUsers.add(dto.getUserId());
            } catch (Exception e) {
                errors.add("评分导入失败: " + e.getMessage());
            }
        }

        // 自动更新受影响用户的画像
        for (Long userId : affectedUsers) {
            try {
                profileService.saveProfile(userId);
            } catch (Exception e) {
                log.warn("自动更新用户{}画像失败: {}", userId, e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inserted", inserted);
        result.put("updated", updated);
        result.put("errors", errors);
        result.put("total", ratings.size());
        result.put("success", inserted + updated);
        result.put("affectedUsers", affectedUsers.size());

        return result;
    }

    /**
     * 记录数据导入日志
     */
    public void recordImport(String dataType, String fileName, int importCount,
                             int inserted, int updated, int skipped, int failed,
                             LocalDateTime startTime, LocalDateTime endTime, String errorMsg) {
        DataImportRecord record = new DataImportRecord();
        record.setDataType(dataType);
        record.setFileName(fileName);
        record.setImportCount(importCount);
        record.setInsertedCount(inserted);
        record.setUpdatedCount(updated);
        record.setSkippedCount(skipped);
        record.setFailedCount(failed);
        record.setErrorMsg(errorMsg);
        record.setStartTime(startTime);
        record.setEndTime(endTime);
        record.setDurationSeconds(
                java.time.Duration.between(startTime, endTime).toMillis() / 1000.0);

        importRecordMapper.insert(record);
    }

    /**
     * 获取导入记录列表
     */
    public List<DataImportRecord> getImportRecords() {
        QueryWrapper<DataImportRecord> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        return importRecordMapper.selectList(wrapper);
    }
}
