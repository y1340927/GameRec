package com.gamerec.gamerecommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gamerec.gamerecommend.entity.RatingTrain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 训练集评分 Mapper
 *
 * 【问题 2.2】训练侧唯一数据源，禁止在推荐算法中改用 RatingMapper 读取全量 rating。
 *
 * @author GameRec Team
 */
@Mapper
public interface RatingTrainMapper extends BaseMapper<RatingTrain> {

    /**
     * 批量插入训练集
     *
     * @param list 训练集记录
     * @return 插入条数
     */
    int insertBatch(@Param("list") List<RatingTrain> list);

    /**
     * 物理清空训练集（重新划分时使用；BaseMapper.delete 受逻辑删除影响，不可用）
     */
    void truncate();
}
