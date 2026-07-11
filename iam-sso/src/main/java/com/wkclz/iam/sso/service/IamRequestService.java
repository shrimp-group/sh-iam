package com.wkclz.iam.sso.service;

import com.wkclz.iam.common.entity.IamRequestLog;
import com.wkclz.iam.common.helper.IpLocalCacheHelper;
import com.wkclz.auth.bean.RequestRecord;
import com.wkclz.iam.sso.mapper.SsoRequestLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * @author shrimp
 */
@Service
public class IamRequestService {

    @Resource
    private SsoRequestLogMapper ssoRequestLogMapper;

    public void insertLog(RequestRecord log) {
        if (log == null) {
            return;
        }
        IamRequestLog iLog = new IamRequestLog();
        iLog.setTenantCode(log.getTenantCode());
        iLog.setAppCode(log.getAppCode());
        iLog.setUserAgent(log.getUserAgent());
        iLog.setBrowserName(log.getBrowserName());
        iLog.setBrowserVersion(log.getBrowserVersion());
        iLog.setEngineName(log.getEngineName());
        iLog.setEngineVersion(log.getEngineVersion());
        iLog.setUserOs(log.getUserOs());
        iLog.setUserPlatform(log.getUserPlatform());
        iLog.setCharacterEncoding(log.getCharacterEncoding());
        iLog.setAccept(log.getAccept());
        iLog.setAcceptLanguage(log.getAcceptLanguage());
        iLog.setAcceptEncoding(log.getAcceptEncoding());
        iLog.setCookie(log.getCookie());
        iLog.setOrigin(log.getOrigin());
        iLog.setReferer(log.getReferer());
        iLog.setRemoteAddr(log.getRemoteAddr());
        iLog.setMethod(log.getMethod());
        iLog.setHttpProtocol(log.getHttpProtocol());
        iLog.setRequestHost(log.getRequestHost());
        iLog.setRequestUri(log.getRequestUri());
        iLog.setQueryString(log.getQueryString());
        iLog.setRequestBody(log.getRequestBody());
        iLog.setHttpStatus(log.getHttpStatus());
        iLog.setToken(log.getToken());
        iLog.setUserCode(log.getUserCode());
        iLog.setUsername(log.getUsername());
        iLog.setNickname(log.getNickname());
        iLog.setCostTime(log.getCostTime());
        iLog.setErrorMsg(log.getErrorMsg());
        iLog.setResponseBody(log.getResponseBody());

        // 缓存的地址信息
        IamRequestLog location = IpLocalCacheHelper.offerQueue(iLog.getRemoteAddr());
        if (location != null) {
            iLog.setLocation(location.getLocation());
            iLog.setIsp(location.getIsp());
        }

        if (iLog.getUserCode() != null) {
            iLog.setCreateBy(iLog.getUserCode());
            iLog.setUpdateBy(iLog.getUserCode());
        }
        ssoRequestLogMapper.insertLog(iLog);
    }

}
