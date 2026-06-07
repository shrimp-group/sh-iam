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
                  <el-button link type="success" icon="View" @click="handleDetail(row)">详情</el-button>
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
          </el-tab-pane>

          <!-- 树形视图 -->
          <el-tab-pane label="树形视图" name="tree">
            <div class="tree-toolbar">
              <el-input v-model="queryParams.appCode" disabled style="width: 160px" />
              <el-input v-model="treeKeyword" placeholder="角色名称搜索" clearable style="width: 200px" />
              <el-button size="small" @click="expandAll">一键展开</el-button>
              <el-button size="small" @click="collapseAll">一键折叠</el-button>
            </div>

            <el-table
              ref="roleTreeTableRef"
              v-loading="treeLoading"
              :data="filteredRoleTree"
              row-key="roleCode"
              :tree-props="{ children: 'children' }"
              :default-expand-all="false"
            >
              <el-table-column label="角色名称" prop="roleName" min-width="200" />
              <el-table-column label="是否可申请" prop="applicable" width="100">
                <template #default="{row}"><dict-tag :options="BOOLEAN" :value="row.applicable" /></template>
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
                  <el-button link type="success" icon="View" @click="handleDetail(row)">详情</el-button>
                  <el-button link type="primary" icon="Plus" @click="handleTreeAdd(row)">新增</el-button>
                  <el-button link type="primary" icon="Edit" @click="handleTreeUpdate(row)">编辑</el-button>
                  <el-popconfirm v-if="!row.children || row.children.length === 0" :title="'确认删除:' + row.roleName + '?'" placement="top-end" @confirm="handleTreeDelete(row)">
                    <template #reference><el-button link type="danger" icon="Delete">删除</el-button></template>
                  </el-popconfirm>
                  <el-tooltip v-if="row.children && row.children.length > 0" effect="dark" content="请先删除子角色!" placement="top-start">
                    <el-button link type="danger" icon="Delete" disabled>删除</el-button>
                  </el-tooltip>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </template>
    </layout-split>
    <edit ref="editRef" @change="handleEditChange"/>
    <detail ref="detailRef" @change="getList" />
  </div>
</template>

<script setup name="IamRole">
import {roleList, roleRemove, roleTree} from "@/api/user/role";
import Edit from "./components/edit"
import Detail from "./components/detail"
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

// ========== 树形视图相关 ==========
const activeTab = ref('list')
const treeKeyword = ref('')
const roleTreeData = ref([])
const roleTreeTableRef = ref(null)
const treeLoading = ref(false)
const roleTreeDataLoaded = ref(false)

// 过滤后的树数据
const filteredRoleTree = computed(() => {
  if (!treeKeyword.value) return roleTreeData.value
  return filterTree(roleTreeData.value, treeKeyword.value.toLowerCase())
})

// 递归过滤树节点，匹配子节点时保留所有祖先节点
function filterTree(nodes, keyword) {
  if (!nodes) return []
  const result = []
  for (const node of nodes) {
    const children = filterTree(node.children, keyword)
    const nameMatch = node.roleName && node.roleName.toLowerCase().includes(keyword)
    if (nameMatch || children.length > 0) {
      const newNode = { ...node, children: children.length > 0 ? children : node.children }
      result.push(newNode)
    }
  }
  return result
}

// 一键展开所有节点
function expandAll() {
  toggleExpansion(filteredRoleTree.value, true)
}

// 一键折叠所有节点
function collapseAll() {
  toggleExpansion(filteredRoleTree.value, false)
}

// 递归展开/折叠
function toggleExpansion(data, expanded) {
  data.forEach(item => {
    roleTreeTableRef.value.toggleRowExpansion(item, expanded)
    if (item.children && item.children.length > 0) {
      toggleExpansion(item.children, expanded)
    }
  })
}

// 加载角色树数据
function loadRoleTreeData() {
  if (!queryParams.value.appCode) return
  treeLoading.value = true
  roleTree({ appCode: queryParams.value.appCode }).then(res => {
    roleTreeData.value = res.data || []
    roleTreeDataLoaded.value = true
  }).finally(() => {
    treeLoading.value = false
  })
}

// 切换 Tab 时懒加载树数据
watch(activeTab, (val) => {
  if (val === 'tree' && !roleTreeDataLoaded.value) {
    loadRoleTreeData()
  }
})

// 通过遍历树数据构建父级路径
function buildTreeParents(data) {
  const parents = [{ roleName: '顶级' }]
  const path = findPath(roleTreeData.value, data.roleCode)
  if (path) {
    parents.push(...path)
  }
  return parents
}

// 在树中查找节点路径
function findPath(nodes, targetCode, currentPath = []) {
  if (!nodes) return null
  for (const node of nodes) {
    const newPath = [...currentPath, { roleName: node.roleName }]
    if (node.roleCode === targetCode) {
      return newPath
    }
    if (node.children && node.children.length > 0) {
      const result = findPath(node.children, targetCode, newPath)
      if (result) return result
    }
  }
  return null
}

// 树形视图-新增子角色
function handleTreeAdd(row) {
  if (!queryParams.value.appCode) return
  const param = { appCode: queryParams.value.appCode, parentCode: row.roleCode }
  const parents = buildTreeParents(row)
  parents.push({ roleName: row.roleName })
  param.parents = parents
  proxy.$refs["editRef"].init(param)
}

// 树形视图-编辑角色
function handleTreeUpdate(row) {
  row.parents = buildTreeParents(row)
  proxy.$refs["editRef"].init(row)
}

// 树形视图-删除角色
function handleTreeDelete(row) {
  roleRemove({ id: row.id }).then(res => {
    proxy.$modal.msgSuccess("删除成功")
    loadRoleTreeData()
    getList()
  })
}

// ========== 列表视图相关 ==========

function selectApp(row) {
  queryParams.value.appCode = row.appCode;
  keyword.value = '';
  resetNavigation();
  handleQuery();
  // 切换应用时重置树数据
  roleTreeDataLoaded.value = false
  roleTreeData.value = []
  if (activeTab.value === 'tree') {
    loadRoleTreeData()
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
  roleList(queryParams.value).then(res => {
    dataList.value = res.data;
    keyword.value = '';
    handleFilter();
    // 列表刷新后，如果树数据已加载，也刷新树数据
    if (roleTreeDataLoaded.value) loadRoleTreeData()
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

function handleDetail(row) {
  proxy.$refs["detailRef"].init(row);
}

// 编辑弹窗变更回调，同步刷新列表和树数据
function handleEditChange() {
  getList()
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

.tree-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  gap: 8px;
}
</style>
