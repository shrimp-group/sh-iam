package com.wkclz.auth.contract.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;

/**
 * PBKDF2WithHmacSHA256 密码编码器工具类。
 * <p>
 * 非 Spring Bean，供外部按需使用。IAM 内部统一使用 {@link DefaultPasswordEncoder}。
 * 100,000 次迭代，salt 由调用方传入、与密码分开存储。
 */
public class PBKDF2PasswordEncoder implements PasswordEncoder {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int HASH_BYTES = 32;
    private static final int ITERATIONS = 100_000;
    static final String PREFIX = "{PBKDF2}";

    @Override
    public String encode(String rawPassword, String salt) {
        if (rawPassword == null || salt == null) {
            throw new IllegalArgumentException("rawPassword and salt must not be null");
        }
        String hash = Base64.getEncoder().encodeToString(
            pbkdf2(rawPassword.toCharArray(), salt.getBytes()));
        return PREFIX + hash;
    }

    @Override
    public boolean matches(String rawPassword, String salt, String encoded) {
        if (rawPassword == null || salt == null || encoded == null) {
            return false;
        }
        String hashPart = encoded.startsWith(PREFIX) ? encoded.substring(PREFIX.length()) : encoded;
        byte[] hash = pbkdf2(rawPassword.toCharArray(), salt.getBytes());
        return hashPart.equals(Base64.getEncoder().encodeToString(hash));
    }

    private byte[] pbkdf2(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, HASH_BYTES * 8);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("PBKDF2 密码哈希失败", e);
        } finally {
            spec.clearPassword();
        }
    }

}
