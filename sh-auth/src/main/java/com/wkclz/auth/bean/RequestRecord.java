package com.wkclz.auth.bean;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 请求日志 */
@Data
public class RequestRecord implements Serializable {
    private String tenantCode;
    private String appCode;
    private String userAgent;
    private String browserName;
    private String browserVersion;
    private String engineName;
    private String engineVersion;
    private String userOs;
    private String userPlatform;
    private String characterEncoding;
    private String accept;
    private String acceptLanguage;
    private String acceptEncoding;
    private String cookie;
    private String origin;
    private String referer;
    private String httpProtocol;
    private String requestHost;
    private String requestUri;
    private String queryString;
    private String requestBody;
    private String responseBody;
    private String remoteAddr;
    private String method;
    private Integer httpStatus;
    private String token;
    private String userCode;
    private String username;
    private String nickname;
    private String location;
    private String isp;
    private Long costTime;
    private LocalDateTime requestTime;
    private String errorMsg;
}
