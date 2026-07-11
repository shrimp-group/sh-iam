package com.wkclz.auth.bean;

import lombok.Data;
import java.io.Serializable;

/** HTTP 安全头 */
@Data
public class SecurityHeaders implements Serializable {
    private String contentSecurityPolicy;
    private String xFrameOptions;
    private String xContentTypeOptions;
    private String strictTransportSecurity;
    private String xXssProtection;
    private String referrerPolicy;
}
