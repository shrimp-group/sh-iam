package com.wkclz.iam.sdk.helper;


import com.wkclz.iam.sdk.model.UserJwt;
import com.wkclz.iam.sdk.model.UserSession;
import com.wkclz.web.helper.RequestHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.AntPathMatcher;

public class SessionHelper {

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    public static boolean match(String rule, String uri) {
        if (StringUtils.isBlank(rule) || StringUtils.isBlank(uri)) {
            return false;
        }
        return ANT_PATH_MATCHER.match(rule, uri);
    }


    public static String getAppCode(HttpServletRequest request) {
        // 1. 从请求头中获取token
        return request.getHeader("app-code");
    }
    public static String getToken(HttpServletRequest request) {
        // 1. 从请求头中获取token
        String token = request.getHeader("Authorization");
        if (StringUtils.isBlank(token)) {
            token = request.getHeader("token");
        }

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return token;
    }

    public static void invalidToken() {
        // TODO 需要补充逻辑
    }


    public static void cacheUserInfo(HttpServletRequest request, UserJwt userJwt, UserSession userSession) {
        request.setAttribute("userJwt", userJwt);
        request.setAttribute("userSession", userSession);
    }

    public static String getUserCode() {
        return getUserJwt() == null ? null : getUserJwt().getUserCode();
    }
    public static String getTenantCode() {
        // TODO 需要补充逻辑
        return "default";
    }

    public static UserJwt getUserJwt() {
        HttpServletRequest request = RequestHelper.getRequest();
        return getUserJwt(request);
    }
    public static UserJwt getUserJwt(HttpServletRequest request) {
        Object userJwt = request.getAttribute("userJwt");
        return userJwt == null ? null : (UserJwt) userJwt;
    }
    public static UserSession getUserSession() {
        HttpServletRequest request = RequestHelper.getRequest();
        return getUserSession(request);
    }
    public static UserSession getUserSession(HttpServletRequest request) {
        Object userSession = request.getAttribute("userSession");
        return userSession == null ? null : (UserSession) userSession;
    }


}
