package com.wkclz.iam.sso.contract;

import com.wkclz.auth.bean.AuthToken;
import com.wkclz.auth.bean.Principal;
import com.wkclz.auth.contract.auth.TokenService;
import com.wkclz.auth.enums.AuthErrorType;
import com.wkclz.auth.enums.TokenType;
import com.wkclz.auth.exception.AuthenticationException;
import com.wkclz.iam.sso.config.IamSsoConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenService implements TokenService {

    private static final long SESSION_TTL_SECONDS = 24 * 60 * 60L;

    @Autowired
    private IamSsoConfig iamSsoConfig;

    @Override
    public AuthToken generateToken(Principal principal) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userCode", principal.getUserCode());
        claims.put("username", principal.getUsername());
        claims.put("nickname", principal.getNickname());
        claims.put("avatar", principal.getAvatar());
        if (principal.getAppCode() != null) {
            claims.put("appCode", principal.getAppCode());
        }
        if (principal.getAuthIdentifier() != null) {
            claims.put("authIdentifier", principal.getAuthIdentifier());
        }

        Date issuedAt = new Date(System.currentTimeMillis() - 60 * 1000);
        Date expiration = new Date(System.currentTimeMillis() + SESSION_TTL_SECONDS * 1000);
        SecretKey signingKey = getSigningKey();

        String tokenValue = Jwts.builder()
            .claims(claims)
            .issuedAt(issuedAt)
            .expiration(expiration)
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact();

        AuthToken token = new AuthToken();
        token.setType(TokenType.JWT);
        token.setTokenValue(tokenValue);
        token.setIssueTime(LocalDateTime.ofInstant(issuedAt.toInstant(), ZoneId.systemDefault()));
        token.setExpireTime(LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault()));
        return token;
    }

    @Override
    public Principal parseToken(String tokenValue) {
        Claims claims = parseClaims(tokenValue);
        Principal principal = new Principal();
        principal.setUserCode(claims.get("userCode", String.class));
        principal.setUsername(claims.get("username", String.class));
        principal.setNickname(claims.get("nickname", String.class));
        principal.setAvatar(claims.get("avatar", String.class));
        principal.setAppCode(claims.get("appCode", String.class));
        principal.setAuthIdentifier(claims.get("authIdentifier", String.class));
        return principal;
    }

    @Override
    public boolean validateToken(String tokenValue) {
        try {
            parseClaims(tokenValue);
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

    // ===== private =====

    private SecretKey getSigningKey() {
        byte[] keyBytes = iamSsoConfig.getJwtSecretKey().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims parseClaims(String tokenValue) {
        try {
            SecretKey signingKey = getSigningKey();
            return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(tokenValue)
                .getPayload();
        } catch (ExpiredJwtException e) {
            throw new AuthenticationException(AuthErrorType.TOKEN_EXPIRED, "JWT已过期", e);
        } catch (MalformedJwtException e) {
            throw new AuthenticationException(AuthErrorType.TOKEN_INVALID, "JWT格式错误", e);
        } catch (UnsupportedJwtException e) {
            throw new AuthenticationException(AuthErrorType.TOKEN_INVALID, "不支持的JWT", e);
        } catch (SignatureException e) {
            throw new AuthenticationException(AuthErrorType.TOKEN_INVALID, "JWT签名错误", e);
        } catch (IllegalArgumentException e) {
            throw new AuthenticationException(AuthErrorType.TOKEN_INVALID, "JWT参数错误", e);
        }
    }
}
