package com.wkclz.iam.sso.service;

import com.wkclz.iam.common.entity.IamRequestLog;
import com.wkclz.iam.common.helper.IpLocalCacheHelper;
import com.wkclz.iam.session.bean.RequestRecord;
import com.wkclz.iam.session.spi.RequestRecordHandler;
import com.wkclz.iam.sso.mapper.SsoRequestLogMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 请求日志持久化实现 — 将 {@link RequestRecord} 映射为 {@link IamRequestRecord} 并写入数据库。
 *
 * <p>覆盖 {@link com.wkclz.iam.session.spi.NoOpRequestRecordHandler}，自动被 Spring 容器优先装配。</p>
 */
@Component
public class RequestRecordHandlerImpl implements RequestRecordHandler {

    private static final Logger log = LoggerFactory.getLogger(RequestRecordHandlerImpl.class);

    @Resource
    private SsoRequestLogMapper ssoRequestLogMapper;

    @Override
    public void handle(RequestRecord data) {
        if (data == null) {
            return;
        }
        try {
            IamRequestLog entity = mapToEntity(data);
            ssoRequestLogMapper.insertLog(entity);
        } catch (Exception e) {
            log.warn("Failed to save request log: uri={}, error={}", data.getRequestUri(), e.getMessage());
        }
    }

    private IamRequestLog mapToEntity(RequestRecord data) {
        IamRequestLog entity = new IamRequestLog();

        entity.setTenantCode(data.getTenantCode());
        entity.setAppCode(data.getAppCode());
        entity.setUserAgent(truncate(data.getUserAgent(), 1023));
        entity.setCharacterEncoding(truncate(data.getCharacterEncoding(), 15));
        entity.setAccept(truncate(data.getAccept(), 255));
        entity.setAcceptLanguage(truncate(data.getAcceptLanguage(), 255));
        entity.setAcceptEncoding(truncate(data.getAcceptEncoding(), 31));
        entity.setCookie(truncate(data.getCookie(), 2047));
        entity.setOrigin(truncate(data.getOrigin(), 255));
        entity.setReferer(truncate(data.getReferer(), 1023));
        entity.setRemoteAddr(truncate(data.getRemoteAddr(), 63));
        entity.setMethod(truncate(data.getMethod(), 15));
        entity.setHttpProtocol(truncate(data.getHttpProtocol(), 31));
        entity.setRequestHost(truncate(data.getRequestHost(), 63));
        entity.setRequestUri(truncate(data.getRequestUri(), 255));
        entity.setQueryString(truncate(data.getQueryString(), 1023));
        entity.setRequestBody(truncate(data.getRequestBody(), 4095));
        entity.setResponseBody(data.getResponseBody());
        entity.setHttpStatus(data.getHttpStatus());
        entity.setToken(truncate(data.getToken(), 511));
        entity.setUserCode(truncate(data.getUserCode(), 31));
        entity.setUsername(truncate(data.getUsername(), 31));
        entity.setNickname(truncate(data.getNickname(), 31));
        entity.setCostTime(data.getCostTime());
        entity.setErrorMsg(truncate(data.getErrorMsg(), 4095));

        // IP 归属地查询（缓存）
        IamRequestLog location = IpLocalCacheHelper.offerQueue(entity.getRemoteAddr());
        if (location != null) {
            entity.setLocation(location.getLocation());
            entity.setIsp(location.getIsp());
        }

        // 创建人/更新人
        if (entity.getUserCode() != null) {
            entity.setCreateBy(entity.getUserCode());
            entity.setUpdateBy(entity.getUserCode());
        }

        return entity;
    }

    private static String truncate(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }
}
