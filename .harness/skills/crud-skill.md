# CRUD 技能

## 技能定位

提供标准化的 CRUD 操作能力，适用于所有业务实体的增删改查操作。

## 操作流程

### 1. 分页查询 (Page)

**输入参数**
| 参数 | 类型 | 说明 |
|------|------|------|
| pageNum | Integer | 页码 |
| pageSize | Integer | 每页数量 |
| query条件 | Object | 查询条件 |

**处理流程**
1. 构建查询条件
2. 调用 BaseMapper.page()
3. 返回 PageData

**输出**
```json
{
  "list": [],
  "total": 0,
  "pageNum": 1,
  "pageSize": 10
}
```

### 2. 单个查询 (Get)

**输入参数**
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |

**处理流程**
1. 参数校验
2. 调用 BaseMapper.selectById()
3. 验证结果存在性
4. 返回实体

**输出**
```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 3. 新增 (Create)

**输入参数**
| 参数 | 类型 | 说明 |
|------|------|------|
| dto | Object | 数据对象 |

**处理流程**
1. 参数校验 (@Valid)
2. 唯一性校验
3. 生成业务编码
4. 调用 BaseMapper.insert()
5. 返回创建后的实体

### 4. 更新 (Update)

**输入参数**
| 参数 | 类型 | 说明 |
|------|------|------|
| dto | Object | 数据对象 |

**处理流程**
1. 参数校验
2. 查询原记录
3. 验证记录存在
4. 属性拷贝 (copyIfNotNull)
5. 调用 BaseMapper.updateById()

### 5. 删除 (Delete)

**输入参数**
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |

**处理流程**
1. 参数校验
2. 查询记录
3. 验证存在性
4. 检查关联数据
5. 调用 BaseMapper.deleteById()

### 6. 批量删除 (BatchDelete)

**输入参数**
| 参数 | 类型 | 说明 |
|------|------|------|
| ids | List<Long> | 主键ID列表 |

**处理流程**
1. 参数校验
2. 遍历检查关联数据
3. 调用 BaseMapper.deleteByIds()

## 通用规则

### 参数校验
- 使用 `@Valid` 注解
- 使用 `Assert.notNull()`
- 使用自定义异常

### 事务管理
- 使用 `@Transactional(rollbackFor = Exception.class)`
- 跨表操作需要事务

### 日志记录
- 记录关键操作日志
- 记录操作人信息
- 记录操作时间

### 返回格式
- 统一使用 `R` 封装
- 成功返回 `R.ok(data)`
- 失败返回 `R.fail(message)`

## 示例代码

```java
@Service
public class IamUserService extends BaseService<IamUser, IamUserMapper> {

    public R page(IamUserDto dto) {
        PageData<IamUser> page = baseMapper.page(dto);
        return R.ok(page);
    }

    public R get(Long id) {
        IamUser user = baseMapper.selectById(id);
        Assert.notNull(user, "用户不存在");
        return R.ok(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public R create(IamUserDto dto) {
        // 唯一性校验
        Assert.isNull(baseMapper.selectByUsername(dto.getUsername()), "用户名已存在");
        
        IamUser user = IamUser.copy(dto);
        user.setUserCode(redisIdGenerator.generateIdWithPrefix("user_"));
        baseMapper.insert(user);
        
        // 创建关联记录
        // ...
        
        return R.ok(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public R update(IamUserDto dto) {
        IamUser user = baseMapper.selectById(dto.getId());
        Assert.notNull(user, "用户不存在");
        
        IamUser.copyIfNotNull(dto, user);
        baseMapper.updateById(user);
        
        return R.ok(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public R delete(Long id) {
        IamUser user = baseMapper.selectById(id);
        Assert.notNull(user, "用户不存在");
        
        // 检查关联数据
        Assert.isNull(userRoleMapper.selectByUserCode(user.getUserCode()), "存在关联角色");
        
        baseMapper.deleteById(id);
        return R.ok();
    }
}
```