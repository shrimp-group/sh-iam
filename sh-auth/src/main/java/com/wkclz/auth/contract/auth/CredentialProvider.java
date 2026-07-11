package com.wkclz.auth.contract.auth;

import com.wkclz.auth.bean.Credential;
import com.wkclz.auth.enums.CredentialType;

public interface CredentialProvider {
    CredentialType supportedType();
    String verify(Credential credential);
}
