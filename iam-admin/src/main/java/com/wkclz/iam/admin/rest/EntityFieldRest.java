package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.resp.EntityFieldNode;
import com.wkclz.iam.admin.helper.EntityFieldAnalyzer;
import com.wkclz.web.bean.RestInfo;
import com.wkclz.web.helper.RestHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 实体字段分析接口
 * 提供基于 API 接口返回值的字段树查询
 */
@RestController
@RequestMapping(Route.PREFIX)
@Validated
@Tag(name = "实体字段分析", description = "实体字段分析接口")
public class EntityFieldRest {

    private static final Logger log = LoggerFactory.getLogger(EntityFieldRest.class);

    @Autowired
    private EntityFieldAnalyzer entityFieldAnalyzer;

    /**
     * 根据 API 自动定位返回值实体类并返回字段树
     * 1. 优先通过 returnGenericInfo 构建完整返回值字段树（含 R/PageData 包装层）
     * 2. 若 R 未指定泛型，则分析 R 类本身的结构返回给前端
     */
    @GetMapping(Route.ENTITY_FIELD_RESOLVE)
    @Operation(summary = "根据API定位实体类字段树")
    public R<Map<String, Object>> entityFieldResolve(
            @RequestParam @NotBlank(message = "method 不能为空") String method,
            @RequestParam @NotBlank(message = "uri 不能为空") String uri) {
        log.info("根据API定位实体类字段树, method={}, uri={}", method, uri);

        // 通过 RestHelper 匹配接口
        RestInfo matched = findRestInfo(method, uri);
        if (matched == null) {
            log.info("未找到匹配的接口, method={}, uri={}", method, uri);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "未找到匹配的接口，请手动维护字段");
            return R.ok(result);
        }

        // 有泛型信息时，构建完整返回值字段树
        if (matched.getReturnGenericInfo() != null && !matched.getReturnGenericInfo().isEmpty()) {
            List<EntityFieldNode> tree = entityFieldAnalyzer.buildReturnFieldTree(matched.getReturnGenericInfo());
            if (!tree.isEmpty()) {
                // 提取实体类信息
                Map<String, String> entityClass = entityFieldAnalyzer.parseReturnGenericInfo(matched.getReturnGenericInfo());
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("entityClass", entityClass != null ? entityClass : Map.of("className", "com.wkclz.core.base.R", "simpleName", "R"));
                result.put("fieldTree", tree);
                return R.ok(result);
            }
        }

        // R 未指定泛型，分析 R 类本身的结构返回给前端
        log.info("R未指定泛型，返回R类结构, method={}, uri={}", method, uri);
        List<EntityFieldNode> tree = entityFieldAnalyzer.analyzeRStructure();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("entityClass", Map.of("className", "com.wkclz.core.base.R", "simpleName", "R"));
        result.put("fieldTree", tree);
        result.put("message", "该接口未指定返回值泛型，展示R类结构供参考，请手动维护字段");
        return R.ok(result);
    }

    /**
     * 通过 RestHelper 查找匹配的 RestInfo
     */
    private RestInfo findRestInfo(String httpMethod, String uri) {
        List<RestInfo> mappings = RestHelper.getMapping();
        for (RestInfo restInfo : mappings) {
            if (httpMethod.equals(restInfo.getMethod()) && uri.equals(restInfo.getUri())) {
                return restInfo;
            }
        }
        return null;
    }

}
