package com.gamerec.gamerecommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gamerec.gamerecommend.entity.Game;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 游戏Mapper
 *
 * @author GameRec Team
 */
@Mapper
public interface GameMapper extends BaseMapper<Game> {

    /**
     * 批量插入游戏
     *
     * @param gameList 游戏列表
     * @return 插入条数
     */
    int insertBatch(@Param("list") List<Game> gameList);

    /**
     * 根据 gameId（Steam appid）查询游戏
     */
    Game selectByGameId(@Param("gameId") Long gameId);

    /**
     * 根据游戏名（精确匹配英文名或中文名）查询游戏
     */
    List<Game> selectByName(@Param("name") String name);
}
