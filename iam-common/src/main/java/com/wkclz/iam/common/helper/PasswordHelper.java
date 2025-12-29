package com.wkclz.iam.common.helper;

import com.wkclz.core.exception.UserException;
import com.wkclz.tool.tools.Md5Tool;
import org.apache.commons.lang3.StringUtils;

public class PasswordHelper {


    public static String generatePassword(String password, String salt) {
        if (StringUtils.isBlank(password)) {
            throw UserException.of("password 不能为空");
        }
        if (StringUtils.isBlank(salt)) {
            throw UserException.of("salt 不能为空");
        }
        return Md5Tool.md5(password + salt);
    }

    public static boolean validatePassword(String password, String salt, String md5) {
        if (StringUtils.isBlank(password)) {
            throw UserException.of("password 不能为空");
        }
        if (StringUtils.isBlank(salt)) {
            throw UserException.of("salt 不能为空");
        }
        if (StringUtils.isBlank(md5)) {
            throw UserException.of("md5 不能为空");
        }
        return md5.equals(Md5Tool.md5(password + salt));
    }


}
