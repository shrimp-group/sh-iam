package com.wkclz.iam.sdk.model;

import com.wkclz.core.annotation.Desc;
import lombok.Data;

@Data
public class RequestLog {


    @Desc("租户编码")
    private String tenantCode;
    @Desc("应用编码")
    private String appCode;
    @Desc("UA")
    private String userAgent;
    @Desc("浏览器名称")
    private String browserName;
    @Desc("浏览器版本")
    private String browserVersion;
    @Desc("引擎类型")
    private String engineName;
    @Desc("引擎版本")
    private String engineVersion;
    @Desc("用户系统")
    private String userOs;
    @Desc("用户平台")
    private String userPlatform;
    @Desc("请求编码")
    private String characterEncoding;
    @Desc("Accept")
    private String accept;
    @Desc("Accept-语言")
    private String acceptLanguage;
    @Desc("Accept-编码")
    private String acceptEncoding;
    @Desc("Cookie")
    private String cookie;
    @Desc("Origin")
    private String origin;
    @Desc("引用页")
    private String referer;
    @Desc("请求协议")
    private String httpProtocol;
    @Desc("请求 主机")
    private String requestHost;
    @Desc("请求 URI")
    private String requestUri;
    @Desc("查询内容")
    private String queryString;
    @Desc("请求体")
    private String requestBody;
    @Desc("响应体")
    private String responseBody;
    @Desc("客户端地址")
    private String remoteAddr;
    @Desc("请求方式")
    private String method;
    @Desc("响应状态")
    private Integer httpStatus;
    @Desc("用户token")
    private String token;
    @Desc("用户编码")
    private String userCode;
    @Desc("用户名")
    private String username;
    @Desc("用户昵称")
    private String nickname;
    @Desc("耗时")
    private Long costTime;
    @Desc("异常信息")
    private String errorMsg;


}
