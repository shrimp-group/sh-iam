package com.wkclz.auth.contract.auth;

import com.wkclz.tool.tools.Md5Tool;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;

/**
 * 统合密码编码器（唯一 Bean）。
 * <p>
 * 编码：始终使用 PBKDF2WithHmacSHA256，产出格式为 {PBKDF2}base64hash。
 * 校验：根据编码字符串前缀自动选择算法——
 * {PBKDF2} → PBKDF2，{MD5} → MD5，无前缀 → MD5（兼容历史数据）。
 * <p>
 * 修改密码时自动升级为 PBKDF2，无需额外处理。
 */
@Component
public class DefaultPasswordEncoder implements PasswordEncoder {

    // === 前缀常量 ===
    static final String PREFIX_PBKDF2 = "{PBKDF2}";
    static final String PREFIX_MD5 = "{MD5}";

    // === PBKDF2 参数 ===
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int HASH_BYTES = 32;
    private static final int ITERATIONS = 100_000;

    // ===== encode: 统一使用 PBKDF2 =====

    @Override
    public String encode(String rawPassword, String salt) {
        if (rawPassword == null || salt == null) {
            throw new IllegalArgumentException("rawPassword and salt must not be null");
        }
        String hash = Base64.getEncoder().encodeToString(pbkdf2(rawPassword.toCharArray(), salt.getBytes()));
        return PREFIX_PBKDF2 + hash;
    }

    // ===== matches: 前缀分发 =====

    @Override
    public boolean matches(String rawPassword, String salt, String encoded) {
        if (rawPassword == null || salt == null || encoded == null) {
            return false;
        }
        if (encoded.startsWith(PREFIX_PBKDF2)) {
            return matchesPBKDF2(rawPassword, salt, encoded.substring(PREFIX_PBKDF2.length()));
        }
        if (encoded.startsWith(PREFIX_MD5)) {
            return matchesMD5(rawPassword, salt, encoded.substring(PREFIX_MD5.length()));
        }
        // 无前缀：兼容历史数据，按 MD5 校验
        return matchesMD5(rawPassword, salt, encoded);
    }

    // ===== MD5 =====

    private boolean matchesMD5(String rawPassword, String salt, String encoded) {
        return encoded.equals(Md5Tool.md5(rawPassword + salt));
    }

    // ===== PBKDF2 =====

    private boolean matchesPBKDF2(String rawPassword, String salt, String encoded) {
        byte[] hash = pbkdf2(rawPassword.toCharArray(), salt.getBytes());
        return encoded.equals(Base64.getEncoder().encodeToString(hash));
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
