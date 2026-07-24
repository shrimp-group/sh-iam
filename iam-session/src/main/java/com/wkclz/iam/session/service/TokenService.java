package com.wkclz.iam.session.service;

import com.wkclz.iam.session.bean.TokenInfo;
import com.wkclz.iam.session.config.IamSessionConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 令牌生成、验证与刷新服务。
 *
 * <p>HS256 签名，claims 仅含 userCode / username / nickname（最小信息集），
 * TTL 可配置，默认 24h。</p>
 */
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private SecretKey secretKey = null;

    @Autowired
    private IamSessionConfig iamSessionConfig;

    private SecretKey getSecretKey() {
        if (secretKey != null) {
            return secretKey;
        }
        String secretKeyStr = iamSessionConfig.getSecretKey();

        if (secretKeyStr == null || secretKeyStr.length() < 32) {
            throw new IllegalArgumentException("iam.session.token.secret-key 必须配置且长度不低于 32 字符，当前长度: " + (secretKeyStr != null ? secretKeyStr.length() : 0));
        }
        secretKey = Keys.hmacShaKeyFor(secretKeyStr.getBytes(StandardCharsets.UTF_8));
        return secretKey;
    }

    /**
     * 生成 Token。
     * <p>claims 仅含 sub(userCode)、username、nickname、iat、exp。</p>
     */
    public String generateToken(String userCode, String username, String nickname) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .subject(userCode)
            .claim("username", username)
            .claim("nickname", nickname)
            .issuedAt(new Date(now - 60_000))
            .expiration(new Date(now + iamSessionConfig.getTtl() * 1000))
            .signWith(getSecretKey(), Jwts.SIG.HS256)
            .compact();
    }


    /**
     * 从请求中提取 Token，若未找到返回 null。
     */
    public static String resolve(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        token = request.getHeader("token");
        if (StringUtils.hasText(token)) {
            return token;
        }
        return null;
    }

    /**
     * 验证 Token，解析出 TokenInfo。
     *
     * @return TokenInfo（含 userCode/username/nickname）
     * @throws IllegalArgumentException 签名错误、过期、格式异常时抛出，message 含错误原因
     */
    public TokenInfo verifyToken(String token) {
        Claims claims = parseClaims(token);
        TokenInfo tokenInfo = new TokenInfo();
        tokenInfo.setUserCode(claims.getSubject());
        tokenInfo.setUsername(claims.get("username", String.class));
        tokenInfo.setNickname(claims.get("nickname", String.class));
        tokenInfo.setIssuedAt(claims.getIssuedAt().getTime());
        tokenInfo.setExpireAt(claims.getExpiration().getTime());
        return tokenInfo;
    }

    /**
     * 刷新 Token：基于旧 Token 签发新 Token（新 iat/exp）。
     *
     * @param oldToken 旧 Token（需未过期或仍在有效期内）
     * @return 新 Token 字符串
     */
    public String refreshToken(String oldToken) {
        TokenInfo info = verifyToken(oldToken);
        return generateToken(info.getUserCode(), info.getUsername(), info.getNickname());
    }

    /**
     * 尽力解析 Token claims（best-effort）— 即使 Token 已过期也尝试提取 claims。
     *
     * <p>用于白名单接口或会话失效时，尽最大可能获取用户身份信息。
     * 过期 Token 的 claims 仍可从 {@link ExpiredJwtException#getClaims()} 中获取。</p>
     *
     * @param token JWT Token
     * @return TokenInfo，完全无法解析时返回 null
     */
    public TokenInfo parseClaimsBestEffort(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            return verifyToken(token);
        } catch (IllegalArgumentException e) {
            // 尝试从 ExpiredJwtException 中提取 claims（过期 token 仍携带有效 claims）
            Throwable cause = e.getCause();
            if (cause instanceof ExpiredJwtException expiredEx) {
                Claims claims = expiredEx.getClaims();
                if (claims != null) {
                    log.debug("Best-effort parse from expired token: sub={}", claims.getSubject());
                    TokenInfo tokenInfo = new TokenInfo();
                    tokenInfo.setUserCode(claims.getSubject());
                    tokenInfo.setUsername(claims.get("username", String.class));
                    tokenInfo.setNickname(claims.get("nickname", String.class));
                    tokenInfo.setIssuedAt(claims.getIssuedAt() != null ? claims.getIssuedAt().getTime() : null);
                    tokenInfo.setExpireAt(claims.getExpiration() != null ? claims.getExpiration().getTime() : null);
                    return tokenInfo;
                }
            }
            log.debug("Best-effort token parse failed: {}", e.getMessage());
            return null;
        }
    }

    // ========== 内部方法 ==========

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException e) {
            throw new IllegalArgumentException("Token 已过期: " + e.getMessage(), e);
        } catch (SignatureException e) {
            throw new IllegalArgumentException("Token 签名错误: " + e.getMessage(), e);
        } catch (MalformedJwtException e) {
            throw new IllegalArgumentException("Token 格式错误: " + e.getMessage(), e);
        } catch (UnsupportedJwtException e) {
            throw new IllegalArgumentException("不支持的 Token: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Token 参数错误: " + e.getMessage(), e);
        }
    }

}
