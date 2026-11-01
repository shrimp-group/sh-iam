package com.wkclz.iam.session.bean.auth;

import com.wkclz.iam.session.bean.ApiRequestControl;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

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
     * 应用内全部菜单（MENU 与 BUTTON）
     */
    @Schema(description = "应用内全部菜单")
    private List<AuthMenu> menus;

    /**
     * 角色菜单关系：roleCode → menuCodes
     */
    @Schema(description = "角色菜单关系：roleCode → menuCodes")
    private Map<String, Set<String>> roleMenuMap;

    /**
     * 菜单 API 关系：menuCode → apis（权限计算与纳管判定依据）
     */
    @Schema(description = "菜单 API 关系：menuCode → apis")
    private Map<String, List<AuthApi>> menuApiMap;

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
}
