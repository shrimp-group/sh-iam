package com.wkclz.iam.sdk.service;

import com.wkclz.iam.sdk.model.UserSession;

public interface IamSsoService {

    UserSession tokenCheck(String token, String authIdentifier);

}
