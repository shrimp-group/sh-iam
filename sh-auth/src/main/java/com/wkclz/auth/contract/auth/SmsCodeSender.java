package com.wkclz.auth.contract.auth;

public interface SmsCodeSender {
    boolean send(String phoneNumber, String code);
}
