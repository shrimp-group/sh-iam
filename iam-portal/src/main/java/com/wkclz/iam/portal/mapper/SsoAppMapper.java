package com.wkclz.iam.portal.mapper;

import com.wkclz.iam.portal.bean.resp.SsoAppInfoResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 登录页应用信息查询。
 */
@Mapper
public interface SsoAppMapper {

    /**
     * 按 appCode 查询应用名称与登录页背景图（仅返回这两个字段）。
     */
    SsoAppInfoResp selectAppInfo(@Param("appCode") String appCode);
}
