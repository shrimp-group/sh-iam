package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamUser;
import lombok.Data;

@Data
public class IamUserDto extends IamUser {

    private String password;

}
