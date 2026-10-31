package com.wkclz.iam.admin.service;

import com.google.common.collect.Lists;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.bean.req.MenuExportReq;
import com.wkclz.iam.admin.bean.req.MenuImportReq;
import com.wkclz.iam.admin.bean.resp.MenuDetailResp;
import com.wkclz.iam.admin.bean.resp.MenuExportResp;
import com.wkclz.iam.admin.bean.resp.MenuImportResp;
import com.wkclz.iam.admin.mapper.IamMenuApiMapper;
import com.wkclz.iam.admin.mapper.IamMenuMapper;
import com.wkclz.iam.common.dto.IamMenuDto;
import com.wkclz.iam.common.entity.IamMenu;
import com.wkclz.iam.common.entity.IamMenuApi;
import com.wkclz.mybatis.service.BaseService;
import com.wkclz.redis.helper.RedisIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_menu (菜单) 单表服务类，代码重新生成不覆盖. 只建议完成单表的逻辑，或主表为 iam_menu 的逻辑. 其他逻辑放 custom 中
 */

@Service
public class IamMenuService extends BaseService<IamMenu, IamMenuMapper> {

    private static final Logger log = LoggerFactory.getLogger(IamMenuService.class);

    /**
     * 导入批量分块大小，避免单 SQL 过大
     */
    private static final int IMPORT_BATCH_SIZE = 200;

    /**
     * 菜单类型合法值
     */
    private static final Set<String> MENU_TYPES = Set.of("MENU", "BUTTON", "FIELDS");

    @Autowired
    private RedisIdGenerator redisIdGenerator;

    @Autowired
    private IamMenuFieldService iamMenuFieldService;

    @Autowired
    private IamMenuApiMapper iamMenuApiMapper;

    public List<IamMenuDto> menuList(IamMenu entity) {
        return mapper.getAppMenuList(entity.getAppCode());
    }


    public List<IamMenuDto> menuTree(IamMenu entity) {
        log.info("查询菜单树, entity={}", entity);
        // 查询所有菜单
        List<IamMenuDto> menus = mapper.getAppMenu4Tree(entity.getAppCode());
        // 构建菜单树
        return buildMenuTree(menus);
    }

