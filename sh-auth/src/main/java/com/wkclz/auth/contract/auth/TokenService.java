package com.wkclz.auth.contract.auth;

import com.wkclz.auth.bean.AuthToken;
import com.wkclz.auth.bean.Principal;
import com.wkclz.auth.enums.TokenType;

public interface TokenService {
    AuthToken generateToken(Principal principal);
    Principal parseToken(String tokenValue);
    boolean validateToken(String tokenValue);
    AuthToken refreshToken(String tokenValue);
    TokenType getTokenType();
}
