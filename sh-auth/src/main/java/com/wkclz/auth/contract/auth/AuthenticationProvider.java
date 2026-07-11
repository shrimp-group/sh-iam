package com.wkclz.auth.contract.auth;

import com.wkclz.auth.bean.AuthRequest;
import com.wkclz.auth.bean.AuthResult;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public interface AuthenticationProvider {
    AuthResult authenticate(AuthRequest request, HttpServletRequest httpRequest);
    List<String> supportedAuthTypes();
    default int getOrder() { return 0; }
}
