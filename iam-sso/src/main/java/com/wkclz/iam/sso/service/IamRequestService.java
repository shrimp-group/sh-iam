package com.wkclz.iam.sso.service;

import com.wkclz.iam.common.entity.IamRequestLog;
import com.wkclz.iam.common.helper.IpLocalCacheHelper;
import com.wkclz.iam.session.bean.RequestRecord;
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

    public void insertRecord(RequestRecord record) {
        if (record == null) {
            return;
        }
        IamRequestLog iLog = new IamRequestLog();
        iLog.setTenantCode(record.getTenantCode());
        iLog.setAppCode(record.getAppCode());
        iLog.setUserAgent(record.getUserAgent());
        iLog.setBrowserName(record.getBrowserName());
        iLog.setBrowserVersion(record.getBrowserVersion());
        iLog.setEngineName(record.getEngineName());
        iLog.setEngineVersion(record.getEngineVersion());
        iLog.setUserOs(record.getUserOs());
        iLog.setUserPlatform(record.getUserPlatform());
        iLog.setCharacterEncoding(record.getCharacterEncoding());
        iLog.setAccept(record.getAccept());
        iLog.setAcceptLanguage(record.getAcceptLanguage());
        iLog.setAcceptEncoding(record.getAcceptEncoding());
        iLog.setCookie(record.getCookie());
        iLog.setOrigin(record.getOrigin());
        iLog.setReferer(record.getReferer());
        iLog.setRemoteAddr(record.getRemoteAddr());
        iLog.setMethod(record.getMethod());
        iLog.setHttpProtocol(record.getHttpProtocol());
        iLog.setRequestHost(record.getRequestHost());
        iLog.setRequestUri(record.getRequestUri());
        iLog.setQueryString(record.getQueryString());
        iLog.setRequestBody(record.getRequestBody());
        iLog.setHttpStatus(record.getHttpStatus());
        iLog.setToken(record.getToken());
        iLog.setUserCode(record.getUserCode());
        iLog.setUsername(record.getUsername());
        iLog.setNickname(record.getNickname());
        iLog.setCostTime(record.getCostTime());
        iLog.setErrorMsg(record.getErrorMsg());
        iLog.setResponseBody(record.getResponseBody());

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
