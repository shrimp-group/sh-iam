package com.wkclz.iam.sdk.contract;

import com.wkclz.auth.bean.AuthToken;
import com.wkclz.auth.bean.Principal;
import com.wkclz.auth.contract.auth.TokenService;
import com.wkclz.auth.enums.TokenType;
import com.wkclz.iam.sdk.bean.UserJwt;
import com.wkclz.iam.sdk.contract.config.ContractSettings;
import com.wkclz.iam.sdk.util.JwtUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class JwtTokenService implements TokenService {

    @Override
    public AuthToken generateToken(Principal principal) {
        UserJwt userJwt = toUserJwt(principal);

        String tokenValue = JwtUtil.generateToken(userJwt, ContractSettings.getJwtSecretKey());

        AuthToken authToken = new AuthToken();
        authToken.setType(TokenType.JWT);
        authToken.setTokenValue(tokenValue);
        authToken.setIssueTime(LocalDateTime.now());
        authToken.setExpireTime(LocalDateTime.now().plusSeconds(JwtUtil.SESSION_TTL_SECONDS));
        return authToken;
    }

    @Override
    public Principal parseToken(String tokenValue) {
        UserJwt userJwt = JwtUtil.parseToken(tokenValue, ContractSettings.getJwtSecretKey());
        return toPrincipal(userJwt);
    }

    @Override
    public boolean validateToken(String tokenValue) {
        try {
            parseToken(tokenValue);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public AuthToken refreshToken(String tokenValue) {
        Principal principal = parseToken(tokenValue);
        return generateToken(principal);
    }

    @Override
    public TokenType getTokenType() {
        return TokenType.JWT;
    }

    private UserJwt toUserJwt(Principal principal) {
        UserJwt userJwt = new UserJwt();
        userJwt.setUserCode(principal.getUserCode());
        userJwt.setUsername(principal.getUsername());
        userJwt.setNickname(principal.getNickname());
        userJwt.setAvatar(principal.getAvatar());
        return userJwt;
    }

    private Principal toPrincipal(UserJwt userJwt) {
        Principal principal = new Principal();
        principal.setUserCode(userJwt.getUserCode());
        principal.setUsername(userJwt.getUsername());
        principal.setNickname(userJwt.getNickname());
        principal.setAvatar(userJwt.getAvatar());
        return principal;
    }
}
