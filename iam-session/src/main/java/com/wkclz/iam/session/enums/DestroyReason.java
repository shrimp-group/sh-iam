package com.wkclz.iam.session.enums;

/**
 * 会话销毁原因枚举。
 */
public enum DestroyReason {

    /**
     * 用户主动登出
     */
    LOGOUT,

    /**
     * 用户自己修改密码
     */
    PASSWORD_CHANGED,

    /**
     * 管理员重置密码
     */
    PASSWORD_RESET_BY_ADMIN,

    /**
     * 用户被禁用
     */
    USER_DISABLED,

    /**
     * 并发会话踢出
     */
    CONCURRENT_KICK,

    /**
     * 会话自然过期
     */
    SESSION_EXPIRED

}
