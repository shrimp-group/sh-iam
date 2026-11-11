package com.wkclz.iam.portal.service;

import com.wkclz.core.base.PageData;
import com.wkclz.iam.portal.bean.req.UserSelectReq;
import com.wkclz.iam.portal.bean.resp.UserSelectResp;
import com.wkclz.iam.portal.mapper.SsoUserMapper;
import com.wkclz.mybatis.helper.PageQuery;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户查询服务 — 用户选择分页查询
 *
 * @author shrimp
 */
@Slf4j
@Service
public class SsoUserService {

    @Resource
    private SsoUserMapper ssoUserMapper;

    /**
     * 用户选择分页查询（仅启用用户，用户名/姓名模糊、性别精确，按用户名升序）
     */
    public PageData<UserSelectResp> getUserSelectPage(UserSelectReq req) {
        log.info("用户选择分页查询: request={}", req);
        try {
            return PageQuery.page(req, ssoUserMapper::getUserSelectPage);
        } catch (RuntimeException e) {
            log.error("用户选择分页查询失败: request={}, error={}", req, e.getMessage(), e);
            throw e;
        }
    }

}
