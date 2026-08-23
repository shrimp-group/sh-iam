package com.wkclz.iam.sso.mapper;

import com.wkclz.iam.common.entity.IamApi;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 请求控制 API 配置查询。
 */
@Mapper
public interface SsoApiControlMapper {

    /**
     * 查询所有已配置请求控制（request_control 非空）的 API。
     */
    List<IamApi> listRequestControlApis();
}
