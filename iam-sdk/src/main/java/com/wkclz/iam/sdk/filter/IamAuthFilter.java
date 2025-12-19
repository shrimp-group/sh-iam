package com.wkclz.iam.sdk.filter;

import com.wkclz.iam.sdk.config.IamSdkConfig;
import com.wkclz.iam.sdk.helper.ResponseHelper;
import com.wkclz.iam.sdk.helper.SessionHelper;
import com.wkclz.iam.sdk.model.UserJwt;
import com.wkclz.iam.sdk.model.UserSession;
import com.wkclz.iam.sdk.service.IamSsoService;
import com.wkclz.iam.sdk.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class IamAuthFilter extends OncePerRequestFilter {

    @Autowired
    private IamSdkConfig iamSdkConfig;
    @Autowired
    private IamSsoService iamSsoService;

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        if (SessionHelper.match( "/*/public/**", requestURI)) {
            chain.doFilter(request, response);
            return;
        }
        // 1. 从请求头中获取token
        String token = SessionHelper.getToken(request);
        
        // 2. 验证token是否存在
        if (!StringUtils.hasText(token)) {
            ResponseHelper.responseError(response, HttpStatus.UNAUTHORIZED, "token 不存在!");
            return;
        }

        // 3. 验证JWT有效性
        if (!JwtUtil.validateToken(token, iamSdkConfig.getJwtSecretKey())) {
            ResponseHelper.responseError(response, HttpStatus.UNAUTHORIZED, "无效的token!");
            return;
        }

        try {
            // 4. 解析JWT获取用户信息
            UserJwt userJwt = JwtUtil.parseToken(token, iamSdkConfig.getJwtSecretKey());
            
            // 5. 从Redis中获取用户会话信息 【从 cas 中获取】
            UserSession userSession = iamSsoService.tokenCheck(token, userJwt.getUsername());

            // 6. 将用户信息存入请求上下文，方便后续使用
            SessionHelper.cacheUserInfo(request, userJwt, userSession);

            // 7. 放行请求
            chain.doFilter(request, response);
        } catch (Exception e) {
            ResponseHelper.responseError(response, HttpStatus.UNAUTHORIZED, "token验证失败!");
        }
    }

}