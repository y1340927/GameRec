package com.gamerec.gamerecommend.dto;

/**
 * 评分导入DTO
 *
 * @author GameRec Team
 */
public class RatingImportDTO {

    private Long userId;
    private Long gameId;
    private String gameName;
    private Double playHours;

    // ===== Getters and Setters =====

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public Double getPlayHours() {
        return playHours;
    }

    public void setPlayHours(Double playHours) {
        this.playHours = playHours;
    }
}
