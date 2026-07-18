package com.wkclz.iam.sdk.util;

import com.wkclz.iam.sdk.bean.UserJwt;
import com.wkclz.iam.sdk.exception.JwtValidationException;
import com.wkclz.tool.tools.Md5Tool;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类。
 *
 * @deprecated 已由 {@code com.wkclz.iam.session.service.TokenService} 取代。
 * claims 设计差异：旧工具类含 nickname/avatar，新 TokenService 仅含 userCode/username/nickname（最小信息集）。
 * 新代码请使用 iam-session 模块的 TokenService。
 */
@Deprecated
public class JwtUtil {

    public static final long SESSION_TTL_SECONDS = 24 * 60 * 60L;

    // 使用 Base64 解码生成 SecretKey（更安全，避免 UTF-8 编码问题）
    private static SecretKey getSigningKey(String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public static String getTokenRedisKey(String token, String identifier) {
        return "iam:session:" + identifier + ":" + Md5Tool.md5(token);
    }

    public static String getSessionListRedisKey(String username) {
        return "iam:session:list:" + username;
    }


    /**
     * 生成带过期时间的JWT token
     * @param userJwt 用户JWT对象
     * @return JWT token字符串
     */
    public static String generateToken(UserJwt userJwt, String secretKey) {
        return generateToken(userJwt, secretKey, SESSION_TTL_SECONDS);
    }

    /**
     * 生成带过期时间的JWT token
     * @param userJwt 用户JWT对象
     * @param sessionTtlSeconds 过期 sessionTtlSeconds * @return JWT token字符串
     */
    public static String generateToken(UserJwt userJwt, String secretKey, long sessionTtlSeconds) {
        // 创建声明
        Map<String, Object> claims = new HashMap<>();
        claims.put("userCode", userJwt.getUserCode());
        claims.put("username", userJwt.getUsername());
        claims.put("nickname", userJwt.getNickname());
        claims.put("avatar", userJwt.getAvatar());
        SecretKey signingKey = getSigningKey(secretKey);

        // 生成token
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date(System.currentTimeMillis() - 60 * 1000))
                .expiration(new Date(System.currentTimeMillis() + sessionTtlSeconds * 1000))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 从token中获取UserJwt对象
     * @param token JWT token字符串
     * @return UserJwt对象
     * @throws JwtValidationException 解析异常，包含错误类型信息
     */
    public static UserJwt parseToken(String token, String secretKey) throws JwtValidationException {
        Claims claims = parseClaims(token, secretKey);
        UserJwt userJwt = new UserJwt();
        userJwt.setUserCode(claims.get("userCode", String.class));
        userJwt.setUsername(claims.get("username", String.class));
        userJwt.setNickname(claims.get("nickname", String.class));
        userJwt.setAvatar(claims.get("avatar", String.class));
        return userJwt;
    }

    /**
     * 解析JWT获取声明
     * @param token JWT token字符串
     * @return Claims对象
     * @throws JwtValidationException 解析异常，通过 errorCode 区分过期/签名错误等情况
     */
    public static Claims parseClaims(String token, String secretKey) throws JwtValidationException {
        try {
            SecretKey signingKey = getSigningKey(secretKey);
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new JwtValidationException(JwtValidationException.CODE_EXPIRED, "JWT已过期", e);
        } catch (MalformedJwtException e) {
            throw new JwtValidationException(JwtValidationException.CODE_MALFORMED, "JWT格式错误", e);
        } catch (UnsupportedJwtException e) {
            throw new JwtValidationException(JwtValidationException.CODE_UNSUPPORTED, "不支持的JWT", e);
        } catch (SignatureException e) {
            throw new JwtValidationException(JwtValidationException.CODE_SIGNATURE, "JWT签名错误", e);
        } catch (IllegalArgumentException e) {
            throw new JwtValidationException(JwtValidationException.CODE_ILLEGAL_ARGUMENT, "JWT参数错误", e);
        }
    }

    /**
     * 验证JWT token是否有效
     * @param token JWT token字符串
     * @return 有效返回true，否则返回false
     */
    public static boolean validateToken(String token, String secretKey) {
        try {
            parseClaims(token, secretKey);
            return true;
        } catch (JwtValidationException e) {
            return false;
        }
    }

    /**
     * 检查JWT是否已过期
     * @param token JWT token字符串
     * @return 已过期返回true，否则返回false
     */
    public static boolean isExpired(String token, String secretKey) {
        try {
            Claims claims = parseClaims(token, secretKey);
            return claims.getExpiration().before(new Date());
        } catch (JwtValidationException e) {
            return JwtValidationException.CODE_EXPIRED.equals(e.getErrorCode());
        }
    }

    /**
     * 刷新JWT token
     * @param token 旧的JWT token字符串
     * @return 新的JWT token字符串
     * @throws Exception 刷新异常
     */
    public static String refreshToken(String token, String secretKey) throws JwtValidationException {
        UserJwt userJwt = parseToken(token, secretKey);
        return generateToken(userJwt, secretKey);
    }

    /**
     * 刷新JWT token，使用指定的过期时间
     * @param token 旧的JWT token字符串
     * @param expiration 新的过期时间（毫秒）
     * @return 新的JWT token字符串
     * @throws Exception 刷新异常
     */
    public static String refreshToken(String token, String secretKey, long expiration) throws JwtValidationException {
        UserJwt userJwt = parseToken(token, secretKey);
        return generateToken(userJwt, secretKey, expiration);
    }

    /**
     * 从token中获取指定声明的值
     * @param token JWT token字符串
     * @param claimName 声明名称
     * @param <T> 声明类型
     * @return 声明值
     * @throws Exception 解析异常
     */
    public static <T> T getClaim(String token, String claimName, Class<T> claimType, String secretKey) throws JwtValidationException {
        Claims claims = parseClaims(token, secretKey);
        return claims.get(claimName, claimType);
    }

    /**
     * 获取JWT的过期时间
     * @param token JWT token字符串
     * @return 过期时间
     * @throws Exception 解析异常
     */
    public static Date getExpirationDate(String token, String secretKey) throws JwtValidationException {
        Claims claims = parseClaims(token, secretKey);
        return claims.getExpiration();
    }

    /**
     * 获取JWT的签发时间
     * @param token JWT token字符串
     * @return 签发时间
     * @throws Exception 解析异常
     */
    public static Date getIssuedAt(String token, String secretKey) throws JwtValidationException {
        Claims claims = parseClaims(token, secretKey);
        return claims.getIssuedAt();
    }

    /**
     * 从token中获取userCode
     * @param token JWT token字符串
     * @return userCode
     * @throws Exception 解析异常
     */
    public static String getUserCode(String token, String secretKey) throws JwtValidationException {
        return getClaim(token, "userCode", String.class, secretKey);
    }

    /**
     * 从token中获取username
     * @param token JWT token字符串
     * @return username
     * @throws Exception 解析异常
     */
    public static String getUsername(String token, String secretKey) throws JwtValidationException {
        return getClaim(token, "username", String.class, secretKey);
    }

    /**
     * 从token中获取nickname
     * @param token JWT token字符串
     * @return nickname
     * @throws Exception 解析异常
     */
    public static String getNickname(String token, String secretKey) throws JwtValidationException {
        return getClaim(token, "nickname", String.class, secretKey);
    }

    /**
     * 从token中获取avatar
     * @param token JWT token字符串
     * @return avatar
     * @throws Exception 解析异常
     */
    public static String getAvatar(String token, String secretKey) throws JwtValidationException {
        return getClaim(token, "avatar", String.class, secretKey);
    }

}