    /**
     * 菜单导出：ids 有值时导出选中节点及其全部子孙（内存沿 parentCode 递归补齐），否则按 appCode 全量导出；
     * 菜单与绑定各一次批量查询，递归仅内存进行，apiCodes 按 menuCode 分组挂接
     *
     * @param req 导出请求
     * @return 导出数据（树形嵌套，节点含 apiCodes 与递归 children）
     */
    public List<MenuExportResp> getMenus4Export(MenuExportReq req) {
        log.info("导出菜单, ids={}, appCode={}", req.getIds(), req.getAppCode());

        // 主数据查询：选中模式先按主键查选中节点、再按其 appCode 集合一次批量查全量用于内存子孙补齐；全量模式直接按 appCode
        List<IamMenu> selected = Collections.emptyList();
        List<IamMenuDto> allMenus;
        if (!CollectionUtils.isEmpty(req.getIds())) {
            selected = mapper.selectByIds(req.getIds());
            if (selected.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> appCodes = selected.stream().map(IamMenu::getAppCode).distinct().toList();
            allMenus = mapper.getMenusByAppCodes(appCodes);
        } else {
            allMenus = mapper.getAppMenu4Tree(req.getAppCode());
        }
        if (allMenus.isEmpty()) {
            return Collections.emptyList();
        }

        // 选中模式：内存建 parentCode → 子节点 索引，从选中节点出发递归收集全部子孙
        List<IamMenuDto> exportMenus = allMenus;
        if (!CollectionUtils.isEmpty(req.getIds())) {
            Map<String, List<IamMenuDto>> childrenIndex = new HashMap<>();
            for (IamMenuDto menu : allMenus) {
                childrenIndex.computeIfAbsent(menu.getParentCode(), k -> new ArrayList<>()).add(menu);
            }
            Set<String> keep = new HashSet<>();
            for (IamMenu menu : selected) {
                collectDescendants(menu.getMenuCode(), childrenIndex, keep);
            }
            exportMenus = allMenus.stream().filter(menu -> keep.contains(menu.getMenuCode())).toList();
        }

        // 绑定关系一次 IN 批量查询，按 menuCode 分组
        List<String> menuCodes = exportMenus.stream().map(IamMenu::getMenuCode).toList();
        Map<String, List<IamMenuApi>> bindMap = iamMenuApiMapper.getBindingsByMenuCodes(menuCodes).stream()
            .collect(Collectors.groupingBy(IamMenuApi::getMenuCode));

        // 组装导出树：SQL 已按 sort,id 排序，按查询顺序挂接即保持同级 sort 升序、id 升序；父级不在导出集合内的节点为根
        Map<String, MenuExportResp> respMap = new LinkedHashMap<>();
        for (IamMenuDto menu : exportMenus) {
            respMap.put(menu.getMenuCode(), buildExportNode(menu, bindMap));
        }
        List<MenuExportResp> tree = new ArrayList<>();
        for (MenuExportResp node : respMap.values()) {
            String parentCode = node.getParentCode();
            MenuExportResp parent = StringUtils.isBlank(parentCode) ? null : respMap.get(parentCode);
            if (parent == null || parent == node) {
                tree.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }

        log.info("导出菜单完成, 菜单数={}, 绑定数={}", exportMenus.size(),
            bindMap.values().stream().mapToInt(List::size).sum());
        return tree;
    }

    /**
     * 菜单导入：按 menuCode 判断存在性，存在更新、不存在新增；对显式携带 apiCodes 的菜单做绑定全量快照重建；
     * parentCode 引用缺失的节点置为顶级并提示。菜单全程无删除，不查询、不改动 iam_api
     *
     * @param req 导入请求
     * @return 新增/更新菜单数、重建绑定数与 parentCode 修复提示
     */
    @Transactional(rollbackFor = Exception.class)
    public MenuImportResp menuImport(MenuImportReq req) {
        log.info("导入菜单, 顶层条数={}", req.getMenus().size());

        // 先序展开整棵树（父在子前），带层级标签用于校验错误定位
        List<FlatItem> items = new ArrayList<>();
        flattenMenus(req.getMenus(), "", items);
        validateImport(items);

        // 一次批量查询已有菜单：导入存在性判断与 parentCode 存在性判断共用
        Set<String> fileMenuCodes = items.stream().map(item -> item.node().getMenuCode()).collect(Collectors.toSet());
        Set<String> queryCodes = new HashSet<>(fileMenuCodes);
        for (FlatItem item : items) {
            String parentCode = item.node().getParentCode();
            if (StringUtils.isNotBlank(parentCode) && !"0".equals(parentCode)) {
                queryCodes.add(parentCode);
            }
        }
        Map<String, IamMenu> existingMap = mapper.getMenusByCodes(new ArrayList<>(queryCodes)).stream()
            .collect(Collectors.toMap(IamMenu::getMenuCode, t -> t, (a, b) -> a));

        // parentCode 修复：既不在文件内也不在目标库中 → 置为顶级并提示
        List<String> parentFixHints = new ArrayList<>();
        for (FlatItem item : items) {
            MenuImportReq.MenuNode node = item.node();
            String parentCode = node.getParentCode();
            if (StringUtils.isNotBlank(parentCode) && !"0".equals(parentCode)
                && !fileMenuCodes.contains(parentCode) && !existingMap.containsKey(parentCode)) {
                node.setParentCode("0");
                parentFixHints.add("菜单 " + node.getMenuCode() + "(" + node.getMenuName() + ") 的父级不存在，已置为顶级");
            }
        }

        // 划分新增/更新：menuCode 已存在 → 更新（全量覆盖），否则新增（先序展开即父先子后）
        List<IamMenu> inserts = new ArrayList<>();
        List<IamMenu> updates = new ArrayList<>();
        for (FlatItem item : items) {
            IamMenu entity = buildImportEntity(item.node());
            IamMenu existing = existingMap.get(item.node().getMenuCode());
            if (existing == null) {
                inserts.add(entity);
            } else {
                entity.setId(existing.getId());
                updates.add(entity);
            }
        }
        for (List<IamMenu> chunk : Lists.partition(inserts, IMPORT_BATCH_SIZE)) {
            mapper.insertBatch(chunk);
        }
        for (List<IamMenu> chunk : Lists.partition(updates, IMPORT_BATCH_SIZE)) {
            mapper.updateBatch4Import(chunk);
        }

        // 绑定全量快照重建：仅对显式携带 apiCodes 的菜单先清后插，不做接口存在性过滤
        List<FlatItem> bindItems = items.stream().filter(item -> item.node().getApiCodes() != null).toList();
        List<IamMenuApi> bindings = new ArrayList<>();
        if (!bindItems.isEmpty()) {
            List<String> bindMenuCodes = bindItems.stream().map(item -> item.node().getMenuCode()).distinct().toList();
            iamMenuApiMapper.deleteByMenuCodes(bindMenuCodes);
            for (FlatItem item : bindItems) {
                MenuImportReq.MenuNode node = item.node();
                for (String apiCode : node.getApiCodes()) {
                    IamMenuApi binding = new IamMenuApi();
                    binding.setAppCode(node.getAppCode());
                    binding.setMenuCode(node.getMenuCode());
                    binding.setApiCode(apiCode);
                    bindings.add(binding);
                }
            }
            for (List<IamMenuApi> chunk : Lists.partition(bindings, IMPORT_BATCH_SIZE)) {
                iamMenuApiMapper.insertBatch(chunk);
            }
        }

        MenuImportResp resp = new MenuImportResp();
        resp.setInsertMenuCount((long) inserts.size());
        resp.setUpdateMenuCount((long) updates.size());
        resp.setRebuildBindCount((long) bindings.size());
        resp.setParentFixHints(parentFixHints);
        log.info("导入菜单完成, 新增菜单={}, 更新菜单={}, 重建绑定={}, 父级修复数={}",
            resp.getInsertMenuCount(), resp.getUpdateMenuCount(), resp.getRebuildBindCount(), parentFixHints.size());
        return resp;
    }

    /**
     * 从指定菜单出发，沿 parentCode 递归收集全部子孙的 menuCode（防御循环引用，已收集的直接返回）
     */
    private void collectDescendants(String menuCode, Map<String, List<IamMenuDto>> childrenIndex, Set<String> collected) {
        if (!collected.add(menuCode)) {
            return;
        }
        List<IamMenuDto> children = childrenIndex.get(menuCode);
        if (CollectionUtils.isEmpty(children)) {
            return;
        }
        for (IamMenuDto child : children) {
            collectDescendants(child.getMenuCode(), childrenIndex, collected);
        }
    }

    private MenuExportResp buildExportNode(IamMenu menu, Map<String, List<IamMenuApi>> bindMap) {
        MenuExportResp resp = new MenuExportResp();
        resp.setAppCode(menu.getAppCode());
        resp.setParentCode(menu.getParentCode());
        resp.setMenuCode(menu.getMenuCode());
        resp.setMenuName(menu.getMenuName());
        resp.setIcon(menu.getIcon());
        resp.setMenuType(menu.getMenuType());
        resp.setRoutePath(menu.getRoutePath());
        resp.setComponent(menu.getComponent());
        resp.setButtonCode(menu.getButtonCode());
        resp.setHidden(menu.getHidden());
        resp.setRemark(menu.getRemark());
        resp.setSort(menu.getSort());
        resp.setApiCodes(bindMap.getOrDefault(menu.getMenuCode(), Collections.emptyList()).stream()
            .map(IamMenuApi::getApiCode).toList());
        resp.setChildren(new ArrayList<>());
        return resp;
    }

    /**
     * 导入树先序展开项：节点 + 层级定位标签（用于校验错误信息定位条目）
     */
    private record FlatItem(MenuImportReq.MenuNode node, String tag) {
    }

    /**
     * 先序展开导入树：顶层标签「第N项(menuCode)」，子级在前缀上追加「>子级第M项(menuCode)」
     */
    private void flattenMenus(List<MenuImportReq.MenuNode> nodes, String prefix, List<FlatItem> items) {
        for (int i = 0; i < nodes.size(); i++) {
            MenuImportReq.MenuNode node = nodes.get(i);
            String menuCode = StringUtils.isBlank(node.getMenuCode()) ? "?" : node.getMenuCode();
            String tag = prefix + "第" + (i + 1) + "项(" + menuCode + ")";
            items.add(new FlatItem(node, tag));
            if (!CollectionUtils.isEmpty(node.getChildren())) {
                flattenMenus(node.getChildren(), tag + ">子级", items);
            }
        }
    }

    /**
     * 导入数据校验：逐条定位缺失项，任一不合法则整体不通过
     */
    private void validateImport(List<FlatItem> items) {
        List<String> errors = new ArrayList<>();
        Set<String> menuCodeSet = new HashSet<>();
        for (FlatItem item : items) {
            MenuImportReq.MenuNode node = item.node();
            if (StringUtils.isBlank(node.getMenuCode())) {
                errors.add(item.tag() + "：缺少菜单编码");
            }
            if (StringUtils.isBlank(node.getMenuName())) {
                errors.add(item.tag() + "：缺少菜单名称");
            }
            if (StringUtils.isBlank(node.getMenuType()) || !MENU_TYPES.contains(node.getMenuType())) {
                errors.add(item.tag() + "：菜单类型 " + StringUtils.defaultIfBlank(node.getMenuType(), "空") + " 不合法");
            }
            if (StringUtils.isNotBlank(node.getMenuCode()) && !menuCodeSet.add(node.getMenuCode())) {
                errors.add(item.tag() + "：菜单编码 " + node.getMenuCode() + " 重复出现");
            }
            if (!CollectionUtils.isEmpty(node.getApiCodes())) {
                for (String apiCode : node.getApiCodes()) {
                    if (StringUtils.isBlank(apiCode)) {
                        errors.add(item.tag() + "：接口编码不能为空");
                    }
                }
            }
        }
        if (!errors.isEmpty()) {
            log.warn("导入菜单校验失败, 错误数={}", errors.size());
            throw ValidationException.of(String.join("；", errors));
        }
    }

    private IamMenu buildImportEntity(MenuImportReq.MenuNode node) {
        IamMenu entity = new IamMenu();
        entity.setAppCode(node.getAppCode());
        entity.setParentCode(node.getParentCode());
        entity.setMenuCode(node.getMenuCode());
        entity.setMenuName(node.getMenuName());
        entity.setIcon(node.getIcon());
        entity.setMenuType(node.getMenuType());
        entity.setRoutePath(node.getRoutePath());
        entity.setComponent(node.getComponent());
        entity.setButtonCode(node.getButtonCode());
        entity.setHidden(node.getHidden());
        entity.setRemark(node.getRemark());
        entity.setSort(node.getSort());
        return entity;
    }

    public IamMenu create(IamMenu entity) {
        // 一级路由路径校验：必须以 / 开头
        validateRoutePath(entity);
        // FIELDS 类型校验：parentCode 必须指向 MENU 类型
        if ("FIELDS".equals(entity.getMenuType()) && !"0".equals(entity.getParentCode())) {
            IamMenu parentParam = new IamMenu();
            parentParam.setMenuCode(entity.getParentCode());
            IamMenu parentMenu = selectOneByEntity(parentParam);
            if (parentMenu == null || !"MENU".equals(parentMenu.getMenuType())) {
                throw ValidationException.of("FIELDS 类型菜单的父级必须是 MENU 类型");
            }
        }
        duplicateCheck(entity);
        entity.setMenuCode(redisIdGenerator.generateIdWithPrefix("menu_"));
        mapper.insert(entity);
        log.info("菜单创建成功, menuCode={}, menuType={}", entity.getMenuCode(), entity.getMenuType());
        return entity;
    }

    public IamMenu update(IamMenu entity) {
        // 一级路由路径校验：必须以 / 开头
        validateRoutePath(entity);
        duplicateCheck(entity);
        IamMenu oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        IamMenu.copyIfNotNull(entity, oldEntity);
        updateByIdSelective(oldEntity);
        return oldEntity;
    }

    public IamMenu save(IamMenu entity) {
        return entity.getId() == null ? create(entity) : update(entity);
    }

    public IamMenu remove(IamMenu entity) {
        IamMenu oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        // 检查菜单下是否有子菜单
        IamMenu param = new IamMenu();
        param.setParentCode(oldEntity.getMenuCode());
        long childrenMenuCount = mapper.selectCountByEntity(param);
        if (childrenMenuCount > 0) {
            throw ValidationException.of("请先删除子菜单");
        }
        // 如果删除的是 FIELDS 类型菜单，清理字段绑定
        if ("FIELDS".equals(oldEntity.getMenuType())) {
            iamMenuFieldService.deleteByMenuCode(oldEntity.getMenuCode());
            log.info("已清理字段组菜单的字段绑定, menuCode={}", oldEntity.getMenuCode());
        }
        deleteById(oldEntity);
        return oldEntity;
    }

    /**
     * 查询菜单详情
     *
     * @param id 菜单主键ID
     * @return 菜单详情响应
     */
    public MenuDetailResp getMenuDetail(Long id) {
        log.info("查询菜单详情, id={}", id);
        IamMenu menu = selectById(id);
        if (menu == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        MenuDetailResp resp = new MenuDetailResp();
        BeanUtils.copyProperties(menu, resp);
        return resp;
    }

    private void duplicateCheck(IamMenu entity) {
        // 唯一条件为空，直接通过
        if (StringUtils.isBlank(entity.getMenuCode())) {
            return;
        }

        // 唯一条件不为空，请设置唯一条件
        IamMenu param = new IamMenu();
        // 唯一条件：menuCode + appCode
        param.setMenuCode(entity.getMenuCode());
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

    /**
     * 一级路由路径校验：parentCode 为 "0" 时，routePath 必须以 / 开头
     */
    private void validateRoutePath(IamMenu entity) {
        if ("0".equals(entity.getParentCode()) && StringUtils.isNotBlank(entity.getRoutePath()) && !entity.getRoutePath().startsWith("/")) {
            throw ValidationException.of("一级路由的路径必须以 / 开头!");
        }
    }

    private List<IamMenuDto> buildMenuTree(List<IamMenuDto> menus) {
        List<IamMenuDto> tree = new ArrayList<>();
        Map<String, IamMenuDto> menuMap = menus.stream()
                .collect(Collectors.toMap(IamMenuDto::getMenuCode, t -> t, (v1, v2) -> v1, LinkedHashMap::new));

        for (IamMenuDto menuDto : menuMap.values()) {
            String parentCode = menuDto.getParentCode();
            // 如果是顶级菜单（父编码为"0"），直接放入tree
            if ("0".equals(parentCode)) {
                tree.add(menuDto);
            } else {
                // 否则，放入父菜单的children列表
                IamMenuDto parentNode = menuMap.get(parentCode);
                if (parentNode != null) {
                    List<IamMenuDto> children = parentNode.getChildren();
                    if (children == null) {
                        children = new ArrayList<>();
                        parentNode.setChildren(children);
                    }
                    parentNode.getChildren().add(menuDto);
                }
            }
        }
        assignLevel(tree, 1);
        return tree;
    }

    private void assignLevel(List<IamMenuDto> nodes, int level) {
        for (IamMenuDto node : nodes) {
            node.setLevel(level);
            if (node.getChildren() != null) {
                assignLevel(node.getChildren(), level + 1);
            }
        }
    }

}

