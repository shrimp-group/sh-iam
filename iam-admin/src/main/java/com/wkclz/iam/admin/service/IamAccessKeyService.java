package com.wkclz.iam.admin.service;

import com.wkclz.core.base.PageData;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.mapper.IamAccessKeyMapper;
import com.wkclz.iam.common.entity.IamAccessKey;
import com.wkclz.mybatis.helper.PageQuery;
import com.wkclz.mybatis.service.BaseService;
import com.wkclz.redis.helper.RedisIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.security.SecureRandom;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_access_key (AK 密钥) 单表服务类，代码重新生成不覆盖. 只建议完成单表的逻辑，或主表为 iam_access_key 的逻辑. 其他逻辑放 custom 中
 */
 
@Service
public class IamAccessKeyService extends BaseService<IamAccessKey, IamAccessKeyMapper> {


    private static final int AK_LENGTH = 16;
    private static final int SK_LENGTH = 32;
    private static final String APPID_PREFIX = "app_";
    private static final String AK_PREFIX = "AK_";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @Autowired
    private RedisIdGenerator redisIdGenerator;

    public PageData<IamAccessKey> getAccessKeyPage(IamAccessKey entity) {
        return PageQuery.page(entity, mapper::getAccessKeyList);
    }


    public IamAccessKey create(IamAccessKey entity) {
        String appId = redisIdGenerator.generateIdWithPrefix(APPID_PREFIX);
        String ak = AK_PREFIX + generateRandomString(AK_LENGTH);
        String sk = generateRandomString(SK_LENGTH);

        entity.setAppId(appId);
        entity.setAccessKey(ak);
        entity.setSecretKey(sk);

        mapper.insert(entity);
        return entity;
    }

    public IamAccessKey update(IamAccessKey entity) {
        Assert.notNull(entity.getId(), "请求错误！参数[id]不能为空");
        Assert.notNull(entity.getVersion(), "请求错误！参数[version]不能为空");
        IamAccessKey oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        entity.setAppId(null);
        entity.setAccessKey(null);
        entity.setSecretKey(null);
        IamAccessKey.copyIfNotNull(entity, oldEntity);
        updateByIdSelective(oldEntity);
        return oldEntity;
    }

    public IamAccessKey save(IamAccessKey entity) {
        return entity.getId() == null ? create(entity) : update(entity);
    }

    public IamAccessKey remove(IamAccessKey entity) {
        Assert.notNull(entity.getId(), "请求错误！参数[id]不能为空");
        IamAccessKey oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        deleteById(oldEntity);
        return oldEntity;
    }


    private static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = SECURE_RANDOM.nextInt(CHARS.length());
            sb.append(CHARS.charAt(index));
        }
        return sb.toString();
    }

}

