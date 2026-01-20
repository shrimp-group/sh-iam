package com.wkclz.iam.admin.service;

import com.wkclz.core.base.PageData;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
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
import com.wkclz.tool.utils.SecretUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_user (用户表) 单表服务类，代码重新生成不覆盖. 只建议完成单表的逻辑，或主表为 iam_user 的逻辑. 其他逻辑放 custom 中
 */
 
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

    public IamUser update(IamUser entity) {
        Assert.notNull(entity.getId(), "请求错误！参数[id]不能为空");
        Assert.notNull(entity.getVersion(), "请求错误！参数[version]不能为空");
        IamUser oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        IamUser.copyIfNotNull(entity, oldEntity);
        updateByIdSelective(oldEntity);
        return oldEntity;
    }

    public IamUser remove(IamUser entity) {
        Assert.notNull(entity.getId(), "请求错误！参数[id]不能为空");
        IamUser oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        deleteById(oldEntity);
        return oldEntity;
    }


    @Transactional(rollbackFor = Exception.class)
    public IamUserDto customCreate(IamUserDto dto) {
        // 用户重复判断
        IamUser param = new IamUser();
        param.setUsername(dto.getUsername());
        long userCount = selectCountByEntity(param);

        if (userCount > 0) {
            throw UserException.of("用户已存在!");
        }

        // 用户入库
        dto.setUserCode(redisIdGenerator.generateIdWithPrefix("user_"));
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
        pwd.setLastChangedTime(LocalDateTime.now());
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

