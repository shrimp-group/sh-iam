package com.wkclz.auth.contract.auth;

import com.wkclz.tool.tools.Md5Tool;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * MD5+salt 密码编码器默认实现
 *
 * 编码格式: {MD5}salt$hash（接口契约）
 * - encode() 自动生成 16 字节随机 salt，计算 MD5(password+salt)，拼接返回
 * - matches() 从编码字符串中解析 salt 和 hash，重新计算并比较
 *
 * 兼容现有盐值分离存储（重载方法，不破坏接口契约）：
 * - encode(raw, salt) / matches(raw, salt, encoded)
 */
public class Md5PasswordEncoder implements PasswordEncoder {

    private static final String PREFIX = "{MD5}";
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    // ===== 接口标准方法 =====

    @Override
    public String encode(String rawPassword) {
        byte[] saltBytes = new byte[SALT_BYTES];
        RANDOM.nextBytes(saltBytes);
        String salt = Base64.getEncoder().encodeToString(saltBytes);
        String hash = Md5Tool.md5(rawPassword + salt);
        return PREFIX + salt + "$" + hash;
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        if (!encodedPassword.startsWith(PREFIX)) {
            return false;
        }
        String content = encodedPassword.substring(PREFIX.length());
        int dollarIdx = content.indexOf('$');
        if (dollarIdx < 0) {
            return false;
        }
        String salt = content.substring(0, dollarIdx);
        String hash = content.substring(dollarIdx + 1);
        return hash.equals(Md5Tool.md5(rawPassword + salt));
    }

    // ===== 兼容盐值分离存储的重载方法 =====

    /**
     * 使用指定盐值编码密码（兼容已有盐值分离存储的 DB 结构）
     * @param rawPassword 原始密码
     * @param salt 已有盐值
     * @return MD5(rawPassword + salt)
     */
    public String encode(String rawPassword, String salt) {
        return Md5Tool.md5(rawPassword + salt);
    }

    /**
     * 校验密码（兼容已有盐值分离存储的 DB 结构）
     * @param rawPassword 原始密码
     * @param salt 盐值
     * @param encoded MD5 值
     * @return 是否匹配
     */
    public boolean matches(String rawPassword, String salt, String encoded) {
        if (rawPassword == null || salt == null || encoded == null) {
            return false;
        }
        return encoded.equals(Md5Tool.md5(rawPassword + salt));
    }
}
