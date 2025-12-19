package com.wkclz.iam.sdk.helper;


import com.wkclz.iam.sdk.model.UserJwt;
import com.wkclz.iam.sdk.model.UserSession;
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


    public static String getToken(HttpServletRequest request) {
        // 1. 从请求头中获取token
        String token = request.getHeader("Authorization");
        if (StringUtils.isBlank(token)) {
            token = request.getParameter("token");
        }

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return token;
    }


    public static void cacheUserInfo(HttpServletRequest request, UserJwt userJwt, UserSession userSession) {
        request.setAttribute("userJwt", userJwt);
        request.setAttribute("userSession", userSession);
    }




}
