package com.wkclz.auth.bean;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/** 数据权限范围 */
@Data
public class DataScope implements Serializable {
    private String dimensionCode;
    private String dimensionName;
    private String scopeValue;
    private List<String> authorizedValues;
    private String appCode;
}
