package com.wkclz.iam.admin.helper;

import com.alibaba.fastjson2.JSONObject;
import com.wkclz.core.annotation.FieldDesc;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.bean.resp.EntityFieldNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 实体字段分析器
 * 通过反射解析实体类，读取 @FieldDesc 注解，递归解析嵌套对象和 List 泛型，生成字段树
 */
@Component
public class EntityFieldAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(EntityFieldAnalyzer.class);

    /**
     * 基本类型和白名单类，不需要递归解析
     */
    private static final Set<Class<?>> SIMPLE_TYPES = Set.of(
            String.class, Integer.class, Long.class, Double.class, Float.class,
            Boolean.class, Byte.class, Short.class, Character.class,
            BigDecimal.class, BigInteger.class, Date.class, LocalDateTime.class,
            LocalDate.class, LocalTime.class, java.sql.Timestamp.class
    );

    /**
     * BaseEntity 及其父类中的系统字段名，不纳入字段树
     */
    private static final Set<String> SYSTEM_FIELD_NAMES = Set.of(
            "id", "sort", "createTime", "createBy", "updateTime", "updateBy",
            "remark", "version", "createByName", "updateByName",
            "userCode", "tenantCode", "orderBy", "ids", "keyword",
            "timeFrom", "timeTo", "current", "size", "offset", "total", "count", "debug"
    );

    /**
     * 解析实体类生成字段树
     *
     * @param className 全限定类名
     * @return 字段树根节点列表
     */
    public List<EntityFieldNode> analyze(String className) {
        log.info("开始解析实体类字段树, className={}", className);

        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            log.error("实体类未找到, className={}", className, e);
            throw ValidationException.of("实体类未找到: " + className);
        }

        List<EntityFieldNode> nodes = buildFieldNodes(clazz, "$.data", false);
        log.info("实体类字段树解析完成, className={}, 字段数={}", className, nodes.size());
        return nodes;
    }



    // --- 私有辅助方法 ---

    /**
     * 递归构建字段节点列表
     *
     * @param clazz        当前类
     * @param parentPath   父级 JSONPath
     * @param isParentList 父级是否为列表
     */
    private List<EntityFieldNode> buildFieldNodes(Class<?> clazz, String parentPath, boolean isParentList) {
        List<EntityFieldNode> nodes = new ArrayList<>();
        List<Field> fields = getAllFields(clazz);

        for (Field field : fields) {
            String fieldName = field.getName();

            // 过滤系统字段
            if (SYSTEM_FIELD_NAMES.contains(fieldName)) {
                continue;
            }

            EntityFieldNode node = new EntityFieldNode();
            node.setFieldName(fieldName);

            // 读取 @FieldDesc 注解作为描述
            FieldDesc fieldDesc = field.getAnnotation(FieldDesc.class);
            node.setFieldDesc(fieldDesc != null ? fieldDesc.value() : fieldName);

            Class<?> fieldType = field.getType();
            node.setFieldType(fieldType.getSimpleName());

            // 生成 JSONPath
            String jsonPath = buildJsonPath(parentPath, fieldName, isParentList);
            node.setJsonPath(jsonPath);

            // 判断字段类型
            if (isSimpleType(fieldType)) {
                // 基本类型 → 叶子节点
                node.setIsList(false);
            } else if (List.class.isAssignableFrom(fieldType) || Set.class.isAssignableFrom(fieldType)) {
                // List/Set → 标记 isList=true，通过泛型获取元素类型，递归解析
                node.setIsList(true);
                Class<?> genericType = getListGenericType(field);
                if (genericType != null && !isSimpleType(genericType)) {
                    // 列表元素为复杂对象，递归解析子字段，路径使用 [*]
                    String childPath = jsonPath + "[*]";
                    node.setChildren(buildFieldNodes(genericType, childPath, true));
                }
            } else {
                // 其他对象类型 → 递归解析子字段
                node.setIsList(false);
                node.setChildren(buildFieldNodes(fieldType, jsonPath, false));
            }

            nodes.add(node);
        }

        return nodes;
    }

    /**
     * 递归获取类及其父类的所有字段
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            Field[] declaredFields = current.getDeclaredFields();
            fields.addAll(Arrays.asList(declaredFields));
            current = current.getSuperclass();
        }
        return fields;
    }

    /**
     * 判断是否为简单类型（不需要递归解析）
     */
    private boolean isSimpleType(Class<?> type) {
        // 基本类型
        if (type.isPrimitive()) {
            return true;
        }
        // 白名单包装类型
        if (SIMPLE_TYPES.contains(type)) {
            return true;
        }
        // Object 类型视为简单类型
        if (type == Object.class) {
            return true;
        }
        // 枚举类型视为简单类型
        if (Enum.class.isAssignableFrom(type)) {
            return true;
        }
        return false;
    }

    /**
     * 解析 RestInfo.returnGenericInfo JSON，递归提取最内层业务实体类
     * 解包 R<T>、PageData<T>、List<T>、Set<T> 等包装类型
     *
     * @param returnGenericInfo JSON 格式的泛型信息
     * @return {className, simpleName} 或 null
     */
    public Map<String, String> parseReturnGenericInfo(String returnGenericInfo) {
        if (returnGenericInfo == null || returnGenericInfo.isEmpty()) {
            return null;
        }

        log.info("解析返回值泛型信息: {}", returnGenericInfo);

        try {
            JSONObject genericInfo = JSONObject.parseObject(returnGenericInfo);
            String entityClassName = extractEntityClassName(genericInfo);

            if (entityClassName != null) {
                // 验证类是否可加载
                Class<?> clazz = Class.forName(entityClassName);
                Map<String, String> result = new LinkedHashMap<>();
                result.put("className", entityClassName);
                result.put("simpleName", clazz.getSimpleName());
                log.info("解析到实体类: {}", entityClassName);
                return result;
            }
        } catch (ClassNotFoundException e) {
            log.warn("实体类未找到: {}", returnGenericInfo);
        } catch (Exception e) {
            log.error("解析泛型信息异常: {}", returnGenericInfo, e);
        }

        return null;
    }

    /**
     * 从 GenericTypeInfo JSON 中递归提取最内层业务实体类名
     * 包装类型: com.wkclz.core.base.R, com.wkclz.core.base.PageData,
     * java.util.List, java.util.Set, java.util.ArrayList 等
     */
    private String extractEntityClassName(JSONObject genericInfo) {
        if (genericInfo == null) return null;

        String rawType = genericInfo.getString("rawType");
        com.alibaba.fastjson2.JSONArray typeArgs = genericInfo.getJSONArray("typeArgs");

        // 如果没有类型参数，当前就是叶子类型
        if (typeArgs == null || typeArgs.isEmpty()) {
            // 判断是否为业务实体类（在 iam-common 包下）
            if (isBusinessEntityClass(rawType)) {
                return rawType;
            }
            return null;
        }

        // 判断当前是否为包装类型，需要解包
        if (isWrapperType(rawType)) {
            // 递归解析所有类型参数，找到第一个业务实体类
            for (int i = 0; i < typeArgs.size(); i++) {
                JSONObject arg = typeArgs.getJSONObject(i);
                String result = extractEntityClassName(arg);
                if (result != null) return result;
            }
        }

        // 非包装类型，当前 rawType 可能就是业务实体类
        if (isBusinessEntityClass(rawType)) {
            return rawType;
        }

        // 尝试递归解析类型参数
        for (int i = 0; i < typeArgs.size(); i++) {
            JSONObject arg = typeArgs.getJSONObject(i);
            String result = extractEntityClassName(arg);
            if (result != null) return result;
        }

        return null;
    }

    /**
     * 判断是否为包装类型（需要解包继续查找）
     */
    private boolean isWrapperType(String rawType) {
        return "com.wkclz.core.base.R".equals(rawType)
                || "com.wkclz.core.base.PageData".equals(rawType)
                || "java.util.List".equals(rawType)
                || "java.util.Set".equals(rawType)
                || "java.util.ArrayList".equals(rawType)
                || "java.util.HashSet".equals(rawType)
                || "java.util.LinkedHashMap".equals(rawType)
                || "java.util.HashMap".equals(rawType)
                || "java.lang.Object".equals(rawType);
    }

    /**
     * 判断是否为业务实体类（在 iam-common 包下）
     */
    private boolean isBusinessEntityClass(String className) {
        return className != null
                && (className.startsWith("com.wkclz.iam.common.entity")
                || className.startsWith("com.wkclz.iam.common.dto"));
    }

    /**
     * 分析 R 类的字段结构，用于 R 未指定泛型时展示给前端
     * R 类字段: code, msg, data, requestTime, responseTime, costTime
     *
     * @return R 类字段树
     */
    public List<EntityFieldNode> analyzeRStructure() {
        log.info("分析R类结构");
        return buildFieldNodes(com.wkclz.core.base.R.class, "$", false);
    }

    /**
     * 获取 List/Set 的泛型元素类型
     */
    private Class<?> getListGenericType(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType parameterizedType) {
            Type[] typeArgs = parameterizedType.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> elementClass) {
                return elementClass;
            }
        }
        return null;
    }

    /**
     * 生成 JSONPath
     *
     * @param parentPath   父级路径
     * @param fieldName    字段名
     * @param isParentList 父级是否为列表
     */
    private String buildJsonPath(String parentPath, String fieldName, boolean isParentList) {
        if (isParentList) {
            return parentPath + "[*]." + fieldName;
        }
        return parentPath + "." + fieldName;
    }


}
