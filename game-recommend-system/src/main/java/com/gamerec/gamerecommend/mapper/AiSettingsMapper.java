package com.gamerec.gamerecommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gamerec.gamerecommend.entity.AiSettings;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 接口设置 Mapper
 *
 * @author GameRec Team
 */
@Mapper
public interface AiSettingsMapper extends BaseMapper<AiSettings> {
}
