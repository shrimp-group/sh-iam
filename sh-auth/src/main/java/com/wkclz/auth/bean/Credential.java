package com.wkclz.auth.bean;

import com.wkclz.auth.enums.CredentialType;
import lombok.Data;
import java.io.Serializable;

/** 凭据 */
@Data
public class Credential implements Serializable {
    private CredentialType type;
    private String credentialValue;
    private String captchaCode;
    private String captchaId;
}
