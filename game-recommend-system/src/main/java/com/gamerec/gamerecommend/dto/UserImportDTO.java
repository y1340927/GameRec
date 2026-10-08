package com.gamerec.gamerecommend.dto;

/**
 * 用户导入DTO
 *
 * @author GameRec Team
 */
public class UserImportDTO {

    private Long userId;
    private Integer purchaseCount;
    private Integer playCount;
    private Double totalPlayHours;
    private Double avgPlayHours;
    private Double purchasePlayRatio;

    // ===== Getters and Setters =====

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Double getPurchasePlayRatio() {
        return purchasePlayRatio;
    }

    public void setPurchasePlayRatio(Double purchasePlayRatio) {
        this.purchasePlayRatio = purchasePlayRatio;
    }
}
