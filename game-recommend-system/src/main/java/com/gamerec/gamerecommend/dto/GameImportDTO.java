package com.gamerec.gamerecommend.dto;

/**
 * 游戏导入DTO
 *
 * @author GameRec Team
 */
public class GameImportDTO {

    private Long gameId;
    private String gameName;
    private String gameNameCn;
    private String developer;
    private String publisher;
    private String genres;
    private String genresCn;
    private Double price;
    private Integer isFree;
    private String releaseDate;

    // ===== Getters and Setters =====

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

    public String getGameNameCn() {
        return gameNameCn;
    }

    public void setGameNameCn(String gameNameCn) {
        this.gameNameCn = gameNameCn;
    }

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getGenres() {
        return genres;
    }

    public void setGenres(String genres) {
        this.genres = genres;
    }

    public String getGenresCn() {
        return genresCn;
    }

    public void setGenresCn(String genresCn) {
        this.genresCn = genresCn;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getIsFree() {
        return isFree;
    }

    public void setIsFree(Integer isFree) {
        this.isFree = isFree;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }
}
