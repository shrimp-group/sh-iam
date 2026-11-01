package com.wkclz.iam.session.bean.auth;

import com.wkclz.iam.session.bean.ApiRequestControl;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 单应用静态资源聚合模型（iam-session 模块自持，不依赖 iam-common）。
 *
 * <p>缓存与加载约定：按应用整体一次性加载（L1 静态资源缓存，键为 appCode），
 * 不设 TTL，仅变更指令精确清理与容量 LRU；后续扩展其他应用静态信息（如数据维度）
 * 只需增加字段并在 facade 装配侧补数据，缓存结构不变。</p>
 *
 * @author shrimp
 */
@Data
public class AppAuthResource {

    /**
     * 应用编码
     */
    @Schema(description = "应用编码")
    private String appCode;

    /**
     * 加载时间戳（毫秒）
     */
    @Schema(description = "加载时间戳（毫秒）")
    private long loadTime;

    /**
     * 应用内全部角色编码
     */
    @Schema(description = "应用内全部角色编码")
    private Set<String> roleCodes;

    /**
     * 应用内全部菜单：menuCode → 节点（LinkedHashMap 保 sort 序，MENU 与 BUTTON 两类）。
     * <p>快照不可变约束：缓存内对象为多请求共享实例，消费方不得修改其字段（组树须浅拷贝节点）。</p>
     */
    @Schema(description = "应用内全部菜单：menuCode → AuthMenu")
    private Map<String, AuthMenu> menuMap;

    /**
     * 角色菜单关系：roleCode → menuCodes（一对多）
     */
    @Schema(description = "角色菜单关系：roleCode → menuCodes")
    private Map<String, Set<String>> roleMenuMap;

    /**
     * 应用内 API 定义：apiCode → 定义（method/uri，权限匹配依据）
     */
    @Schema(description = "应用内 API 定义：apiCode → AuthApi")
    private Map<String, AuthApi> apiMap;

    /**
     * 菜单 API 关系：menuCode → apiCodes（一对多，与 roleMenuMap 同构）
     */
    @Schema(description = "菜单 API 关系：menuCode → apiCodes")
    private Map<String, Set<String>> menuApiMap;

    /**
     * 接口限流配置条目（对应 iam_api.request_control）
     */
    @Schema(description = "接口限流配置条目")
    private List<ApiControlEntry> requestControls;

    /**
     * 接口字段权限（对应 iam_api_field）
     */
    @Schema(description = "接口字段权限")
    private List<AuthApiField> apiFields;

    /**
     * 接口限流配置条目。
     *
     * @param apiCode        API 编码
     * @param apiMethod      HTTP 方法
     * @param apiUri         API URI（支持 Ant 通配）
     * @param requestControl 请求控制配置（已解析）
     */
    public record ApiControlEntry(String apiCode, String apiMethod, String apiUri,
                                  ApiRequestControl requestControl) {
    }

    /**
     * 构造空资源（集合字段均为可变空集合，避免调用方 NPE），供 NoOp 兜底实现使用。
     *
     * @param appCode 应用编码
     * @return 空静态资源
     */
    public static AppAuthResource empty(String appCode) {
        AppAuthResource resource = new AppAuthResource();
        resource.setAppCode(appCode);
        resource.setLoadTime(System.currentTimeMillis());
        resource.setRoleCodes(new LinkedHashSet<>());
        resource.setMenuMap(new LinkedHashMap<>());
        resource.setRoleMenuMap(new HashMap<>());
        resource.setApiMap(new HashMap<>());
        resource.setMenuApiMap(new HashMap<>());
        resource.setRequestControls(new ArrayList<>());
        resource.setApiFields(new ArrayList<>());
        return resource;
    }
}
