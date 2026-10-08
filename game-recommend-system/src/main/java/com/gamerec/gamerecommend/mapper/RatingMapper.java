package com.gamerec.gamerecommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gamerec.gamerecommend.entity.Rating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评分Mapper
 *
 * @author GameRec Team
 */
@Mapper
public interface RatingMapper extends BaseMapper<Rating> {

    /**
     * 批量插入评分
     *
     * @param ratingList 评分列表
     * @return 插入条数
     */
    int insertBatch(@Param("list") List<Rating> ratingList);
}
