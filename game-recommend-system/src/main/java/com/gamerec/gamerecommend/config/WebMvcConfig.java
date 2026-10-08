package com.gamerec.gamerecommend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 静态资源映射：将 /images/games/** 映射到 generated-images/games/ 目录
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 游戏封面图片映射
        // /images/games/10.jpg → file:../generated-images/games/10.jpg
        registry.addResourceHandler("/images/games/**")
                .addResourceLocations("file:../generated-images/games/");
    }
}
