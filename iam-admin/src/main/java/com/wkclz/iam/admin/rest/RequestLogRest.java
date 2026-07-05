package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.RequestLogPageReq;
import com.wkclz.iam.admin.bean.resp.RequestLogResp;
import com.wkclz.iam.admin.service.IamRequestLogService;
import com.wkclz.iam.common.entity.IamRequestLog;
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
@Tag(name = "请求日志", description = "请求日志查询接口")
public class RequestLogRest {

    @Autowired
    protected IamRequestLogService iamRequestLogService;

    @GetMapping(Route.REQUEST_LOG_PAGE)
    @Operation(summary = "请求日志分页查询")
    public R<PageData<RequestLogResp>> requestLogPage(@Valid RequestLogPageReq req) {
        IamRequestLog entity = BeanUtil.cp(req, IamRequestLog.class);
        PageData<IamRequestLog> page = iamRequestLogService.getRequestLogPage(entity);
        PageData<RequestLogResp> convert = page.convert(RequestLogResp.class);
        return R.ok(convert);
    }

    @GetMapping(Route.REQUEST_LOG_INFO)
    @Operation(summary = "请求日志详情")
    public R<RequestLogResp> requestLogInfo(@Valid IdReq req) {
        IamRequestLog requestLog = iamRequestLogService.selectById(req.getId());
        return R.ok(BeanUtil.cp(requestLog, RequestLogResp.class));
    }

}
