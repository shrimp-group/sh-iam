package com.wkclz.iam.admin;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.annotation.Router;

@Router(value = "iam-admin")
public interface Route {


    @Desc("admin 用户-分页")
    String USER_PAGE = "/iam-admin/user/page";
    @Desc("admin 用户-创建")
    String USER_CREATE = "/iam-admin/user/create";
    @Desc("admin 用户-修改")
    String USER_UPDATE = "/iam-admin/user/update";
    @Desc("admin 用户-删除")
    String USER_REMOVE = "/iam-admin/user/remove";


}
