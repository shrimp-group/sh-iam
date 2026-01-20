package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.service.IamRequestLogService;
import com.wkclz.iam.common.entity.IamRequestLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Route.PREFIX)
public class RequestLogRest {

    @Autowired
    protected IamRequestLogService iamRequestLogService;

    @GetMapping(Route.REQUEST_LOG_PAGE)
    public R requestLogPage(IamRequestLog entity) {
        PageData<IamRequestLog> page = iamRequestLogService.getRequestLogPage(entity);
        return R.ok(page);
    }

    @GetMapping(Route.REQUEST_LOG_INFO)
    public R requestLogInfo(IamRequestLog entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        IamRequestLog requestLog = iamRequestLogService.selectById(entity.getId());
        return R.ok(requestLog);
    }

}