package com.gamerec.gamerecommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gamerec.gamerecommend.entity.DataImportRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据导入记录Mapper
 *
 * @author GameRec Team
 */
@Mapper
public interface DataImportRecordMapper extends BaseMapper<DataImportRecord> {
}
