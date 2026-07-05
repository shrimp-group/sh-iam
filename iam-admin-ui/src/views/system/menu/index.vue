<template>
  <div class="app-container">

    <layout-split>
      <template #left>
        <app-options @select="selectApp"/>
      </template>
      <template #right>
        <el-tabs v-model="activeTab">
          <!-- 列表视图 -->
          <el-tab-pane label="列表视图" name="list">
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
              <el-table-column label="接口数" prop="apiBindCount" width="80">
                <template #default="{row}">{{ row.apiBindCount || 0 }}</template>
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
              <el-table-column fixed="right" width="280">
                <template #header><table-setting v-model:columns="columns"/></template>
                <template #default="{row}">
                  <el-button link type="success" icon="View" @click="handleDetail(row)">详情</el-button>
                  <el-button link type="primary" icon="Plus" @click="handleAdd(row)">新增</el-button>
                  <el-button link type="primary" icon="Edit" @click="handleUpdate(row)">编辑</el-button>
                  <el-popconfirm v-if="row.childrenCount === 0" :title="'确认删除:' + row.menuName + '?'" placement="top-end" @confirm="handleDelete(row)">
                    <template #reference><el-button link type="danger" icon="Delete">删除</el-button></template>
                  </el-popconfirm>
                  <el-tooltip v-if="row.childrenCount > 0" effect="dark" content="请先删除子菜单!" placement="top-start">
                    <el-button link type="danger" icon="Delete" disabled>删除</el-button>
                  </el-tooltip>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 树形视图 -->
          <el-tab-pane label="树形视图" name="tree">
            <div class="tree-toolbar">
              <el-input v-model="queryParams.appCode" disabled style="width: 160px" />
              <el-input v-model="treeKeyword" placeholder="菜单名称搜索" clearable style="width: 200px" />
              <el-button size="small" @click="expandAll">一键展开</el-button>
              <el-button size="small" @click="collapseAll">一键折叠</el-button>
            </div>

            <el-table
              ref="menuTreeTableRef"
              v-loading="treeLoading"
              :data="filteredMenuTree"
              row-key="menuCode"
              :tree-props="{ children: 'children' }"
              :default-expand-all="false"
            >
              <el-table-column label="菜单名称" prop="menuName" min-width="200">
                <template #default="{row}">
                  <svg-icon v-if="row.icon" :icon-class="row.icon" style="margin-right: 8px;" />
                  <span>{{ row.menuName }}</span>
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
              <el-table-column fixed="right" width="280">
                <template #header><table-setting v-model:columns="columns"/></template>
                <template #default="{row}">
                  <el-button link type="success" icon="View" @click="handleDetail(row)">详情</el-button>
                  <el-button link type="primary" icon="Plus" @click="handleTreeAdd(row)">新增</el-button>
                  <el-button link type="primary" icon="Edit" @click="handleTreeUpdate(row)">编辑</el-button>
                  <el-popconfirm v-if="!row.children || row.children.length === 0" :title="'确认删除:' + row.menuName + '?'" placement="top-end" @confirm="handleTreeDelete(row)">
                    <template #reference><el-button link type="danger" icon="Delete">删除</el-button></template>
                  </el-popconfirm>
                  <el-tooltip v-if="row.children && row.children.length > 0" effect="dark" content="请先删除子菜单!" placement="top-start">
                    <el-button link type="danger" icon="Delete" disabled>删除</el-button>
                  </el-tooltip>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </template>
    </layout-split>
    <edit ref="editRef" @change="getList"/>
    <detail ref="detailRef" />
  </div>
</template>

<script setup name="IamMenu">
import {menuList, menuTree, menuRemove} from "@/api/system/menu";
import Edit from "./components/edit"
import Detail from "./components/detail"
import AppOptions from "@/views/components/AppOptions"

const { proxy } = getCurrentInstance();
const { BOOLEAN, MENU_TYPE } = proxy.useDict("BOOLEAN", "MENU_TYPE");
const dataList = ref([]);
const showList = ref([]);
const loading = ref(false);

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
  appCode: undefined,
});

// ========== 树形视图相关变量 ==========
const activeTab = ref('list')
const treeKeyword = ref('')
const menuTreeData = ref([])
const menuTreeTableRef = ref(null)
const menuTreeDataLoaded = ref(false)
const treeLoading = ref(false)

// 过滤后的树数据
const filteredMenuTree = computed(() => {
  if (!treeKeyword.value) return menuTreeData.value
  return filterTree(menuTreeData.value, treeKeyword.value.toLowerCase())
})

