package com.wkclz.iam.portal.mapper;

import com.wkclz.iam.common.entity.IamApp;
import com.wkclz.iam.portal.bean.resp.SsoAppInfoResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 登录页应用信息查询。
 */
@Mapper
public interface SsoAppMapper {

    /**
     * 按 appCode 查询应用名称与登录页背景图（仅返回这两个字段）。
     */
    SsoAppInfoResp selectAppInfo(@Param("appCode") String appCode);

    /**
     * 全部未删除应用（含鉴权类型与域名，按 sort 升序）。
     */
    List<IamApp> selectAllApps();

    /**
     * 用户有正在生效角色的应用编码（去重）。
     */
    List<String> selectUserActiveAppCodes(@Param("userCode") String userCode);
}
