package com.chronomo.services.constant;

/**
 * <p>ResultCodeEnum class.</p>
 *
 * @author liujinghui9
 * @version $Id: $Id
 */
public enum ResultCodeEnum {
    /**
     * 操作成功
     */
    SUCCESS("操作成功", 200),
    /**
     * 操作失败
     */
    ERROR("操作失败", 5000),
    /**
     * 数据验证错
     */
    VALIDATE_ERROR("数据验证错", 5001),
    /**
     * 非法参数
     */
    PARAMETER_ILLEGAL("非法参数", 5002),
    /**
     * 需要登录
     */
    NEED_LOGIN("需要登录", 6001),
    /**
     * 身份验证错误
     */
    AUTH_ERROR("身份验证错", 6002),
    /**
     * 权限校验错
     */
    PERMISSIONED_ERROR("权限校验错", 6003);
    // 成员变量
    /**
     * <p>Getter for the field <code>msg</code>.</p>
     *
     * @return a {@link String} object.
     */
    /**
     * <p>Getter for the field <code>msg</code>.</p>
     *
     * @return a {@link String} object.
     */
    private String msg;
    private Integer code;

    public String getMsg() {
        return msg;
    }

    /**
     * <p>Setter for the field <code>msg</code>.</p>
     *
     * @param msg a {@link String} object.
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * <p>Getter for the field <code>code</code>.</p>
     *
     * @return a {@link Integer} object.
     */
    public Integer getCode() {
        return code;
    }

    /**
     * <p>Setter for the field <code>code</code>.</p>
     *
     * @param code a {@link Integer} object.
     */
    public void setCode(Integer code) {
        this.code = code;
    }

    // 构造方法
    private ResultCodeEnum(String msg, int code) {
        this.msg = msg;
        this.code = code;
    }

    /**
     * <p>Getter for the field <code>msg</code>.</p>
     *
     * @param code a {@link Integer} object.
     * @return a {@link String} object.
     */
    public static String getMsg(Integer code) {
        for (ResultCodeEnum c : ResultCodeEnum.values()) {
            if (c.code.equals(code)) {
                return c.msg;
            }
        }
        return null;
    }

}
