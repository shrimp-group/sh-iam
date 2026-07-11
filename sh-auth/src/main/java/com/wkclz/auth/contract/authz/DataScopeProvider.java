package com.wkclz.auth.contract.authz;

import com.wkclz.auth.bean.DataScope;
import com.wkclz.auth.bean.Principal;
import java.util.List;

public interface DataScopeProvider {
    List<DataScope> getDataScopes(Principal principal);
    List<DataScope> getDataScopesByDimension(Principal principal, String dimensionCode);
}
