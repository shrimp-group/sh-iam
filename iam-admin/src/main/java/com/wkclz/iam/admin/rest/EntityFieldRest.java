package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.helper.EntityFieldAnalyzer;
import com.wkclz.iam.admin.bean.resp.EntityFieldNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 实体字段分析接口
 * 提供实体类字段树查询和可用实体类列表
 */
@RestController
@RequestMapping(Route.PREFIX)
public class EntityFieldRest {

    private static final Logger log = LoggerFactory.getLogger(EntityFieldRest.class);

    @Autowired
    private EntityFieldAnalyzer entityFieldAnalyzer;

    /**
     * 查询实体字段树
     */
    @GetMapping(Route.ENTITY_FIELD_TREE)
    public R entityFieldTree(@RequestParam String className) {
        log.info("查询实体字段树, className={}", className);
        List<EntityFieldNode> tree = entityFieldAnalyzer.analyze(className);
        return R.ok(tree);
    }

    /**
     * 查询可用实体类列表
     */
    @GetMapping(Route.ENTITY_FIELD_CLASSES)
    public R entityFieldClasses() {
        log.info("查询可用实体类列表");
        List<Map<String, String>> classes = entityFieldAnalyzer.getEntityClasses();
        return R.ok(classes);
    }

}
