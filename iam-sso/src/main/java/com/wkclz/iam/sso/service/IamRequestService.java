package com.wkclz.iam.sso.service;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.wkclz.auth.bean.RequestRecord;
import com.wkclz.auth.contract.infra.RequestLogger;
import com.wkclz.iam.common.entity.IamRequestLog;
import com.wkclz.iam.common.helper.IpLocalCacheHelper;
import com.wkclz.iam.sso.mapper.SsoRequestLogMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IamRequestService implements RequestLogger {

    private static final Logger log = LoggerFactory.getLogger(IamRequestService.class);

    private static final Pattern PWD_PATTERN = Pattern.compile("assword\"\\s*:\\s*\"(.*?)\"");
    private static final int MAX_BODY_LEN = 4096;

    @Resource
    private SsoRequestLogMapper ssoRequestLogMapper;

    @Override
    public void save(RequestRecord record) {
        if (record == null) {
            return;
        }
        // 异步保存
        ThreadUtil.execAsync(() -> {
            try {
                doSave(record);
            } catch (Exception e) {
                log.error("save request log error: {}", e.getMessage());
            }
        });
    }

    private void doSave(RequestRecord log) {
        // ─── 数据加工：UA 解析 ───
        if (log.getUserAgent() != null) {
            UserAgent ua = UserAgentUtil.parse(log.getUserAgent());
            log.setBrowserName(ua.getBrowser() == null ? null : ua.getBrowser().toString());
            log.setBrowserVersion(ua.getVersion());
            log.setEngineName(ua.getEngine() == null ? null : ua.getEngine().toString());
            log.setEngineVersion(ua.getEngineVersion());
            log.setUserOs(ua.getOs() == null ? null : ua.getOs().toString());
            log.setUserPlatform(ua.getPlatform() == null ? null : ua.getPlatform().toString());
        }

        // ─── 数据加工：截断 ───
        log.setTenantCode(subText(log.getTenantCode(), 31));
        log.setAppCode(subText(log.getAppCode(), 31));
        log.setUserAgent(subText(log.getUserAgent(), 1023));
        log.setBrowserName(subText(log.getBrowserName(), 31));
        log.setBrowserVersion(subText(log.getBrowserVersion(), 31));
        log.setEngineName(subText(log.getEngineName(), 31));
        log.setEngineVersion(subText(log.getEngineVersion(), 31));
        log.setUserOs(subText(log.getUserOs(), 63));
        log.setUserPlatform(subText(log.getUserPlatform(), 31));
        log.setCharacterEncoding(subText(log.getCharacterEncoding(), 15));
        log.setAccept(subText(log.getAccept(), 255));
        log.setAcceptLanguage(subText(log.getAcceptLanguage(), 255));
        log.setAcceptEncoding(subText(log.getAcceptEncoding(), 31));
        log.setCookie(subText(log.getCookie(), 2047));
        log.setOrigin(subText(log.getOrigin(), 255));
        log.setReferer(subText(log.getReferer(), 1023));
        log.setRemoteAddr(subText(log.getRemoteAddr(), 63));
        log.setMethod(subText(log.getMethod(), 15));
        log.setHttpProtocol(subText(log.getHttpProtocol(), 31));
        log.setRequestHost(subText(log.getRequestHost(), 63));
        log.setRequestUri(subText(log.getRequestUri(), 255));
        log.setQueryString(subText(log.getQueryString(), 1023));
        log.setRequestBody(subText(log.getRequestBody(), MAX_BODY_LEN));
        log.setUserCode(subText(log.getUserCode(), 31));
        log.setUsername(subText(log.getUsername(), 31));
        log.setNickname(subText(log.getNickname(), 31));
        log.setErrorMsg(subText(log.getErrorMsg(), MAX_BODY_LEN));

        // ─── 数据加工：脱敏 ───
        log.setToken(maskToken(log.getToken()));
        log.setRequestBody(maskPwd(log.getRequestBody()));

        // ─── 控制台日志（已脱敏） ───
        logConsole(log);

        // ─── 持久化 ───
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

        // IP 归属地
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

    /** 控制台日志（依赖 logback MaskingPatternLayout 对 password 字段脱敏） */
    private void logConsole(RequestRecord r) {
        int status = r.getHttpStatus() != null ? r.getHttpStatus() : 0;
        String ip = StringUtils.defaultString(r.getRemoteAddr(), "-");
        String user = StringUtils.defaultString(r.getUsername(), "-");
        long cost = r.getCostTime() != null ? r.getCostTime() : 0;
        String method = StringUtils.defaultString(r.getMethod(), "-");
        String uri = StringUtils.defaultString(r.getRequestUri(), "-");
        String body = getLogBody(r);

        if (r.getErrorMsg() != null) {
            log.warn("{} | {} | {}ms | {} | {} | {} | status={} error={}",
                    ip, user, cost, method, uri, body, status, r.getErrorMsg());
        } else {
            log.info("{} | {} | {}ms | {} | {} | {} | status={}",
                    ip, user, cost, method, uri, body, status);
        }
    }

    private String getLogBody(RequestRecord r) {
        // GET 请求用 queryString，POST 用 requestBody
        String body = "GET".equalsIgnoreCase(r.getMethod()) ? r.getQueryString() : r.getRequestBody();
        return StringUtils.isNotBlank(body) ? body : "-";
    }

    private static String maskPwd(String body) {
        if (StringUtils.isBlank(body) || !body.contains("assword")) {
            return body;
        }
        Matcher matcher = PWD_PATTERN.matcher(body);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String pwd = matcher.group(1);
            matcher.appendReplacement(sb, "assword\": \"" + "*".repeat(pwd.length()) + "\"");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String maskToken(String token) {
        if (StringUtils.isBlank(token)) {
            return token;
        }
        int len = token.length();
        if (len < 16) {
            return "*".repeat(len);
        }
        return token.substring(0, 8) + "***" + token.substring(len - 4);
    }

    private static String subText(String text, int max) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        int length = text.length();
        if (length <= max) {
            return text;
        }
        return text.substring(0, max);
    }
}
