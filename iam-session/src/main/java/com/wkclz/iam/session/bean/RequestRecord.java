package com.wkclz.iam.session.bean;

import lombok.Data;

import java.io.Serializable;

/**
 * 请求日志数据载体 — 用于在 RequestLogFilter 和 SPI 实现之间传递请求/响应日志信息。
 *
 * <p>纯 JDK 依赖，不依赖任何框架或模块。字段与 {@code IamRequestLog} 实体对应，
 * 由 SPI 实现方负责 UA 解析、IP 归属地查询和持久化。</p>
 *
 * @see com.wkclz.iam.session.spi.RequestRecordHandler
 */
@Data
public class RequestRecord implements Serializable {

    // ========== 基础请求信息 ==========
    private String method;
    private String requestUri;
    private String queryString;
    private String requestBody;
    private String httpProtocol;

    // ========== 浏览器信息 ==========
    private String browserName;
    private String browserVersion;
    private String engineName;
    private String engineVersion;

    // ========== 客户端平台信息 ==========
    private String userOs;
    private String userPlatform;

    // ========== 响应信息 ==========
    private Integer httpStatus;
    private String responseBody;

    // ========== 用户信息 ==========
    private String userCode;
    private String username;
    private String nickname;
    private String token;

    // ========== 网络信息 ==========
    private String remoteAddr;
    private String requestHost;
    private String characterEncoding;

    // ========== 请求头 ==========
    private String userAgent;
    private String accept;
    private String acceptLanguage;
    private String acceptEncoding;
    private String cookie;
    private String origin;
    private String referer;

    // ========== 应用/租户 ==========
    private String tenantCode;
    private String appCode;

    // ========== 性能/错误 ==========
    private Long costTime;
    private String errorMsg;

}
