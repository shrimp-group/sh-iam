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
            <el-input v-model="keyword" placeholder="角色名称, 快速匹配" clearable style="width: 200px" @input="handleFilter()" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">刷新</el-button>
          </el-form-item>
          <el-form-item style="float: right;">
            <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          </el-form-item>
        </el-form>

        <div class="role-breadcrumb">
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
          <el-table-column label="是否可申请" prop="applicable" width="100">
            <template #default="{row}"><dict-tag :options="BOOLEAN" :value="row.applicable" /></template>
          </el-table-column>
          <el-table-column label="用户数" prop="userBindCount" width="80">
            <template #default="{row}">{{ row.userBindCount || 0 }}</template>
          </el-table-column>
          <el-table-column label="排序" prop="sort" width="60" v-if="columns.sort.visible"/>
          <el-table-column label="备注" prop="remark" min-width="120" v-if="columns.remark.visible"/>
          <el-table-column label="创建时间" prop="createTime" width="160" v-if="columns.createTime.visible">
            <template #default="{row}">{{ parseTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="创建人" prop="createBy" width="120" v-if="columns.createBy.visible"/>
          <el-table-column fixed="right" width="300">
            <template #header><table-setting v-model:columns="columns"/></template>
            <template #default="{row}">
              <el-button link type="success" icon="Menu" @click="handleMenuBind(row)" v-if="row.applicable === 1">菜单</el-button>
              <el-button link type="primary" icon="Plus" @click="handleAdd(row)">新增</el-button>
              <el-button link type="primary" icon="Edit" @click="handleUpdate(row)">编辑</el-button>
              <el-popconfirm v-if="row.childrenCount === 0" :title="'确认删除:' + row.roleName + '?'" placement="top-end" @confirm="handleDelete(row)">
                <template #reference><el-button link type="danger" icon="Delete">删除</el-button></template>
              </el-popconfirm>
              <el-tooltip v-if="row.childrenCount > 0" effect="dark" content="请先删除子角色!" placement="top-start">
                <el-button link type="danger" icon="Delete" disabled>删除</el-button>
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </layout-split>
    <edit ref="editRef" @change="getList"/>
    <menu-bind ref="menuBindRef" />
  </div>
</template>

<script setup name="IamRole">
import {roleList, roleRemove} from "@/api/user/role";
import Edit from "./components/edit"
import MenuBind from "./components/menu-bind"
import AppOptions from "@/views/components/AppOptions"

const { proxy } = getCurrentInstance();
const { BOOLEAN } = proxy.useDict("BOOLEAN");
const dataList = ref([]);
const showList = ref([]);
const loading = ref(false);

const keyword = ref('');
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
  appCode: undefined,
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
    for (const role of dataList.value) {
      if (role.roleName.indexOf(keyword.value) > -1) {
        result.push(role);
      }
    }
  } else {
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

function navigateToChild(role) {
  if (role.childrenCount > 0) {
    currentPath.value.push(role);
    updateBreadcrumb();
    handleFilter();
  }
}

function navigateToLevel(level) {
  if (level === 0) {
    resetNavigation();
  } else if (level <= currentPath.value.length) {
    currentPath.value = currentPath.value.slice(0, level);
    updateBreadcrumb();
  }
  handleFilter();
}

function updateBreadcrumb() {
  keyword.value = '';
  breadcrumb.value = [...currentPath.value];
}

function handleAdd(row) {
  if (!queryParams.value.appCode) {
    return;
  }
  const param = {appCode: queryParams.value.appCode, parentCode: row?.roleCode || '0'};
  let parents = [{roleName: '顶级'}];
  const current = currentPath.value;
  if (current.length > 0) {
    const lastRole = current[current.length - 1];
    param.parentCode = lastRole.roleCode;
    parents = [...parents, ...current];
  }
  if (row?.roleCode) {
    param.parentCode = row.roleCode;
    parents = [...parents, {roleName: row.roleName}];
  }
  param.parents = parents;
  proxy.$refs["editRef"].init(param);
}

function handleUpdate(row) {
  let parents = [{roleName: '顶级'}];
  if (currentPath.value.length > 0) {
    parents = [...parents, ...currentPath.value];
  }
  row.parents = parents;
  proxy.$refs["editRef"].init(row);
}

function handleDelete(row) {
  roleRemove({id: row.id}).then(res => {
    proxy.$modal.msgSuccess("删除成功");
    if (showList.value.length < 2 && currentPath.value.length > 0) {
      navigateToLevel(currentPath.value.length - 1);
    }
    getList();
  })
}

function handleMenuBind(row) {
  proxy.$refs["menuBindRef"].init(row);
}

</script>

<style scoped lang="scss">
.role-breadcrumb {
  font-size: 14px;
  margin: 8px 0;
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
</style>
