package com.gamerec.gamerecommend.dto;

import java.util.List;
import java.util.Map;

/**
 * 玩家画像DTO
 *
 * 整合四个维度的画像信息：
 *   1. RFM价值分层
 *   2. 时间衰减活跃度
 *   3. 兴趣标签
 *   4. 玩家类型
 *
 * @author GameRec Team
 */
public class UserProfileDTO {

    /** 用户ID */
    private Long userId;

    /** RFM画像 */
    private Map<String, Object> rfm;

    /** 活跃度画像 */
    private Map<String, Object> activity;

    /** 兴趣标签列表 */
    private List<Map<String, Object>> interestTags;

    /** 玩家类型 */
    private String playerType;

    /** 总游玩时长（小时） */
    private Double totalPlayHours;

    /** 平均游玩时长（小时） */
    private Double avgPlayHours;

    /** 购买游戏总数 */
    private Integer purchaseCount;

    /** 游玩游戏总数 */
    private Integer playCount;

    /** 购买-游玩转化率 */
    private Double purchasePlayRatio;

    // ===== Getters and Setters =====

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Map<String, Object> getRfm() {
        return rfm;
    }

    public void setRfm(Map<String, Object> rfm) {
        this.rfm = rfm;
    }

    public Map<String, Object> getActivity() {
        return activity;
    }

    public void setActivity(Map<String, Object> activity) {
        this.activity = activity;
    }

    public List<Map<String, Object>> getInterestTags() {
        return interestTags;
    }

    public void setInterestTags(List<Map<String, Object>> interestTags) {
        this.interestTags = interestTags;
    }

    public String getPlayerType() {
        return playerType;
    }

    public void setPlayerType(String playerType) {
        this.playerType = playerType;
    }

    public Double getTotalPlayHours() {
        return totalPlayHours;
    }

    public void setTotalPlayHours(Double totalPlayHours) {
        this.totalPlayHours = totalPlayHours;
    }

    public Double getAvgPlayHours() {
        return avgPlayHours;
    }

    public void setAvgPlayHours(Double avgPlayHours) {
        this.avgPlayHours = avgPlayHours;
    }

    public Integer getPurchaseCount() {
        return purchaseCount;
    }

    public void setPurchaseCount(Integer purchaseCount) {
        this.purchaseCount = purchaseCount;
    }

    public Integer getPlayCount() {
        return playCount;
    }

    public void setPlayCount(Integer playCount) {
        this.playCount = playCount;
    }

    public Double getPurchasePlayRatio() {
        return purchasePlayRatio;
    }

    public void setPurchasePlayRatio(Double purchasePlayRatio) {
        this.purchasePlayRatio = purchasePlayRatio;
    }
}
