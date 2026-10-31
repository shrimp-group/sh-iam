package com.wkclz.iam.portal.service;

import com.wkclz.iam.portal.bean.resp.SsoAppInfoResp;
import com.wkclz.iam.portal.mapper.SsoAppMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SsoAppService {

    private static final Logger log = LoggerFactory.getLogger(SsoAppService.class);

    @Autowired
    private SsoAppMapper ssoAppMapper;

    public SsoAppInfoResp getAppInfo(String appCode) {
        log.info("查询登录页应用信息, appCode: {}", appCode);
        try {
            SsoAppInfoResp appInfo = ssoAppMapper.selectAppInfo(appCode);
            if (appInfo == null) {
                log.info("应用不存在, appCode: {}", appCode);
            }
            return appInfo;
        } catch (Exception e) {
            log.error("查询登录页应用信息失败, appCode: {}", appCode, e);
            throw e;
        }
    }
}
