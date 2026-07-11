package com.wkclz.iam.contract.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 请求日志
 * 由 SsoFacadeContract.saveLog() 上报到 SSO 服务端
 *
 * @author shrimp
 */
@Data
@Schema(description = "请求日志")
public class RequestLog implements Serializable {

    @Schema(description = "租户编码")
    private String tenantCode;
    @Schema(description = "应用编码")
    private String appCode;
    @Schema(description = "UA")
    private String userAgent;
    @Schema(description = "浏览器名称")
    private String browserName;
    @Schema(description = "浏览器版本")
    private String browserVersion;
    @Schema(description = "引擎类型")
    private String engineName;
    @Schema(description = "引擎版本")
    private String engineVersion;
    @Schema(description = "用户系统")
    private String userOs;
    @Schema(description = "用户平台")
    private String userPlatform;
    @Schema(description = "请求编码")
    private String characterEncoding;
    @Schema(description = "Accept")
    private String accept;
    @Schema(description = "Accept-语言")
    private String acceptLanguage;
    @Schema(description = "Accept-编码")
    private String acceptEncoding;
    @Schema(description = "Cookie")
    private String cookie;
    @Schema(description = "Origin")
    private String origin;
    @Schema(description = "引用页")
    private String referer;
    @Schema(description = "请求协议")
    private String httpProtocol;
    @Schema(description = "请求主机")
    private String requestHost;
    @Schema(description = "请求URI")
    private String requestUri;
    @Schema(description = "查询内容")
    private String queryString;
    @Schema(description = "请求体")
    private String requestBody;
    @Schema(description = "响应体")
    private String responseBody;
    @Schema(description = "客户端地址")
    private String remoteAddr;
    @Schema(description = "请求方式")
    private String method;
    @Schema(description = "响应状态")
    private Integer httpStatus;
    @Schema(description = "用户token")
    private String token;
    @Schema(description = "用户编码")
    private String userCode;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "用户昵称")
    private String nickname;
    @Schema(description = "耗时")
    private Long costTime;
    @Schema(description = "异常信息")
    private String errorMsg;

}
