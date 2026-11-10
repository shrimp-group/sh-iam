package com.wkclz.iam.admin.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.wkclz.core.base.PageData;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.bean.req.RoleUserBindReq;
import com.wkclz.iam.admin.bean.req.RoleUserPageReq;
import com.wkclz.iam.admin.bean.req.UserRoleBindReq;
import com.wkclz.iam.admin.bean.resp.RoleUserResp;
import com.wkclz.iam.admin.bean.resp.UserMenuSourceResp;
import com.wkclz.iam.admin.bean.resp.UserRoleResp;
import com.wkclz.iam.admin.mapper.IamUserRoleMapper;
import com.wkclz.iam.common.dto.IamUserRoleDto;
import com.wkclz.iam.common.entity.IamRole;
import com.wkclz.iam.common.entity.IamUserRole;
import com.wkclz.iam.session.event.PermissionChangeEvent;
import com.wkclz.mybatis.helper.PageQuery;
import com.wkclz.mybatis.service.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_user_role (用户-角色关系) 单表服务类，代码重新生成不覆盖. 只建议完成单表的逻辑，或主表为 iam_user_role 的逻辑. 其他逻辑放 custom 中
 */

@Service
public class IamUserRoleService extends BaseService<IamUserRole, IamUserRoleMapper> {

    private static final Logger log = LoggerFactory.getLogger(IamUserRoleService.class);

    @Autowired
    private IamRoleService iamRoleService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 绑定用户与角色（含有效期）
     */
    @Transactional(rollbackFor = Exception.class)
    public IamUserRole bind(UserRoleBindReq req) {
        log.info("绑定用户角色, userCode={}, roleCode={}, startTime={}, endTime={}",
                req.getUserCode(), req.getRoleCode(), req.getStartTime(), req.getEndTime());

        // 查询角色信息获取 appCode 和 tenantCode
        IamRole roleParam = new IamRole();
        roleParam.setRoleCode(req.getRoleCode());
        IamRole role = iamRoleService.selectOneByEntity(roleParam);
        Assert.notNull(role, "角色不存在!");

        IamUserRole entity = new IamUserRole();
        entity.setTenantCode(role.getTenantCode());
        entity.setAppCode(role.getAppCode());
        entity.setUserCode(req.getUserCode());
        entity.setRoleCode(req.getRoleCode());
        entity.setStartTime(req.getStartTime());
        entity.setEndTime(req.getEndTime());
        // 根据当前时间设置 enableStatus
        entity.setEnableStatus(computeEnableStatus(req.getStartTime(), req.getEndTime()));

        mapper.insert(entity);
        log.info("用户角色绑定成功, id={}", entity.getId());
        publishUserChanged(req.getUserCode());
        return entity;
    }

    /**
     * 解绑用户与角色
     */
    @Transactional(rollbackFor = Exception.class)
    public IamUserRole unbind(Long id) {
        log.info("解绑用户角色, id={}", id);
        Assert.notNull(id, "请求错误！参数[id]不能为空");
        IamUserRole oldEntity = selectById(id);
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        deleteById(oldEntity);
        log.info("用户角色解绑成功, userCode={}, roleCode={}", oldEntity.getUserCode(), oldEntity.getRoleCode());
        // 发布用户级权限变更事件 → 权限缓存清理
        publishUserChanged(oldEntity.getUserCode());
        return oldEntity;
    }

    /**
     * 发布用户级权限变更事件（userCode 为空跳过）
     */
    private void publishUserChanged(String userCode) {
        if (userCode == null || userCode.isEmpty()) {
            log.warn("userCode 为空，跳过权限变更发布");
            return;
        }
        eventPublisher.publishEvent(PermissionChangeEvent.userChanged(userCode));
        log.info("已发布权限变更事件: type=USER, key={}", userCode);
    }

