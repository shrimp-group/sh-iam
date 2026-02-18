package com.wkclz.iam.sso.mapper;

import com.wkclz.iam.common.dto.IamMenuDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SsoResourceMapper {

    List<IamMenuDto> getUserMenu(@Param("appCode") String appCode);


}
