package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.RequestRecordPageReq;
import com.wkclz.iam.admin.bean.resp.RequestRecordResp;
import com.wkclz.iam.admin.service.IamRequestRecordService;
import com.wkclz.iam.common.entity.IamRequestRecord;
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
public class RequestRecordRest {

    @Autowired
    protected IamRequestRecordService iamRequestRecordService;

    @GetMapping(Route.REQUEST_RECORD_PAGE)
    @Operation(summary = "请求日志分页查询")
    public R<PageData<RequestRecordResp>> requestRecordPage(@Valid RequestRecordPageReq req) {
        IamRequestRecord entity = BeanUtil.cp(req, IamRequestRecord.class);
        PageData<IamRequestRecord> page = iamRequestRecordService.getRequestRecordPage(entity);
        PageData<RequestRecordResp> convert = page.convert(RequestRecordResp.class);
        return R.ok(convert);
    }

    @GetMapping(Route.REQUEST_RECORD_INFO)
    @Operation(summary = "请求日志详情")
    public R<RequestRecordResp> requestRecordInfo(@Valid IdReq req) {
        IamRequestRecord requestRecord = iamRequestRecordService.selectById(req.getId());
        return R.ok(BeanUtil.cp(requestRecord, RequestRecordResp.class));
    }

}