// 递归过滤树节点：匹配节点及其祖先节点保留
function filterTree(nodes, keyword) {
  if (!nodes) return []
  const result = []
  for (const node of nodes) {
    const children = filterTree(node.children, keyword)
    const nameMatch = node.menuName && node.menuName.toLowerCase().includes(keyword)
    if (nameMatch || children.length > 0) {
      result.push({ ...node, children: children.length > 0 ? children : node.children })
    }
  }
  return result
}

// 加载树形数据
function loadMenuTreeData() {
  if (!queryParams.value.appCode) return
  treeLoading.value = true
  menuTree({ appCode: queryParams.value.appCode }).then(res => {
    menuTreeData.value = res.data || []
    menuTreeDataLoaded.value = true
  }).finally(() => {
    treeLoading.value = false
  })
}

// 一键展开所有节点
function expandAll() {
  toggleExpansion(filteredMenuTree.value, true)
}

// 一键折叠所有节点
function collapseAll() {
  toggleExpansion(filteredMenuTree.value, false)
}

// 递归展开/折叠
function toggleExpansion(data, expanded) {
  data.forEach(item => {
    menuTreeTableRef.value.toggleRowExpansion(item, expanded)
    if (item.children && item.children.length > 0) {
      toggleExpansion(item.children, expanded)
    }
  })
}

// 切换 Tab 时懒加载树数据
watch(activeTab, (val) => {
  if (val === 'tree' && !menuTreeDataLoaded.value) {
    loadMenuTreeData()
  }
})

// 通过遍历树数据构建父级路径（用于编辑弹窗的 parents 参数）
function buildTreeParents(data) {
  const parents = [{ menuName: '顶级' }]
  const path = findPath(menuTreeData.value, data.menuCode)
  if (path) {
    parents.push(...path)
  }
  return parents
}

// 在树中查找节点路径
function findPath(nodes, targetCode, currentPath = []) {
  if (!nodes) return null
  for (const node of nodes) {
    const newPath = [...currentPath, { menuName: node.menuName, icon: node.icon }]
    if (node.menuCode === targetCode) {
      return newPath
    }
    if (node.children && node.children.length > 0) {
      const result = findPath(node.children, targetCode, newPath)
      if (result) return result
    }
  }
  return null
}

// 树形视图-新增子菜单
function handleTreeAdd(row) {
  if (!queryParams.value.appCode) return
  const param = {
    appCode: queryParams.value.appCode,
    parentCode: row.menuCode
  }
  param.parents = buildTreeParents(row)
  param.parents.push({ menuName: row.menuName, icon: row.icon })
  proxy.$refs["editRef"].init(param)
}

// 树形视图-编辑菜单
function handleTreeUpdate(row) {
  row.parents = buildTreeParents(row)
  proxy.$refs["editRef"].init(row)
}

// 树形视图-删除菜单
function handleTreeDelete(row) {
  menuRemove({ id: row.id }).then(() => {
    proxy.$modal.msgSuccess("删除成功")
    loadMenuTreeData()
  })
}


function selectApp(row) {
  queryParams.value.appCode = row.appCode;
  // 重置导航状态
  keyword.value = '';
  resetNavigation();
  handleQuery();
  // 重置树形视图数据
  menuTreeDataLoaded.value = false
  menuTreeData.value = []
  treeKeyword.value = ''
  if (activeTab.value === 'tree') {
    loadMenuTreeData()
  }
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
  // 如果树形视图已加载过数据，同步刷新
  if (menuTreeDataLoaded.value) {
    loadMenuTreeData()
  }
}

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

function handleAdd(row) {
  if (!queryParams.value.appCode) {
    return;
  }
  const param = {appCode: queryParams.value.appCode, parentCode: row?.menuCode || '0'};
  // 项级路径
  let parents = [{menuName: '顶级'}];
  // 当前路径
  const current = currentPath.value;
  if (current.length > 0) {
    const lastMenu = current[current.length - 1];
    param.parentCode = lastMenu.menuCode;
    parents = [...parents, ...current];
  }
  // 子路径
  if (row?.menuCode) {
    param.parentCode = row.menuCode;
    parents = [...parents, {menuName: row.menuName, icon: row.icon}];
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
  menuRemove({id: row.id}).then(res => {
    proxy.$modal.msgSuccess("删除成功");
    if (showList.value.length < 2 && currentPath.value.length > 0) {
      navigateToLevel(currentPath.value.length - 1);
    }
    getList();
    // 如果树形视图已加载过数据，同步刷新
    if (menuTreeDataLoaded.value) {
      loadMenuTreeData()
    }
  })
}

/** 详情按钮操作 */
function handleDetail(row) {
  proxy.$refs["detailRef"].init(row);
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

// 树形视图样式
.tree-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  gap: 8px;
}
</style>

