package com.wkclz.iam.admin.service;

import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.mapper.IamRoleMapper;
import com.wkclz.iam.common.dto.IamRoleDto;
import com.wkclz.iam.common.entity.IamRole;
import com.wkclz.mybatis.service.BaseService;
import com.wkclz.redis.helper.RedisIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_role (角色) 单表服务类，代码重新生成不覆盖. 只建议完成单表的逻辑，或主表为 iam_role 的逻辑. 其他逻辑放 custom 中
 */
 
@Service
public class IamRoleService extends BaseService<IamRole, IamRoleMapper> {

    private static final Logger log = LoggerFactory.getLogger(IamRoleService.class);


    @Autowired
    private RedisIdGenerator redisIdGenerator;


    public List<IamRoleDto> roleList(IamRole entity) {
        return mapper.getAppRoleList(entity.getAppCode());
    }

    public List<IamRoleDto> roleTree(IamRole entity) {
        // 查询所有角色
        List<IamRoleDto> roles = mapper.getAppRole4Tree(entity.getAppCode());
        // 构建角色树
        return buildRoleTree(roles);
    }

    private List<IamRoleDto> buildRoleTree(List<IamRoleDto> roles) {
        List<IamRoleDto> tree = new ArrayList<>();
        Map<String, IamRoleDto> roleMap = roles.stream()
                .collect(Collectors.toMap(IamRoleDto::getRoleCode, t -> t, (v1, v2) -> v1, LinkedHashMap::new));

        for (IamRoleDto roleDto : roleMap.values()) {
            String parentCode = roleDto.getParentCode();
            // 如果是顶级角色（父编码为"0"），直接放入tree
            if ("0".equals(parentCode)) {
                tree.add(roleDto);
            } else {
                // 否则，放入父角色的children列表
                IamRoleDto parentNode = roleMap.get(parentCode);
                if (parentNode != null) {
                    List<IamRoleDto> children = parentNode.getChildren();
                    if (children == null) {
                        children = new ArrayList<>();
                        parentNode.setChildren(children);
                    }
                    parentNode.getChildren().add(roleDto);
                }
            }
        }
        return tree;
    }


    public IamRole create(IamRole entity) {
        duplicateCheck(entity);
        entity.setRoleCode(redisIdGenerator.generateIdWithPrefix("role_"));
        mapper.insert(entity);
        return entity;
    }

    public IamRole update(IamRole entity) {
        duplicateCheck(entity);
        IamRole oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        IamRole.copyIfNotNull(entity, oldEntity);
        updateByIdSelective(oldEntity);
        return oldEntity;
    }

    public IamRole save(IamRole entity) {
        return entity.getId() == null ? create(entity) : update(entity);
    }

    public IamRole remove(IamRole entity) {
        IamRole oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        deleteById(oldEntity);
        return oldEntity;
    }

    private void duplicateCheck(IamRole entity) {
        // 唯一条件为空，直接通过
        if (StringUtils.isBlank(entity.getRoleCode())) {
            return;
        }
        
        // 唯一条件不为空，请设置唯一条件
        IamRole param = new IamRole();
        // 唯一条件：roleCode + appCode
        param.setRoleCode(entity.getRoleCode());
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

}

