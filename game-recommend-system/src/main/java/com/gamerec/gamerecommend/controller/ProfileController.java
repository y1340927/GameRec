package com.gamerec.gamerecommend.controller;

import com.gamerec.gamerecommend.dto.Result;
import com.gamerec.gamerecommend.dto.UserProfileDTO;
import com.gamerec.gamerecommend.entity.UserProfile;
import com.gamerec.gamerecommend.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 玩家画像控制器
 *
 * @author GameRec Team
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    /**
     * 获取RFM评分
     */
    @GetMapping("/rfm/{userId}")
    public Result<Map<String, Object>> getRFM(@PathVariable Long userId) {
        return Result.success(profileService.calculateRFM(userId));
    }

    /**
     * 获取活跃度
     */
    @GetMapping("/activity/{userId}")
    public Result<Map<String, Object>> getActivity(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0.01") double decayRate) {
        return Result.success(profileService.calculateActivity(userId, decayRate));
    }

    /**
     * 获取兴趣标签
     */
    @GetMapping("/interest/{userId}")
    public Result<List<Map<String, Object>>> getInterestTags(@PathVariable Long userId) {
        return Result.success(profileService.calculateInterestTags(userId));
    }

    /**
     * 获取玩家类型
     */
    @GetMapping("/player-type/{userId}")
    public Result<String> getPlayerType(@PathVariable Long userId) {
        return Result.success(profileService.classifyPlayerType(userId));
    }

    /**
     * 获取购买-游玩转化率
     */
    @GetMapping("/purchase-play-ratio/{userId}")
    public Result<Map<String, Object>> getPurchasePlayRatio(@PathVariable Long userId) {
        return Result.success(profileService.calculatePurchasePlayRatio(userId));
    }

    /**
     * 获取完整画像
     */
    @GetMapping("/full/{userId}")
    public Result<UserProfileDTO> getFullProfile(@PathVariable Long userId) {
        return Result.success(profileService.getFullProfile(userId));
    }

    /**
     * 保存单个用户画像
     */
    @PostMapping("/save/{userId}")
    public Result<String> saveProfile(@PathVariable Long userId) {
        profileService.saveProfile(userId);
        return Result.success("用户" + userId + "画像保存成功");
    }

    /**
     * 批量保存所有用户画像
     */
    @PostMapping("/save-all")
    public Result<Map<String, Object>> saveAllProfiles() {
        int count = profileService.saveAllProfiles();
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("savedCount", count);
        return Result.success(result);
    }

    /**
     * 分页获取画像列表
     */
    @GetMapping("/list")
    public Result<List<UserProfile>> listProfiles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(profileService.listProfiles(page, size));
    }

    /**
     * 获取画像总数
     */
    @GetMapping("/count")
    public Result<Long> getProfileCount() {
        return Result.success(profileService.getProfileCount());
    }
}
