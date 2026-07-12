package com.wkclz.iam.admin.service;

import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.mapper.IamUserAuthPasswordMapper;
import com.wkclz.iam.admin.mapper.IamUserPasswordHisMapper;
import com.wkclz.iam.common.entity.IamUserAuthPassword;
import com.wkclz.iam.common.entity.IamUserPasswordHis;
import com.wkclz.auth.contract.auth.Md5PasswordEncoder;
import com.wkclz.mybatis.service.BaseService;
import com.wkclz.tool.utils.SecretUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_user_auth_password (密码认证表) 单表服务类，代码重新生成不覆盖. 只建议完成单表的逻辑，或主表为 iam_user_auth_password 的逻辑. 其他逻辑放 custom 中
 */
 
@Service
public class IamUserAuthPasswordService extends BaseService<IamUserAuthPassword, IamUserAuthPasswordMapper> {

    @Autowired
    private IamUserPasswordHisMapper iamUserPasswordHisMapper;

    @Autowired
    private Md5PasswordEncoder passwordEncoder;

    public IamUserAuthPassword create(IamUserAuthPassword entity) {
        duplicateCheck(entity);
        mapper.insert(entity);
        return entity;
    }

    public IamUserAuthPassword update(IamUserAuthPassword entity) {
        duplicateCheck(entity);
        Assert.notNull(entity.getId(), "请求错误！参数[id]不能为空");
        Assert.notNull(entity.getVersion(), "请求错误！参数[version]不能为空");
        IamUserAuthPassword oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        IamUserAuthPassword.copyIfNotNull(entity, oldEntity);
        updateByIdSelective(oldEntity);
        return oldEntity;
    }

    public IamUserAuthPassword save(IamUserAuthPassword entity) {
        return entity.getId() == null ? create(entity) : update(entity);
    }

    public IamUserAuthPassword remove(IamUserAuthPassword entity) {
        Assert.notNull(entity.getId(), "请求错误！参数[id]不能为空");
        IamUserAuthPassword oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        deleteById(oldEntity);
        return oldEntity;
    }

    private void duplicateCheck(IamUserAuthPassword entity) {
        // 唯一条件为空，直接通过
        if (true) {
            return;
        }
        
        // 唯一条件不为空，请设置唯一条件
        IamUserAuthPassword param = new IamUserAuthPassword();
        // 唯一条件
        param = selectOneByEntity(param);
        if (param == null) {
            return;
        }
        if (param.getId().equals(entity.getId())) {
            return;
        }
        // 查到有值，为新增或 id 不一样场景，为数据重复
        throw UserException.of(ResultCode.RECORD_DUPLICATE);
    }


    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String userCode, String newPassword) {
        Assert.notNull(userCode, "用户编码不能为空");
        Assert.notNull(newPassword, "新密码不能为空");

        IamUserAuthPassword currentPwd = mapper.selectByUserCode(userCode);
        if (currentPwd == null) {
            throw UserException.of("用户密码记录不存在");
        }

        List<IamUserPasswordHis> historyList = iamUserPasswordHisMapper.selectByUserCodeOrderByCreateTimeDesc(userCode, 3);
        if (isPasswordInHistory(newPassword, historyList)) {
            throw UserException.of("新密码不能与最近3次使用过的密码相同");
        }

        String newSalt = SecretUtil.getKey();
        String encryptedPassword = passwordEncoder.encode(newPassword, newSalt);

        IamUserAuthPassword updatePwd = new IamUserAuthPassword();
        updatePwd.setId(currentPwd.getId());
        updatePwd.setVersion(currentPwd.getVersion());
        updatePwd.setPassword(encryptedPassword);
        updatePwd.setSalt(newSalt);
        updatePwd.setLastChangedTime(LocalDateTime.now());
        mapper.updateByIdSelective(updatePwd);

        IamUserPasswordHis his = new IamUserPasswordHis();
        his.setUserCode(userCode);
        his.setPassword(encryptedPassword);
        his.setSalt(newSalt);
        iamUserPasswordHisMapper.insert(his);
    }


    private boolean isPasswordInHistory(String newPassword, List<IamUserPasswordHis> historyList) {
        if (newPassword == null || historyList == null || historyList.isEmpty()) {
            return false;
        }
        for (IamUserPasswordHis his : historyList) {
            if (passwordEncoder.matches(newPassword, his.getSalt(), his.getPassword())) {
                return true;
            }
        }
        return false;
    }
}