    /**
     * 批量绑定用户到角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchBind(RoleUserBindReq req) {
        log.info("批量绑定用户到角色, roleCode={}, userCodes={}", req.getRoleCode(), req.getUserCodes());

        // 查询角色信息获取 appCode 和 tenantCode
        IamRole roleParam = new IamRole();
        roleParam.setRoleCode(req.getRoleCode());
        IamRole role = iamRoleService.selectOneByEntity(roleParam);
        Assert.notNull(role, "角色不存在!");

        Integer enableStatus = computeEnableStatus(req.getStartTime(), req.getEndTime());

        List<IamUserRole> list = req.getUserCodes().stream().map(userCode -> {
            IamUserRole entity = new IamUserRole();
            entity.setTenantCode(role.getTenantCode());
            entity.setAppCode(role.getAppCode());
            entity.setUserCode(userCode);
            entity.setRoleCode(req.getRoleCode());
            entity.setStartTime(req.getStartTime());
            entity.setEndTime(req.getEndTime());
            entity.setEnableStatus(enableStatus);
            return entity;
        }).toList();

        mapper.insertBatch(list);
        log.info("批量绑定用户到角色完成, roleCode={}, 绑定{}条", req.getRoleCode(), list.size());
        list.forEach(entity -> publishUserChanged(entity.getUserCode()));
    }

    /**
     * 批量解绑
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchUnbind(List<Long> ids) {
        log.info("批量解绑用户角色, ids={}", ids);
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<IamUserRole> oldEntities = selectByIds(ids);
        deleteByIds(ids);
        log.info("批量解绑用户角色完成, 解绑{}条", ids.size());
        oldEntities.forEach(entity -> publishUserChanged(entity.getUserCode()));
    }

    /**
     * 查询用户角色列表（含有效期、角色名称、enableStatus）
     */
    public List<UserRoleResp> getUserRoleList(String userCode, String roleCode) {
        log.info("查询用户角色列表, userCode={}, roleCode={}", userCode, roleCode);
        return mapper.getUserRoleList(userCode, roleCode);
    }

    /**
     * 角色下用户分页查询（支持用户名精确匹配、姓名模糊搜索）
     */
    public PageData<RoleUserResp> getRoleUserPage(RoleUserPageReq req) {
        log.info("角色下用户分页查询, request={}", req);
        PageHelper.startPage(req.getCurrent().intValue(), req.getSize().intValue());
        try {
            List<RoleUserResp> list = mapper.getRoleUserPage(req);
            Page<RoleUserResp> page = (Page<RoleUserResp>) list;
            return PageData.of(page.getResult(), page.getTotal());
        } finally {
            PageHelper.clearPage();
        }
    }

    /**
     * 用户角色树查询（按应用，含绑定数量标记）
     */
    public List<IamUserRoleDto> getUserRoleTree(String userCode, String appCode) {
        log.info("查询用户角色树, userCode={}, appCode={}", userCode, appCode);
        List<IamUserRoleDto> roles = mapper.getUserRoleTree(userCode, appCode);
        return buildRoleTree(roles);
    }

    /**
     * 构建角色树
     */
    private List<IamUserRoleDto> buildRoleTree(List<IamUserRoleDto> roles) {
        List<IamUserRoleDto> tree = new ArrayList<>();
        Map<String, IamUserRoleDto> roleMap = roles.stream()
                .collect(Collectors.toMap(IamUserRoleDto::getRoleCode, t -> t));

        for (IamUserRoleDto role : roles) {
            String parentCode = role.getParentCode();
            if ("0".equals(parentCode)) {
                tree.add(role);
            } else {
                IamUserRoleDto parentNode = roleMap.get(parentCode);
                if (parentNode != null) {
                    List<IamUserRoleDto> children = parentNode.getChildren();
                    if (children == null) {
                        children = new ArrayList<>();
                        parentNode.setChildren(children);
                    }
                    parentNode.getChildren().add(role);
                }
            }
        }
        return tree;
    }

    /**
     * 定时任务调用，扫描并更新 enableStatus
     */
    public void refreshEnableStatus() {
        log.info("开始刷新用户角色绑定状态...");
        // 先查出生效状态将发生变化的用户，更新后逐个发布权限变更
        List<String> enableCandidates = mapper.listEnableCandidates();
        List<String> disableCandidates = mapper.listDisableCandidates();
        int enabled = mapper.enableExpiredBindings();
        int disabled = mapper.disableExpiredBindings();
        log.info("用户角色绑定状态刷新完成, 生效{}条, 失效{}条", enabled, disabled);
        enableCandidates.forEach(userCode -> {
            log.info("绑定到期生效, 发布权限变更: userCode={}", userCode);
            publishUserChanged(userCode);
        });
        disableCandidates.forEach(userCode -> {
            log.info("绑定到期失效, 发布权限变更: userCode={}", userCode);
            publishUserChanged(userCode);
        });
    }

    /**
     * 查询用户菜单来源角色信息
     */
    public List<UserMenuSourceResp> getUserMenuSourceList(String userCode, String appCode) {
        log.info("查询用户菜单来源角色, userCode={}, appCode={}", userCode, appCode);
        return mapper.getUserMenuSourceList(userCode, appCode);
    }

    /**
     * 根据当前时间与有效期计算绑定状态
     */
    private Integer computeEnableStatus(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        return (now.isEqual(startTime) || now.isAfter(startTime)) && (now.isEqual(endTime) || now.isBefore(endTime)) ? 1 : 0;
    }

}
