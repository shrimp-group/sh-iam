package com.wkclz.auth.contract.infra;

import com.wkclz.auth.bean.SecurityHeaders;

public interface SecurityHeaderProvider {
    SecurityHeaders getHeaders();
}
