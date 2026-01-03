package com.wkclz.iam.admin;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.annotation.Router;

@Router(module = "iam-admin", prefix = Route.PREFIX)
public interface Route {

    String PREFIX = "/iam-admin";
    

    @Desc("admin 用户-分页")
    String USER_PAGE = "/user/page";
    @Desc("admin 用户-创建")
    String USER_CREATE = "/user/create";
    @Desc("admin 用户-修改")
    String USER_UPDATE = "/user/update";
    @Desc("admin 用户-删除")
    String USER_REMOVE = "/user/remove";


}
