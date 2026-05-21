package com.wkclz.iam.admin.mapper;

import com.wkclz.mybatis.mapper.BaseMapper;
import com.wkclz.iam.common.entity.IamUserPasswordHis;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IamUserPasswordHisMapper extends BaseMapper<IamUserPasswordHis> {

    Long example();

    List<IamUserPasswordHis> selectByUserCodeOrderByCreateTimeDesc(
            @Param("userCode") String userCode,
            @Param("limit") Integer limit
    );

}

