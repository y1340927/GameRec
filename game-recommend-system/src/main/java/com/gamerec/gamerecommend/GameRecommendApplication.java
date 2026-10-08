package com.gamerec.gamerecommend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 基于玩家画像的游戏推荐系统 - GameRec
 * Spring Boot 启动类
 *
 * @author GameRec Team
 * @since 2026-07-04
 */
@SpringBootApplication
public class GameRecommendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameRecommendApplication.class, args);
        System.out.println("========================================");
        System.out.println("  GameRec 智能游戏推荐系统启动成功！");
        System.out.println("  http://localhost:8080");
        System.out.println("========================================");
    }
}
