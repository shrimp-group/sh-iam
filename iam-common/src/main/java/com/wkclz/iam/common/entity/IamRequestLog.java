package com.wkclz.iam.common.entity;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;




/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_request_log (系统请求日志) 重新生成代码会覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamRequestLog extends BaseEntity {

    /**
     * 租户编码
     */
    @Desc("租户编码")
    private String tenantCode;

    /**
     * 应用编码
     */
    @Desc("应用编码")
    private String appCode;

    /**
     * UA
     */
    @Desc("UA")
    private String userAgent;

    /**
     * 浏览器名称
     */
    @Desc("浏览器名称")
    private String browserName;

    /**
     * 浏览器版本
     */
    @Desc("浏览器版本")
    private String browserVersion;

    /**
     * 引擎类型
     */
    @Desc("引擎类型")
    private String engineName;

    /**
     * 引擎版本
     */
    @Desc("引擎版本")
    private String engineVersion;

    /**
     * 用户系统
     */
    @Desc("用户系统")
    private String userOs;

    /**
     * 用户平台
     */
    @Desc("用户平台")
    private String userPlatform;

    /**
     * 请求编码
     */
    @Desc("请求编码")
    private String characterEncoding;

    /**
     * Accept
     */
    @Desc("Accept")
    private String accept;

    /**
     * Accept-语言
     */
    @Desc("Accept-语言")
    private String acceptLanguage;

    /**
     * Accept-编码
     */
    @Desc("Accept-编码")
    private String acceptEncoding;

    /**
     * Cookie
     */
    @Desc("Cookie")
    private String cookie;

    /**
     * Origin
     */
    @Desc("Origin")
    private String origin;

    /**
     * 引用页
     */
    @Desc("引用页")
    private String referer;

    /**
     * 客户端地址
     */
    @Desc("客户端地址")
    private String remoteAddr;

    /**
     * 请求方式
     */
    @Desc("请求方式")
    private String method;

    /**
     * 请求协议
     */
    @Desc("请求协议")
    private String httpProtocol;

    /**
     * 请求 主机
     */
    @Desc("请求 主机")
    private String requestHost;

    /**
     * 请求 URI
     */
    @Desc("请求 URI")
    private String requestUri;

    /**
     * 查询内容
     */
    @Desc("查询内容")
    private String queryString;

    /**
     * 请求体
     */
    @Desc("请求体")
    private String requestBody;

    /**
     * 响应体
     */
    @Desc("响应体")
    private String responseBody;

    /**
     * 响应状态
     */
    @Desc("响应状态")
    private Integer httpStatus;

    /**
     * 地区
     */
    @Desc("地区")
    private String location;

    /**
     * ISP运营商
     */
    @Desc("ISP运营商")
    private String isp;

    /**
     * 用户token
     */
    @Desc("用户token")
    private String token;

    /**
     * 用户编码
     */
    @Desc("用户编码")
    private String userCode;

    /**
     * 用户名
     */
    @Desc("用户名")
    private String username;

    /**
     * 用户昵称
     */
    @Desc("用户昵称")
    private String nickname;

    /**
     * 耗时
     */
    @Desc("耗时")
    private Long costTime;

    /**
     * 异常信息
     */
    @Desc("异常信息")
    private String errorMsg;


    public static IamRequestLog copy(IamRequestLog source, IamRequestLog target) {
        if (target == null ) { target = new IamRequestLog();}
        if (source == null) { return target; }
        target.setId(source.getId());
        target.setTenantCode(source.getTenantCode());
        target.setAppCode(source.getAppCode());
        target.setUserAgent(source.getUserAgent());
        target.setBrowserName(source.getBrowserName());
        target.setBrowserVersion(source.getBrowserVersion());
        target.setEngineName(source.getEngineName());
        target.setEngineVersion(source.getEngineVersion());
        target.setUserOs(source.getUserOs());
        target.setUserPlatform(source.getUserPlatform());
        target.setCharacterEncoding(source.getCharacterEncoding());
        target.setAccept(source.getAccept());
        target.setAcceptLanguage(source.getAcceptLanguage());
        target.setAcceptEncoding(source.getAcceptEncoding());
        target.setCookie(source.getCookie());
        target.setOrigin(source.getOrigin());
        target.setReferer(source.getReferer());
        target.setRemoteAddr(source.getRemoteAddr());
        target.setMethod(source.getMethod());
        target.setHttpProtocol(source.getHttpProtocol());
        target.setRequestHost(source.getRequestHost());
        target.setRequestUri(source.getRequestUri());
        target.setQueryString(source.getQueryString());
        target.setRequestBody(source.getRequestBody());
        target.setResponseBody(source.getResponseBody());
        target.setHttpStatus(source.getHttpStatus());
        target.setLocation(source.getLocation());
        target.setIsp(source.getIsp());
        target.setToken(source.getToken());
        target.setUserCode(source.getUserCode());
        target.setUsername(source.getUsername());
        target.setNickname(source.getNickname());
        target.setCostTime(source.getCostTime());
        target.setErrorMsg(source.getErrorMsg());
        target.setSort(source.getSort());
        target.setCreateTime(source.getCreateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateTime(source.getUpdateTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setRemark(source.getRemark());
        target.setVersion(source.getVersion());
        return target;
    }

    public static IamRequestLog copyIfNotNull(IamRequestLog source, IamRequestLog target) {
        if (target == null ) { target = new IamRequestLog();}
        if (source == null) { return target; }
        if (source.getId() != null) { target.setId(source.getId()); }
        if (source.getTenantCode() != null) { target.setTenantCode(source.getTenantCode()); }
        if (source.getAppCode() != null) { target.setAppCode(source.getAppCode()); }
        if (source.getUserAgent() != null) { target.setUserAgent(source.getUserAgent()); }
        if (source.getBrowserName() != null) { target.setBrowserName(source.getBrowserName()); }
        if (source.getBrowserVersion() != null) { target.setBrowserVersion(source.getBrowserVersion()); }
        if (source.getEngineName() != null) { target.setEngineName(source.getEngineName()); }
        if (source.getEngineVersion() != null) { target.setEngineVersion(source.getEngineVersion()); }
        if (source.getUserOs() != null) { target.setUserOs(source.getUserOs()); }
        if (source.getUserPlatform() != null) { target.setUserPlatform(source.getUserPlatform()); }
        if (source.getCharacterEncoding() != null) { target.setCharacterEncoding(source.getCharacterEncoding()); }
        if (source.getAccept() != null) { target.setAccept(source.getAccept()); }
        if (source.getAcceptLanguage() != null) { target.setAcceptLanguage(source.getAcceptLanguage()); }
        if (source.getAcceptEncoding() != null) { target.setAcceptEncoding(source.getAcceptEncoding()); }
        if (source.getCookie() != null) { target.setCookie(source.getCookie()); }
        if (source.getOrigin() != null) { target.setOrigin(source.getOrigin()); }
        if (source.getReferer() != null) { target.setReferer(source.getReferer()); }
        if (source.getRemoteAddr() != null) { target.setRemoteAddr(source.getRemoteAddr()); }
        if (source.getMethod() != null) { target.setMethod(source.getMethod()); }
        if (source.getHttpProtocol() != null) { target.setHttpProtocol(source.getHttpProtocol()); }
        if (source.getRequestHost() != null) { target.setRequestHost(source.getRequestHost()); }
        if (source.getRequestUri() != null) { target.setRequestUri(source.getRequestUri()); }
        if (source.getQueryString() != null) { target.setQueryString(source.getQueryString()); }
        if (source.getRequestBody() != null) { target.setRequestBody(source.getRequestBody()); }
        if (source.getResponseBody() != null) { target.setResponseBody(source.getResponseBody()); }
        if (source.getHttpStatus() != null) { target.setHttpStatus(source.getHttpStatus()); }
        if (source.getLocation() != null) { target.setLocation(source.getLocation()); }
        if (source.getIsp() != null) { target.setIsp(source.getIsp()); }
        if (source.getToken() != null) { target.setToken(source.getToken()); }
        if (source.getUserCode() != null) { target.setUserCode(source.getUserCode()); }
        if (source.getUsername() != null) { target.setUsername(source.getUsername()); }
        if (source.getNickname() != null) { target.setNickname(source.getNickname()); }
        if (source.getCostTime() != null) { target.setCostTime(source.getCostTime()); }
        if (source.getErrorMsg() != null) { target.setErrorMsg(source.getErrorMsg()); }
        if (source.getSort() != null) { target.setSort(source.getSort()); }
        if (source.getCreateTime() != null) { target.setCreateTime(source.getCreateTime()); }
        if (source.getCreateBy() != null) { target.setCreateBy(source.getCreateBy()); }
        if (source.getUpdateTime() != null) { target.setUpdateTime(source.getUpdateTime()); }
        if (source.getUpdateBy() != null) { target.setUpdateBy(source.getUpdateBy()); }
        if (source.getRemark() != null) { target.setRemark(source.getRemark()); }
        if (source.getVersion() != null) { target.setVersion(source.getVersion()); }
        return target;
    }

}

