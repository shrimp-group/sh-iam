package com.wkclz.auth.contract.authz;

import com.wkclz.auth.bean.MenuNode;
import com.wkclz.auth.bean.Principal;
import java.util.List;

public interface MenuProvider {
    List<MenuNode> getUserMenuTree(Principal principal);
    List<String> getUserButtonPermissions(Principal principal);
}
