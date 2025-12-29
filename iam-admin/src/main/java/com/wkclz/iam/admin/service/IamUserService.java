package com.wkclz.iam.admin.service;

import com.wkclz.core.base.PageData;
import com.wkclz.core.exception.UserException;
import com.wkclz.iam.admin.mapper.IamUserAuthMapper;
import com.wkclz.iam.admin.mapper.IamUserAuthPasswordMapper;
import com.wkclz.iam.admin.mapper.IamUserMapper;
import com.wkclz.iam.admin.mapper.IamUserPasswordHisMapper;
import com.wkclz.iam.common.dto.IamUserDto;
import com.wkclz.iam.common.entity.IamUser;
import com.wkclz.iam.common.entity.IamUserAuth;
import com.wkclz.iam.common.entity.IamUserAuthPassword;
import com.wkclz.iam.common.entity.IamUserPasswordHis;
import com.wkclz.iam.common.helper.PasswordHelper;
import com.wkclz.iam.sdk.enums.AuthType;
import com.wkclz.mybatis.helper.PageQuery;
import com.wkclz.mybatis.service.BaseService;
import com.wkclz.redis.helper.RedisIdGenerator;
import com.wkclz.tool.tools.Md5Tool;
import com.wkclz.tool.utils.SecretUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class IamUserService extends BaseService<IamUser, IamUserMapper> {


    @Autowired
    private RedisIdGenerator redisIdGenerator;
    @Autowired
    private IamUserAuthMapper iamUserAuthMapper;
    @Autowired
    private IamUserPasswordHisMapper iamUserPasswordHisMapper;
    @Autowired
    private IamUserAuthPasswordMapper iamUserAuthPasswordMapper;


    public PageData<IamUser> userPage(IamUser entity) {
        return PageQuery.page(entity, mapper::getUserList);
    }


    @Transactional(rollbackFor = Exception.class)
    public IamUserDto userCreate(IamUserDto dto) {

        // 用户重复判断
        IamUser param = new IamUser();
        param.setUsername(dto.getUsername());
        long userCount = selectCountByEntity(param);

        if (userCount > 0) {
            throw UserException.of("用户已存在!");
        }

        // 用户入库
        dto.setUserCode(redisIdGenerator.generateId("user_"));
        insert(dto);

        // 认证账号入库
        IamUserAuth auth = new IamUserAuth();
        auth.setUserCode(dto.getUserCode());
        auth.setAuthType(AuthType.PASSWORD.name());
        auth.setAuthIdentifier(dto.getUsername());
        auth.setAuthStatus(1);
        auth.setLoginCount(0);
        iamUserAuthMapper.insert(auth);

        // 密码入库
        IamUserAuthPassword pwd = new IamUserAuthPassword();
        pwd.setUserCode(dto.getUserCode());
        pwd.setSalt(SecretUtil.getKey());
        pwd.setPassword(PasswordHelper.generatePassword(dto.getPassword(), pwd.getSalt()));
        pwd.setLastChangedTime(new Date());
        iamUserAuthPasswordMapper.insert(pwd);

        // 历史密码记录
        IamUserPasswordHis his = new IamUserPasswordHis();

        his.setUserCode(pwd.getUserCode());
        his.setPassword(pwd.getPassword());
        his.setSalt(pwd.getSalt());
        iamUserPasswordHisMapper.insert(his);
        return dto;
    }


}
