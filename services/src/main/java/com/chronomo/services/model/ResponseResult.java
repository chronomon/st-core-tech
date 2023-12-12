package com.chronomo.services.model;

import com.chronomo.services.constant.ResultCodeEnum;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 响应实体类
 *
 * @author jimo
 * @date 2019-09-04
 */
@Data
@NoArgsConstructor
@ToString
public class ResponseResult<T> {
    /**
     * 状态码
     */
    private int resultCode;
    /**
     * 新版状态码
     */
    private int code;

    /**
     * 信息
     */
    private String resultMsg;

    /**
     * 新版信息
     */
    private String message;

    /**
     * 时间戳
     */
    private Long tid;

    /**
     * 数据
     */
    private T data;

    public ResponseResult(int code, String message, Long tid, T data) {
        this.resultCode = code;
        this.resultMsg = message;
        this.code = code;
        this.message = message;
        this.tid = tid;
        this.data = data;
    }

    /**
     * 默认方法，返回200状态位，无信息，无数据
     */
    public static <T> ResponseResult<T> create() {
        return new ResponseResult<>(ResultCodeEnum.SUCCESS.getCode(), null, System.currentTimeMillis(), null);
    }

    /**
     * 返回500的静态方法
     *
     * @param message 信息
     */
    public static <T> ResponseResult<T> fail(String message) {
        return new ResponseResult<>(ResultCodeEnum.ERROR.getCode(), message, System.currentTimeMillis(), null);
    }

    /**
     * 失败
     *
     * @param code    状态码
     * @param message 消息
     */
    public static <T> ResponseResult<T> fail(Integer code, String message) {
        return new ResponseResult<>(code, message, System.currentTimeMillis(), null);
    }

    /**
     * 重载create方法，增加状态码和数据
     *
     * @param code 状态码
     * @param data 数据
     */
    public static <T> ResponseResult<T> create(int code, T data) {
        return new ResponseResult<>(code, null, System.currentTimeMillis(), data);
    }

    /**
     * 重载create方法，增加状态码和消息参数
     *
     * @param code    状态码
     * @param message 消息
     */
    public static <T> ResponseResult<T> create(int code, String message) {
        return new ResponseResult<>(code, message, System.currentTimeMillis(), null);
    }

    /**
     * 返回状态码200，同时携带数据
     *
     * @param data 数据
     */
    public static <T> ResponseResult<T> create(T data) {
        return new ResponseResult<>(ResultCodeEnum.SUCCESS.getCode(), null, System.currentTimeMillis(), data);
    }

    /**
     * 重载create方法，增加状态码和消息参数和数据
     *
     * @param code    状态码
     * @param message 消息
     * @param data    数据
     */
    public static <T> ResponseResult<T> create(int code, String message, T data) {
        return new ResponseResult<>(code, message, System.currentTimeMillis(), data);
    }

    public static <T> ResponseResult<T> success(String resultMsg, T data) {
        return new ResponseResult<>(200, resultMsg, System.currentTimeMillis(), data);
    }

    public static <T> ResponseResult<T> success(T data) {
        return new ResponseResult<>(200, null, System.currentTimeMillis(), data);
    }
}
