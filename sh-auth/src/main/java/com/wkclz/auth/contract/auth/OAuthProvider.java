package com.wkclz.auth.contract.auth;

import com.wkclz.auth.bean.AuthToken;
import com.wkclz.auth.bean.Principal;

public interface OAuthProvider {
    String getProviderName();
    String getAuthorizationUrl(String redirectUri, String state);
    Principal getUserInfo(String code, String state);
    AuthToken getAccessToken(String code, String state);
}
