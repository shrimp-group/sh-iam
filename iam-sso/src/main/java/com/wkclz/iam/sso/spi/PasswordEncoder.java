package com.wkclz.iam.sso.spi;

/**
 * 密码编码器 SPI — 密码加密与校验。
 */
public interface PasswordEncoder {

    /**
     * 加密原始密码，返回存储格式（带前缀标识算法）。
     */
    String encode(String rawPassword, String salt);

    /**
     * 校验原始密码是否与已编码密码匹配。
     */
    boolean matches(String rawPassword, String salt, String encodedPassword);

}
