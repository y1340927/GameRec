package com.gamerec.gamerecommend.dto;

/**
 * 统一返回结果
 *
 * 所有REST API接口统一使用此格式返回：
 * {
 *     "code": 200,
 *     "message": "success",
 *     "data": {...},
 *     "timestamp": 1700000000000
 * }
 *
 * @param <T> 数据类型
 * @author GameRec Team
 */
public class Result<T> {

    /** 状态码：200成功，500失败 */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 实际返回数据 */
    private T data;

    /** 时间戳（毫秒） */
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功返回
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 成功返回（自定义消息）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 失败返回
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    /**
     * 失败返回（自定义状态码）
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    // ===== Getters and Setters =====

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
