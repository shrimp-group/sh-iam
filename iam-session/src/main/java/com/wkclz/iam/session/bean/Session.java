package com.wkclz.iam.session.bean;

import com.wkclz.iam.session.enums.AuthType;
import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 会话领域模型。
 *
 * <p>sessionId = MD5(token)，固定 32 字符，方便 Redis 管理工具浏览。
 * Hash 内存储原始 token 字段用于反向定位。</p>
 * <p>clientIp / userAgent 不存储在 Session 中，由 SSO 层每次请求从 HttpServletRequest 实时获取。</p>
 */
@Data
public class Session implements Serializable {

    /**
     * 会话唯一标识（MD5(token)，固定 32 字符）
     */
    private String sessionId;

    /**
     * 用户标识（userCode）
     */
    private String subjectId;

    /**
     * 认证方式
     */
    private AuthType authType;

    /**
     * 原始 JWT Token（存储于 Hash 中，用于反向定位）
     */
    private String token;

    /**
     * UserIdentity 的 JSON 序列化
     */
    private String userIdentity;

    /**
     * 创建时间戳（毫秒）
     */
    private long createTime;

    /**
     * 过期时间戳（毫秒）
     */
    private long expireTime;

    /**
     * 扩展属性
     */
    private Map<String, String> metadata = new LinkedHashMap<>();

    /**
     * 便捷添加单个扩展属性。
     */
    public void addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new LinkedHashMap<>();
        }
        this.metadata.put(key, value);
    }

}
