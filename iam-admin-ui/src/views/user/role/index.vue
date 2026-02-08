<template>
  <div class="app-container">

    <layout-split>
      <template #left>
        <app-options @select="selectApp"/>
      </template>
      <template #right>
        <el-form :model="queryParams" ref="queryRef" :inline="true" @submit.native.prevent label-width="1px;">
          <el-form-item prop="appCode">
            <el-input v-model="queryParams.appCode" disabled style="width: 160px" @keyup.enter.native="handleQuery" />
          </el-form-item>
          <el-form-item prop="keyword">
            <el-input v-model="keyword" placeholder="角色名称" clearable style="width: 200px" @input="handleFilter()" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">刷新</el-button>
          </el-form-item>
          <el-form-item style="float: right;">
            <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          </el-form-item>
        </el-form>

        <div class="role-breadcrumb">
          <!-- 面包屑导航 -->
          <el-breadcrumb separator-class="el-icon-arrow-right">
            <el-breadcrumb-item @click="navigateToLevel(0)" class="breadcrumb-item">根目录</el-breadcrumb-item>
            <span v-if="keyword">
              <el-breadcrumb-item class="breadcrumb-item">搜索: <el-tag effect="light" round size="small">{{ keyword }}</el-tag></el-breadcrumb-item>
            </span>
            <span v-else>
              <el-breadcrumb-item v-for="(item, index) in breadcrumb" :key="index" @click="navigateToLevel(index + 1)" class="breadcrumb-item">
                <span>{{ item.roleName }}</span>
              </el-breadcrumb-item>
            </span>
          </el-breadcrumb>
        </div>

        <el-table v-loading="loading" :data="showList">

          <el-table-column label="角色名称" prop="roleName" min-width="120">
            <template #default="{row}">
              <!-- 非叶子 -->
              <div v-if="row.childrenCount > 0" class="role-name" @click="navigateToChild(row)">
                <span>{{ row.roleName }}</span>
                <el-icon class="role-icon">
                  <span>{{row.childrenCount}}</span>
                  <ArrowRight />
                </el-icon>
              </div>
              <!-- 叶子 -->
              <div v-else>
                <span>{{ row.roleName }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="排序" prop="sort" width="60" v-if="columns.sort.visible"/>
          <el-table-column label="备注" prop="remark" min-width="120" v-if="columns.remark.visible"/>
          <el-table-column label="创建时间" prop="createTime" width="160" v-if="columns.createTime.visible">
            <template #default="{row}">{{ parseTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="创建人" prop="createBy" width="120" v-if="columns.createBy.visible"/>
          <el-table-column label="更新时间" prop="updateTime" width="160" v-if="columns.updateTime.visible">
            <template #default="{row}">{{ parseTime(row.updateTime) }}</template>
          </el-table-column>
          <el-table-column label="更新人" prop="updateBy" width="120" v-if="columns.updateBy.visible"/>
          <el-table-column fixed="right" width="240">
            <template #header><table-setting v-model:columns="columns"/></template>
            <template #default="{row}">
              <el-button link type="primary" icon="Plus" @click="handleAdd(row)">新增</el-button>
              <el-button link type="primary" icon="Edit" @click="handleUpdate(row)">编辑</el-button>
              <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination
            v-show="total > 0"
            :total="total"
            v-model:page="queryParams.current"
            v-model:limit="queryParams.size"
            @pagination="getList"
        />
      </template>
    </layout-split>
    <edit ref="editRef" @change="getList"/>
  </div>
</template>

<script setup name="IamUserRole">
import { roleList, roleRemove} from "@/api/user/role";
import Edit from "./components/edit"
import AppOptions from "@/views/components/AppOptions"

const { proxy } = getCurrentInstance();
const { BOOLEAN } = proxy.useDict("BOOLEAN");
const dataList = ref([]);
const showList = ref([]);
const loading = ref(false);
const total = ref(0);

const keyword = ref('');
// 菜单导航相关变量
const currentPath = ref([]);
const breadcrumb = ref([]);

const columns = ref({
  sort: {label: "排序", visible: true},
  remark: {label: "备注", visible: true},
  updateBy: {label: "修改人", visible: false},
  updateTime: {label: "修改时间", visible: false},
  createBy: {label: "创建人", visible: false},
  createTime: {label: "创建时间", visible: false},
})

const queryParams = ref({
  current: 1,
  size: 20,
  tenantCode: undefined,
  appCode: undefined,
  userCode: undefined,
  roleCode: undefined,
});

function selectApp(row) {
  queryParams.value.appCode = row.appCode;
  keyword.value = '';
  resetNavigation();
  handleQuery();
}

function resetNavigation() {
  currentPath.value = [];
  breadcrumb.value = [];
}

function handleQuery() {
  getList();
}

/** 查询参数列表 */
function getList() {
  loading.value = true;
  roleList(queryParams.value).then(res => {
    dataList.value = res.data;
    keyword.value = '';
    handleFilter();
  }).finally(() => {
    loading.value = false;
  });
}


function handleFilter() {
  showList.value = [];
  const result = [];
  if (keyword.value) {
    resetNavigation();
    // 包含筛选，按筛选的内容展示
    for (const role of dataList.value) {
      if (role.roleName.indexOf(keyword.value) > -1) {
        result.push(role);
      }
    }
  } else {
    // 不筛选，根据当前层级展示
    const currentLevel = currentPath.value.length;
    const parentCode = currentLevel > 0 ? currentPath.value[currentLevel - 1].roleCode : '0';
    for (const role of dataList.value) {
      if (role.parentCode === parentCode) {
        result.push(role);
      }
    }
  }
  showList.value = result;
}


// 导航到子菜单
function navigateToChild(role) {
  // 只有当菜单有子菜单时才允许点击导航
  if (role.childrenCount > 0) {
    currentPath.value.push(role);
    updateBreadcrumb();
    handleFilter();
  }
  // 没有子菜单的菜单项点击时不执行任何操作
}

// 导航到指定层级
function navigateToLevel(level) {
  if (level === 0) {
    // 导航到顶级
    resetNavigation();
  } else if (level <= currentPath.value.length) {
    // 导航到指定层级
    currentPath.value = currentPath.value.slice(0, level);
    updateBreadcrumb();
  }
  handleFilter();
}

// 更新面包屑
function updateBreadcrumb() {
  keyword.value = '';
  breadcrumb.value = [...currentPath.value];
}


function handleAdd(row) {
  if (!queryParams.value.appCode) {
    return;
  }
  const param = {appCode: queryParams.value.appCode, parentCode: row?.roleCode || '0'};
  // 项级路径
  let parents = [{roleName: '顶级'}];
  // 当前路径
  const current = currentPath.value;
  if (current.length > 0) {
    const lastMenu = current[current.length - 1];
    param.parentCode = lastMenu.roleCode;
    parents = [...parents, ...current];
  }
  // 子路径
  if (row?.roleCode) {
    param.parentCode = row.roleCode;
    parents = [...parents, {roleName: row.roleName}];
  }
  param.parents = parents;
  proxy.$refs["editRef"].init(param);
}


function handleUpdate(row) {
  // 为编辑的菜单添加完整的父菜单路径
  let parents = [{roleName: '顶级'}];
  if (currentPath.value.length > 0) {
    parents = [...parents, ...currentPath.value];
  }
  row.parents = parents;
  proxy.$refs["editRef"].init(row);
}

/** 删除按钮操作 */
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除 :"' + row.roleCode + '"？').then(() => {
    roleRemove({id: row.id}).then(res => {
      proxy.$modal.msgSuccess("删除成功");
      getList();
    })
  }).catch(() => {});
}

</script>

<style scoped lang="scss">
.role-breadcrumb {
  font-size: 14px;
  margin: 8px 0;;
}

.role-name {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #409eff;
  :hover {
    color: #66b1ff;
    text-decoration: underline;
  }
}

.role-icon {
  margin-left: 4px;
}

.breadcrumb-item {
  cursor: pointer;
  :hover {
    text-decoration: underline;
  }
}

:deep(.el-tag--small) {
  height: 12px;
}

</style>

