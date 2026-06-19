package com.wkclz.iam.sdk.service;

import com.wkclz.iam.sdk.bean.UserSession;

public interface IamSsoService {

    UserSession tokenCheck(String token, String authIdentifier);

}
