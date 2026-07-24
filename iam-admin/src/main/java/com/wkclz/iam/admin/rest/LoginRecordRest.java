package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.LoginRecordPageReq;
import com.wkclz.iam.admin.bean.resp.LoginRecordResp;
import com.wkclz.iam.admin.service.IamLoginRecordService;
import com.wkclz.iam.common.entity.IamLoginRecord;
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
public class LoginRecordRest {

    @Autowired
    protected IamLoginRecordService iamLoginRecordService;

    @GetMapping(Route.LOGIN_RECORD_PAGE)
    @Operation(summary = "登录日志分页查询")
    public R<PageData<LoginRecordResp>> loginRecordPage(@Valid LoginRecordPageReq req) {
        IamLoginRecord entity = BeanUtil.cp(req, IamLoginRecord.class);
        PageData<IamLoginRecord> page = iamLoginRecordService.getLoginRecordPage(entity);
        PageData<LoginRecordResp> convert = page.convert(LoginRecordResp.class);
        return R.ok(convert);
    }

    @GetMapping(Route.LOGIN_RECORD_INFO)
    @Operation(summary = "登录日志详情")
    public R<LoginRecordResp> loginRecordInfo(@Valid IdReq req) {
        IamLoginRecord loginRecord = iamLoginRecordService.selectById(req.getId());
        return R.ok(BeanUtil.cp(loginRecord, LoginRecordResp.class));
    }

}
