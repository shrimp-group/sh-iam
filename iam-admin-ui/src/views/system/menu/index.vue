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
            <el-input v-model="keyword" placeholder="菜单名称, 快速匹配" clearable style="width: 200px" @input="handleFilter()" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">刷新</el-button>
          </el-form-item>
          <el-form-item style="float: right;">
            <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          </el-form-item>
        </el-form>

        <div class="menu-breadcrumb">
          <!-- 面包屑导航 -->
          <el-breadcrumb separator-class="el-icon-arrow-right">
            <el-breadcrumb-item @click="navigateToLevel(0)" class="breadcrumb-item">根目录</el-breadcrumb-item>
            <span v-if="keyword">
              <el-breadcrumb-item class="breadcrumb-item">搜索: <el-tag effect="light" round size="small">{{ keyword }}</el-tag></el-breadcrumb-item>
            </span>
            <span v-else>
              <el-breadcrumb-item v-for="(item, index) in breadcrumb" :key="index" @click="navigateToLevel(index + 1)" class="breadcrumb-item">
                <span v-if="item.icon" style="margin-right: 8px;"><svg-icon :icon-class="item.icon"/></span>
                <span>{{ item.menuName }}</span>
              </el-breadcrumb-item>
            </span>
          </el-breadcrumb>
        </div>

        <el-table v-loading="loading" :data="showList">
          <el-table-column label="菜单名称" prop="menuName" min-width="120">
            <template #default="{row}">
              <!-- 非叶子 -->
              <div v-if="row.childrenCount > 0" class="menu-name" @click="navigateToChild(row)">
                <svg-icon v-if="row.icon" :icon-class="row.icon" style="margin-right: 8px;" />
                <span>{{ row.menuName }}</span>
                <el-icon class="menu-icon">
                  <span>{{row.childrenCount}}</span>
                  <ArrowRight />
                </el-icon>
              </div>
              <!-- 叶子 -->
              <div v-else>
                <svg-icon v-if="row.icon" :icon-class="row.icon" style="margin-right: 8px;" />
                <span>{{ row.menuName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="菜单类型" prop="menuType" width="80">
            <template #default="{row}"><dict-tag :options="MENU_TYPE" :value="row.menuType" /></template>
          </el-table-column>
          <el-table-column label="路由地址" prop="routePath" min-width="120"/>
          <el-table-column label="组件" prop="component" min-width="120"/>
          <el-table-column label="按钮编码" prop="buttonCode" min-width="120"/>
          <el-table-column label="隐藏" prop="hidden" width="60">
            <template #default="{row}"><dict-tag :options="BOOLEAN" :value="row.hidden" /></template>
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
          <el-table-column fixed="right" width="160">
            <template #header><table-setting v-model:columns="columns"/></template>
            <template #default="{row}">
              <el-button link type="primary" icon="Edit" @click="handleUpdate(row)">编辑</el-button>
              <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </layout-split>
    <edit ref="editRef" @change="getList"/>
  </div>
</template>

<script setup name="IamMenu">
import { menuList, menuRemove} from "@/api/system/menu";
import Edit from "./components/edit"
import AppOptions from "@/views/components/AppOptions"

const { proxy } = getCurrentInstance();
const { BOOLEAN, MENU_TYPE } = proxy.useDict("BOOLEAN", "MENU_TYPE");
const dataList = ref([]);
const showList = ref([]);
const loading = ref(false);

const appName = ref('');
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
  appCode: undefined,
});
const keyword = ref('');


function selectApp(row) {
  queryParams.value.appCode = row.appCode;
  appName.value = row.appName;
  // 重置导航状态
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
  menuList(queryParams.value).then(res => {
    dataList.value = res.data;
    keyword.value = '';
    handleFilter();
  }).finally(() => {
    loading.value = false;
  });
}

// type=0筛选, type=1跳转
function handleFilter() {
  showList.value = [];
  const result = [];
  if (keyword.value) {
    resetNavigation();
    // 包含筛选，按筛选的内容展示
    for (const menu of dataList.value) {
      if (menu.menuName.indexOf(keyword.value) > -1) {
        result.push(menu);
      }
    }
  } else {
    // 不筛选，根据当前层级展示
    const currentLevel = currentPath.value.length;
    const parentCode = currentLevel > 0 ? currentPath.value[currentLevel - 1].menuCode : '0';
    for (const menu of dataList.value) {
      if (menu.parentCode === parentCode) {
        result.push(menu);
      }
    }
  }
  showList.value = result;
}

// 导航到子菜单
function navigateToChild(menu) {
  // 只有当菜单有子菜单时才允许点击导航
  if (menu.childrenCount > 0) {
    currentPath.value.push(menu);
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

function handleAdd() {
  if (!queryParams.value.appCode) {
    return;
  }
  const param = {appCode: queryParams.value.appCode, parentCode: '0'};
  let parents = [{menuName: '顶级'}];
  // 如果当前有路径，使用当前路径的最后一个菜单作为父菜单
  const current = currentPath.value;
  if (current.length > 0) {
    const lastMenu = current[current.length - 1];
    param.parentCode = lastMenu.menuCode;
    parents = [...parents, ...current];
  }

  param.parents = parents;
  proxy.$refs["editRef"].init(param);
}
function handleUpdate(row) {
  // 为编辑的菜单添加完整的父菜单路径
  let parents = [{menuName: '顶级'}];
  if (currentPath.value.length > 0) {
    parents = [...parents, ...currentPath.value];
  }
  row.parents = parents;
  proxy.$refs["editRef"].init(row);
}
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除 :"' + row.menuName + '"？').then(() => {
    menuRemove({id: row.id}).then(res => {
      proxy.$modal.msgSuccess("删除成功");
      // 重新加载数据并保持当前导航状态
      getList();
    })
  }).catch(() => {});
}

</script>

<style scoped lang="scss">
.menu-breadcrumb {
  font-size: 14px;
  margin: 8px 0;;
}

.menu-name {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #409eff;
  :hover {
    color: #66b1ff;
    text-decoration: underline;
  }
}

.menu-icon {
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

