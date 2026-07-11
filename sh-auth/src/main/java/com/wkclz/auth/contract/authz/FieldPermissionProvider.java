package com.wkclz.auth.contract.authz;

import com.wkclz.auth.bean.FieldPermission;
import com.wkclz.auth.bean.Principal;
import java.util.List;

public interface FieldPermissionProvider {
    List<FieldPermission> getFieldPermissions(String apiCode);
    List<FieldPermission> getUserFieldPermissions(Principal principal, String apiCode);
}
