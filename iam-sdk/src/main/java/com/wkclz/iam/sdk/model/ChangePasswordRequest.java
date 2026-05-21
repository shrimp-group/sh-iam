package com.wkclz.iam.sdk.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChangePasswordRequest implements Serializable {

    private String oldPassword;
    private String newPassword;

}
