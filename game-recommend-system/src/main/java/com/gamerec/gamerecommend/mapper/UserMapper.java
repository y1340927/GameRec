package com.gamerec.gamerecommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gamerec.gamerecommend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户Mapper
 *
 * @author GameRec Team
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 批量插入用户
     *
     * @param userList 用户列表
     * @return 插入条数
     */
    int insertBatch(@Param("list") List<User> userList);
}
