package com.gamerec.gamerecommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gamerec.gamerecommend.entity.RatingTest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 测试集评分 Mapper
 *
 * 【问题 2.2】评估阶段"用户实际喜欢集合"的唯一数据源，与训练集物理隔离。
 *
 * @author GameRec Team
 */
@Mapper
public interface RatingTestMapper extends BaseMapper<RatingTest> {

    /**
     * 批量插入测试集
     *
     * @param list 测试集记录
     * @return 插入条数
     */
    int insertBatch(@Param("list") List<RatingTest> list);

    /**
     * 物理清空测试集（重新划分时使用；BaseMapper.delete 受逻辑删除影响，不可用）
     */
    void truncate();
}
