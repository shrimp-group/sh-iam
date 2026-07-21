package com.wkclz.iam.session.remote;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 远程客户端配置 — 第三方应用通过 HTTP 调用 SSO 服务端时使用。
 *
 * <p>仅当配置了 {@code iam.session.remote.server-url} 时生效，
 * 与 {@link RemoteSsoFacadeImpl} 和 {@link RemoteRequestRecordHandler} 的条件装配联动。</p>
 *
 * <p>历史：从 iam-sdk 的 IamSdkConfig 拆分迁入，仅保留远程调用相关配置项。
 * 原有 jwt.secret-key 已由 IamSessionConfig 替代，app-code 由请求头传递，enabled 开关已删除。</p>
 *
 * <p>配置项绑定前缀：{@code iam.session.remote}</p>
 * <ul>
 *   <li>{@code iam.session.remote.server-url} —— SSO 服务端地址（必填，条件装配触发器）</li>
 *   <li>{@code iam.session.remote.app-id} —— AK 鉴权应用 ID</li>
 *   <li>{@code iam.session.remote.app-secret} —— AK 鉴权应用密钥（RSA 私钥）</li>
 *   <li>{@code iam.session.remote.static.enabled} —— 静态资源过滤开关</li>
 *   <li>{@code iam.session.remote.static.subfix} —— 静态资源后缀</li>
 * </ul>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "iam.session.remote")
@ConditionalOnProperty(name = "iam.session.remote.server-url")
public class RemoteClientConfig {

    /**
     * SSO 服务端地址（必填，条件装配触发器）
     */
    private String serverUrl;

    /**
     * AK 鉴权应用 ID
     */
    private String appId;

    /**
     * AK 鉴权应用密钥（RSA 私钥）
     */
    private String appSecret;

    /**
     * 静态资源过滤配置
     */
    private StaticConfig staticConfig = new StaticConfig();

    @Data
    public static class StaticConfig {
        /**
         * 静态资源过滤开关
         */
        private String enabled = "false";

        /**
         * 静态资源后缀（| 分隔的正则片段）
         */
        private String subfix = "js|css|jpg|png|mp3|html|htm|jpeg|ttf|woff|ico|woff2|map";
    }
}
