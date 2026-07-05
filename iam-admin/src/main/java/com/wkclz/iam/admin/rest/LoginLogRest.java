package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.LoginLogPageReq;
import com.wkclz.iam.admin.bean.resp.LoginLogResp;
import com.wkclz.iam.admin.service.IamLoginLogService;
import com.wkclz.iam.common.entity.IamLoginLog;
import com.wkclz.tool.utils.BeanUtil;
import com.wkclz.web.bean.IdReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Route.PREFIX)
@Validated
@Tag(name = "登录日志", description = "登录日志查询接口")
public class LoginLogRest {

    @Autowired
    protected IamLoginLogService iamLoginLogService;

    @GetMapping(Route.LOGIN_LOG_PAGE)
    @Operation(summary = "登录日志分页查询")
    public R<PageData<LoginLogResp>> loginLogPage(@Valid LoginLogPageReq req) {
        IamLoginLog entity = BeanUtil.cp(req, IamLoginLog.class);
        PageData<IamLoginLog> page = iamLoginLogService.getLoginLogPage(entity);
        PageData<LoginLogResp> convert = page.convert(LoginLogResp.class);
        return R.ok(convert);
    }

    @GetMapping(Route.LOGIN_LOG_INFO)
    @Operation(summary = "登录日志详情")
    public R<LoginLogResp> loginLogInfo(@Valid IdReq req) {
        IamLoginLog loginLog = iamLoginLogService.selectById(req.getId());
        return R.ok(BeanUtil.cp(loginLog, LoginLogResp.class));
    }

}
