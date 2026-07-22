package com.wkclz.iam.session.bean;

import lombok.Data;

import java.io.Serializable;

/**
 * 会话创建结果。
 *
 * <p>包含生成的 Token 和已持久化的 Session 对象。</p>
 */
@Data
public class SessionCreateResult implements Serializable {

    /**
     * 原始 JWT Token 字符串
     */
    private String token;

    /**
     * 已持久化的 Session 对象
     */
    private Session session;

}
