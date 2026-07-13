package com.wkclz.auth.contract.auth;

public interface PasswordEncoder {
    String encode(String rawPassword, String salt);

    boolean matches(String rawPassword, String salt, String encoded);
}
