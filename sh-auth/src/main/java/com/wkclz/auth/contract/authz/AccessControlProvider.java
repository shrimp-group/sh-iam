package com.wkclz.auth.contract.authz;

import com.wkclz.auth.bean.AuthPermission;
import com.wkclz.auth.bean.Principal;
import java.util.List;

public interface AccessControlProvider {
    boolean hasPermission(Principal principal, String apiMethod, String apiUri);
    List<AuthPermission> getUserPermissions(Principal principal);
}
