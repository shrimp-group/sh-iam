package com.wkclz.iam.sdk.util;

import com.wkclz.iam.sdk.model.UserJwt;
import com.wkclz.tool.tools.Md5Tool;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {

    private static final long DEFAULT_EXPIRATION = 24 * 60 * 60 * 1000L;

    // 使用 Base64 解码生成 SecretKey（更安全，避免 UTF-8 编码问题）
    private static SecretKey getSigningKey(String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public static String getTokenRedisKey(String token, String identifier) {
        return "iam:session:" + identifier + ":" + Md5Tool.md5(token);
    }


    /**
     * 生成带过期时间的JWT token
     * @param userJwt 用户JWT对象
     * @return JWT token字符串
     */
    public static String generateToken(UserJwt userJwt, String secretKey) {
        return generateToken(userJwt, secretKey, DEFAULT_EXPIRATION);
    }

    /**
     * 生成带过期时间的JWT token
     * @param userJwt 用户JWT对象
     * @param expiration 过期时间（毫秒）
     * @return JWT token字符串
     */
    public static String generateToken(UserJwt userJwt, String secretKey, long expiration) {
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
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 从token中获取UserJwt对象
     * @param token JWT token字符串
     * @return UserJwt对象
     * @throws Exception 解析异常
     */
    public static UserJwt parseToken(String token, String secretKey) throws Exception {
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
     * @throws Exception 解析异常
     */
    public static Claims parseClaims(String token, String secretKey) throws Exception {
        try {
            SecretKey signingKey = getSigningKey(secretKey);
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new Exception("JWT已过期", e);
        } catch (MalformedJwtException e) {
            throw new Exception("JWT格式错误", e);
        } catch (UnsupportedJwtException e) {
            throw new Exception("不支持的JWT", e);
        } catch (SignatureException e) {
            throw new Exception("JWT签名错误", e);
        } catch (IllegalArgumentException e) {
            throw new Exception("JWT参数错误", e);
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
        } catch (Exception e) {
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
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 刷新JWT token
     * @param token 旧的JWT token字符串
     * @return 新的JWT token字符串
     * @throws Exception 刷新异常
     */
    public static String refreshToken(String token, String secretKey) throws Exception {
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
    public static String refreshToken(String token, String secretKey, long expiration) throws Exception {
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
    public static <T> T getClaim(String token, String claimName, Class<T> claimType, String secretKey) throws Exception {
        Claims claims = parseClaims(token, secretKey);
        return claims.get(claimName, claimType);
    }

    /**
     * 获取JWT的过期时间
     * @param token JWT token字符串
     * @return 过期时间
     * @throws Exception 解析异常
     */
    public static Date getExpirationDate(String token, String secretKey) throws Exception {
        Claims claims = parseClaims(token, secretKey);
        return claims.getExpiration();
    }

    /**
     * 获取JWT的签发时间
     * @param token JWT token字符串
     * @return 签发时间
     * @throws Exception 解析异常
     */
    public static Date getIssuedAt(String token, String secretKey) throws Exception {
        Claims claims = parseClaims(token, secretKey);
        return claims.getIssuedAt();
    }

    /**
     * 从token中获取userCode
     * @param token JWT token字符串
     * @return userCode
     * @throws Exception 解析异常
     */
    public static String getUserCode(String token, String secretKey) throws Exception {
        return getClaim(token, "userCode", String.class, secretKey);
    }

    /**
     * 从token中获取username
     * @param token JWT token字符串
     * @return username
     * @throws Exception 解析异常
     */
    public static String getUsername(String token, String secretKey) throws Exception {
        return getClaim(token, "username", String.class, secretKey);
    }

    /**
     * 从token中获取nickname
     * @param token JWT token字符串
     * @return nickname
     * @throws Exception 解析异常
     */
    public static String getNickname(String token, String secretKey) throws Exception {
        return getClaim(token, "nickname", String.class, secretKey);
    }

    /**
     * 从token中获取avatar
     * @param token JWT token字符串
     * @return avatar
     * @throws Exception 解析异常
     */
    public static String getAvatar(String token, String secretKey) throws Exception {
        return getClaim(token, "avatar", String.class, secretKey);
    }

}
