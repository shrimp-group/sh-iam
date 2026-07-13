package com.wkclz.auth.contract.auth;

import com.wkclz.tool.tools.Md5Tool;

/**
 * MD5+salt 密码编码器工具类。
 * <p>
 * 非 Spring Bean，供外部按需使用。IAM 内部统一使用 {@link DefaultPasswordEncoder}。
 * salt 由调用方传入，与密码分开存储。
 */
public class Md5PasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(String rawPassword, String salt) {
        return Md5Tool.md5(rawPassword + salt);
    }

    @Override
    public boolean matches(String rawPassword, String salt, String encoded) {
        if (rawPassword == null || salt == null || encoded == null) {
            return false;
        }
        return encoded.equals(Md5Tool.md5(rawPassword + salt));
    }

}
