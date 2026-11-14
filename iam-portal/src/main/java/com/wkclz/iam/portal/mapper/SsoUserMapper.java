package com.wkclz.iam.portal.mapper;

import com.wkclz.iam.portal.bean.req.UserSelectReq;
import com.wkclz.iam.portal.bean.resp.UserSelectResp;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户查询 Mapper
 *
 * @author shrimp
 */
@Mapper
public interface SsoUserMapper {

    List<UserSelectResp> getUserSelectPage(UserSelectReq req);

}
