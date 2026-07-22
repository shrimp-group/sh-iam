package com.wkclz.iam.sso.service;

import com.wkclz.iam.sso.spi.PasswordEncoder;
import com.wkclz.tool.tools.Md5Tool;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * PBKDF2WithHmacSHA256 密码编码器。
 *
 * <p>新格式：{@code {PBKDF2}:salt:hash}，salt 和 hash 均 Base64 编码。
 * 兼容历史 {@code {MD5}} 前缀和无前缀 MD5 hex 格式，匹配成功后自动升级。</p>
 */
@Component
public class Pbkdf2PasswordEncoder implements PasswordEncoder {

    private static final String PREFIX = "{PBKDF2}";
    private static final String MD5_PREFIX = "{MD5}";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 10_000;
    private static final int KEY_LENGTH = 256;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String encode(String rawPassword, String salt) {
        byte[] saltBytes;
        if (salt != null && !salt.isEmpty()) {
            saltBytes = salt.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        } else {
            saltBytes = new byte[16];
            secureRandom.nextBytes(saltBytes);
        }
        byte[] hash = pbkdf2(rawPassword.toCharArray(), saltBytes);
        return PREFIX + Base64.getEncoder().encodeToString(saltBytes)
            + ":" + Base64.getEncoder().encodeToString(hash);
    }

    @Override
    public boolean matches(String rawPassword, String salt, String encodedPassword) {
        if (encodedPassword == null || rawPassword == null) {
            return false;
        }
        // 新格式 {PBKDF2}
        if (encodedPassword.startsWith(PREFIX)) {
            return matchPbkdf2(rawPassword, encodedPassword);
        }
        // 历史 {MD5} 或无前缀 → MD5 匹配
        String storedHash = encodedPassword;
        if (storedHash.startsWith(MD5_PREFIX)) {
            storedHash = storedHash.substring(MD5_PREFIX.length());
        }
        return Md5Tool.md5(rawPassword + salt).equals(storedHash);
    }

    /**
     * 检查密文是否为旧格式（需要升级）。
     */
    public boolean needsUpgrade(String encodedPassword) {
        return encodedPassword != null && !encodedPassword.startsWith(PREFIX);
    }

    // ========== 内部方法 ==========

    private boolean matchPbkdf2(String rawPassword, String encodedPassword) {
        String body = encodedPassword.substring(PREFIX.length());
        int colonIdx = body.indexOf(':');
        if (colonIdx < 0) {
            return false;
        }
        byte[] saltBytes = Base64.getDecoder().decode(body.substring(0, colonIdx));
        byte[] expectedHash = Base64.getDecoder().decode(body.substring(colonIdx + 1));
        byte[] actualHash = pbkdf2(rawPassword.toCharArray(), saltBytes);
        return java.util.Arrays.equals(expectedHash, actualHash);
    }

    private byte[] pbkdf2(char[] password, byte[] salt) {
        try {
            KeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 encode error", e);
        }
    }
}
