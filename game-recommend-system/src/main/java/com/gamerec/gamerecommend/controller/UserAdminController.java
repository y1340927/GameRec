package com.gamerec.gamerecommend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gamerec.gamerecommend.dto.Result;
import com.gamerec.gamerecommend.entity.User;
import com.gamerec.gamerecommend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户管理控制器
 *
 * @author GameRec Team
 */
@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    @Autowired
    private UserMapper userMapper;

    /**
     * 分页查询用户列表
     */
    @GetMapping
    public Result<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like("user_id", keyword);
        }
        wrapper.orderByDesc("create_time");

        Page<User> pageResult = userMapper.selectPage(new Page<>(page, size), wrapper);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    /**
     * 根据ID查询用户详情
     */
    @GetMapping("/{userId}")
    public Result<User> getUserById(@PathVariable Long userId) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{userId}")
    public Result<String> updateUser(@PathVariable Long userId, @RequestBody User user) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        User existing = userMapper.selectOne(wrapper);
        if (existing == null) {
            return Result.error("用户不存在");
        }
        user.setId(existing.getId());
        user.setUserId(userId);
        userMapper.updateById(user);
        return Result.success("更新成功");
    }

    /**
     * 获取用户统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getUserStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", userMapper.selectCount(null));
        return Result.success(stats);
    }
}
