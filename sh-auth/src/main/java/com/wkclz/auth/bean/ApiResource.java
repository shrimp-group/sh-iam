package com.wkclz.auth.bean;

import lombok.Data;
import java.io.Serializable;

/** API 资源 */
@Data
public class ApiResource implements Serializable {
    private String apiCode;
    private String apiMethod;
    private String apiUri;
    private String apiName;
    private String module;
    private String appCode;
    private Boolean writeFlag;
}
