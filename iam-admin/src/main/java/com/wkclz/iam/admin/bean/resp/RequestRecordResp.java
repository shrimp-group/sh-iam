package com.wkclz.iam.admin.bean.resp;

import com.wkclz.web.bean.EntityResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 请求日志响应
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "请求日志响应")
public class RequestRecordResp extends EntityResp {

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

    @Schema(description = "客户端地址")
    private String remoteAddr;

    @Schema(description = "请求方式")
    private String method;

    @Schema(description = "请求协议")
    private String httpProtocol;

    @Schema(description = "请求主机")
    private String requestHost;

    @Schema(description = "请求URI")
    private String requestUri;

    @Schema(description = "查询内容")
    private String queryString;

    @Schema(description = "响应状态")
    private Integer httpStatus;

    @Schema(description = "地区")
    private String location;

    @Schema(description = "ISP运营商")
    private String isp;

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
