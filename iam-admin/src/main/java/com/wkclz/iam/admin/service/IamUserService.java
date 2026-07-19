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
import com.wkclz.iam.common.event.AdminSecurityEvent;
import com.wkclz.iam.session.enums.AuthType;
import com.wkclz.iam.sso.spi.PasswordEncoder;
import com.wkclz.mybatis.helper.PageQuery;
import com.wkclz.mybatis.service.BaseService;
import com.wkclz.redis.helper.RedisIdGenerator;
import com.wkclz.tool.utils.BeanUtil;
import com.wkclz.tool.utils.SecretUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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

    private static final Logger log = LoggerFactory.getLogger(IamUserService.class);

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RedisIdGenerator redisIdGenerator;
    @Autowired
    private IamUserAuthMapper iamUserAuthMapper;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
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
        // 保存旧状态用于检测变更
        Integer oldStatus = oldEntity.getUserStatus();
        IamUser.copyIfNotNull(entity, oldEntity);
        updateByIdSelective(oldEntity);

        // 检测用户状态变更为禁用(2)或锁定(3)，发布事件
        if (entity.getUserStatus() != null
            && (entity.getUserStatus() == 2 || entity.getUserStatus() == 3)
            && !entity.getUserStatus().equals(oldStatus)) {
            eventPublisher.publishEvent(AdminSecurityEvent.statusChanged(oldEntity.getUserCode(), entity.getUserStatus()));
            log.info("用户状态变更 — 已发布 AdminSecurityEvent.STATUS_CHANGED: userCode={}, oldStatus={}, newStatus={}",
                oldEntity.getUserCode(), oldStatus, entity.getUserStatus());
        }

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
        if (StringUtils.isBlank(dto.getUserCode())) {
            dto.setUserCode(redisIdGenerator.generateIdWithPrefix("user_"));
        }
        IamUser user = BeanUtil.cp(dto, IamUser.class);
        insert(user);

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
        pwd.setPassword(passwordEncoder.encode(dto.getPassword(), pwd.getSalt()));
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

