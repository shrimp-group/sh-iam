package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.service.IamLoginLogService;
import com.wkclz.iam.common.entity.IamLoginLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Route.PREFIX)
public class LoginLogRest {

    @Autowired
    protected IamLoginLogService iamLoginLogService;

    @GetMapping(Route.LOGIN_LOG_PAGE)
    public R loginLogPage(IamLoginLog entity) {
        PageData<IamLoginLog> page = iamLoginLogService.getLoginLogPage(entity);
        return R.ok(page);
    }

    @GetMapping(Route.LOGIN_LOG_INFO)
    public R loginLogInfo(IamLoginLog entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        IamLoginLog loginLog = iamLoginLogService.selectById(entity.getId());
        return R.ok(loginLog);
    }

}