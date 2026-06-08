package com.wkclz.iam.admin.helper;

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
     * 需要扫描的实体类包路径
     */
    private static final String ENTITY_PACKAGE = "com.wkclz.iam.common.entity";
    private static final String DTO_PACKAGE = "com.wkclz.iam.common.dto";

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

    /**
     * 获取可用实体类列表
     * 扫描 iam-common 的 entity 和 dto 包
     */
    public List<Map<String, String>> getEntityClasses() {
        log.info("查询可用实体类列表");
        List<Map<String, String>> result = new ArrayList<>();

        // 扫描 entity 包
        scanPackageClasses(ENTITY_PACKAGE, result);
        // 扫描 dto 包
        scanPackageClasses(DTO_PACKAGE, result);

        log.info("可用实体类列表查询完成, 共{}个类", result.size());
        return result;
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

    /**
     * 扫描指定包下的类，添加到结果列表
     */
    private void scanPackageClasses(String packageName, List<Map<String, String>> result) {
        String packagePath = packageName.replace('.', '/');
        java.net.URL classpathUrl = getClass().getClassLoader().getResource(packagePath);
        if (classpathUrl == null) {
            log.warn("包路径未找到: {}", packageName);
            return;
        }

        // 仅处理 file 协议（本地目录），jar 包内扫描需要不同逻辑
        if (!"file".equals(classpathUrl.getProtocol())) {
            log.warn("仅支持本地目录扫描, 跳过: {}", classpathUrl);
            return;
        }

        java.io.File dir = new java.io.File(classpathUrl.getFile());
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("目录不存在: {}", dir.getAbsolutePath());
            return;
        }

        java.io.File[] classFiles = dir.listFiles((dir1, name) -> name.endsWith(".class"));
        if (classFiles == null) {
            return;
        }

        for (java.io.File classFile : classFiles) {
            String fileName = classFile.getName();
            String simpleName = fileName.replace(".class", "");
            String fullClassName = packageName + "." + simpleName;

            // 验证类是否可加载
            try {
                Class.forName(fullClassName);
                Map<String, String> item = new LinkedHashMap<>();
                item.put("className", fullClassName);
                item.put("simpleName", simpleName);
                result.add(item);
            } catch (ClassNotFoundException e) {
                log.warn("类加载失败, 跳过: {}", fullClassName);
            }
        }
    }

}
