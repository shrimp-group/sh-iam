<template>
  <el-dialog :title="'角色详情 - ' + roleInfo?.roleName" v-model="open" width="900px" :close-on-click-modal="false">
    <el-tabs v-model="activeTab">
      <!-- Tab 1: 基本信息 -->
      <el-tab-pane label="基本信息" name="basic">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="角色名称">{{ roleInfo?.roleName }}</el-descriptions-item>
          <el-descriptions-item label="角色编码">{{ roleInfo?.roleCode }}</el-descriptions-item>
          <el-descriptions-item label="应用编码">{{ roleInfo?.appCode }}</el-descriptions-item>
          <el-descriptions-item label="是否可申请">
            <dict-tag :options="BOOLEAN" :value="roleInfo?.applicable" />
          </el-descriptions-item>
          <el-descriptions-item label="排序">{{ roleInfo?.sort }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ roleInfo?.remark }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ parseTime(roleInfo?.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ roleInfo?.createBy }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <!-- Tab 2: 菜单 -->
      <el-tab-pane label="菜单" name="menu">
        <el-tree
          ref="treeRef"
          v-loading="menuLoading"
          :data="menuTreeData"
          :props="{ label: 'menuName', children: 'children' }"
          node-key="menuCode"
          show-checkbox
          default-expand-all
          :default-checked-keys="defaultCheckedKeys"
        >
          <template #default="{ node, data }">
            <span class="custom-tree-node">
              <span>{{ data.menuName }}</span>
              <el-tag v-if="data.menuType" size="small" style="margin-left: 8px;">{{ data.menuType === 'MENU' ? '菜单' : '按钮' }}</el-tag>
            </span>
          </template>
        </el-tree>
        <div style="margin-top: 12px; display: flex; justify-content: space-between; align-items: center;">
          <div>
            <el-button size="small" @click="handleCheckAll">全选</el-button>
            <el-button size="small" @click="handleUncheckAll">全取消</el-button>
          </div>
          <el-button type="primary" :loading="menuSaveLoading" @click="handleMenuSave">保 存</el-button>
        </div>
      </el-tab-pane>

      <!-- Tab 3: 用户 -->
      <el-tab-pane label="用户" name="user">
        <!-- 操作栏 -->
        <div style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center;">
          <div style="display: flex; align-items: center; gap: 8px;">
            <el-input v-model="userSearchParams.username" placeholder="用户名(精确)" clearable style="width: 140px" @keyup.enter="handleUserSearch" />
            <el-input v-model="userSearchParams.nickname" placeholder="姓名(模糊)" clearable style="width: 140px" @keyup.enter="handleUserSearch" />
            <el-button type="primary" icon="Search" @click="handleUserSearch">搜索</el-button>
          </div>
          <div>
            <el-button type="primary" plain icon="Plus" @click="handleAddUser">添加用户</el-button>
            <el-button type="danger" plain icon="Delete" :disabled="selectedIds.length === 0" @click="handleBatchUnbind">批量移除</el-button>
          </div>
        </div>
        <!-- 用户列表 -->
        <el-table v-loading="userLoading" :data="userList" max-height="500" min-height="200" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="50" />
          <el-table-column label="用户名" prop="username" min-width="120" />
          <el-table-column label="昵称" prop="nickname" min-width="120" />
          <el-table-column label="有效开始时间" prop="startTime" width="160">
            <template #default="{row}">{{ parseTime(row.startTime) }}</template>
          </el-table-column>
          <el-table-column label="有效结束时间" prop="endTime" width="160">
            <template #default="{row}">{{ parseTime(row.endTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" prop="enableStatus" width="80">
            <template #default="{row}">
              <el-tag :type="row.enableStatus === 1 ? 'success' : 'danger'">{{ row.enableStatus === 1 ? '有效' : '无效' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{row}">
              <el-popconfirm :title="'确认移除用户: ' + row.nickname + '?'" placement="top-end" @confirm="handleSingleUnbind(row)">
                <template #reference><el-button link type="danger" icon="Delete">移除</el-button></template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <!-- 分页 -->
        <div style="margin-top: 12px; display: flex; justify-content: flex-end;">
          <el-pagination
            v-model:current-page="userParams.current"
            v-model:page-size="userParams.size"
            :page-sizes="[10, 20, 50]"
            :total="userTotal"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadRoleUsers"
            @current-change="loadRoleUsers"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 添加用户弹窗（嵌套） -->
    <el-dialog title="添加用户" v-model="addUserOpen" width="800px" :close-on-click-modal="false" append-to-body>
      <div style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center;">
        <el-input v-model="addUserKeyword" placeholder="用户名/昵称搜索" clearable style="width: 200px" @input="handleAddUserSearch" />
        <div style="display: flex; align-items: center; gap: 8px;">
          <span style="font-size: 14px; color: #606266;">有效期:</span>
          <el-date-picker v-model="addUserStartTime" type="datetime" placeholder="开始时间" style="width: 200px;" />
          <span style="color: #606266;">至</span>
          <el-date-picker v-model="addUserEndTime" type="datetime" placeholder="结束时间" style="width: 200px;" />
        </div>
      </div>
      <el-table v-loading="addUserLoading" :data="addUserList" max-height="400" min-height="200" @selection-change="handleAddSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column label="用户名" prop="username" min-width="120" />
        <el-table-column label="昵称" prop="nickname" min-width="120" />
        <el-table-column label="邮箱" prop="email" min-width="150" />
        <el-table-column label="手机号" prop="phone" min-width="120" />
      </el-table>
      <div style="margin-top: 12px; display: flex; justify-content: flex-end;">
        <el-pagination
          v-model:current-page="addUserParams.current"
          v-model:page-size="addUserParams.size"
          :page-sizes="[10, 20, 50]"
          :total="addUserTotal"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadAllUsers"
          @current-change="loadAllUsers"
        />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="addUserOpen = false">取 消</el-button>
          <el-button type="primary" :disabled="addSelectedUsers.length === 0" :loading="bindLoading" @click="handleBindConfirm">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="close">关 闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="IamRoleDetail">
import { menuTree } from "@/api/system/menu";
import { roleMenuList, roleMenuSave } from "@/api/system/role-menu";
import { roleUserPage, roleUserBind, roleUserUnbind } from "@/api/user/role-user";
import { userPage } from "@/api/user/user";
import { parseTime } from "@/utils/ruoyi";

defineExpose({ init });
const emit = defineEmits(["change"]);
const { proxy } = getCurrentInstance();
const { BOOLEAN } = proxy.useDict("BOOLEAN");

const open = ref(false);
const activeTab = ref("basic");
const roleInfo = ref(null);

// ========== 菜单 Tab ==========
const treeRef = ref(null);
const menuLoading = ref(false);
const menuSaveLoading = ref(false);
const menuTreeData = ref([]);
const defaultCheckedKeys = ref([]);

// 获取树中所有叶子节点的 menuCode（仅在已绑定集合中的）
function getLeafKeys(tree, boundCodeSet) {
  const leafKeys = [];
  function traverse(nodes) {
    if (!nodes) return;
    for (const node of nodes) {
      if (node.children && node.children.length > 0) {
        traverse(node.children);
      } else {
        // 叶子节点：如果在已绑定列表中，加入默认勾选
        if (boundCodeSet.has(node.menuCode)) {
          leafKeys.push(node.menuCode);
        }
      }
    }
  }
  traverse(tree);
  return leafKeys;
}

function loadMenuData() {
  if (!roleInfo.value) return;
  menuLoading.value = true;
  Promise.all([
    menuTree({ appCode: roleInfo.value.appCode }),
    roleMenuList({ roleCode: roleInfo.value.roleCode })
  ]).then(([treeRes, boundRes]) => {
    menuTreeData.value = treeRes.data || [];
    // roleMenuList 直接返回 menuCode 字符串数组
    const boundMenuCodes = boundRes.data || [];
    const boundCodeSet = new Set(boundMenuCodes);
    // el-tree 的 default-checked-keys 仅设置叶子节点，父节点会自动勾选
    defaultCheckedKeys.value = getLeafKeys(menuTreeData.value, boundCodeSet);
  }).finally(() => {
    menuLoading.value = false;
  });
}

// 全选所有菜单节点
function handleCheckAll() {
  const tree = treeRef.value
  if (!tree) return
  // 获取树中所有节点的 key
  const allKeys = []
  function getAllKeys(nodes) {
    if (!nodes) return
    for (const node of nodes) {
      allKeys.push(node.menuCode)
      if (node.children && node.children.length > 0) {
        getAllKeys(node.children)
      }
    }
  }
  getAllKeys(menuTreeData.value)
  tree.setCheckedKeys(allKeys)
}

// 全取消所有菜单节点
function handleUncheckAll() {
  const tree = treeRef.value
  if (!tree) return
  tree.setCheckedKeys([])
}

function handleMenuSave() {
  const tree = treeRef.value;
  if (!tree) return;
  // 获取所有勾选的节点 key（全选的）+ 半选的父节点
  const checkedKeys = tree.getCheckedKeys();
  const halfCheckedKeys = tree.getHalfCheckedKeys();
  const allKeys = [...checkedKeys, ...halfCheckedKeys];
  menuSaveLoading.value = true;
  roleMenuSave({
    roleCode: roleInfo.value?.roleCode,
    menuCodes: allKeys
  }).then(() => {
    proxy.$modal.msgSuccess("保存成功");
    emit("change");
  }).finally(() => {
    menuSaveLoading.value = false;
  });
}

// ========== 用户 Tab ==========
const userLoading = ref(false);
const userList = ref([]);
const userTotal = ref(0);
const selectedIds = ref([]);
const userParams = ref({ current: 1, size: 10, roleCode: undefined, username: undefined, nickname: undefined });
// 用户搜索条件
const userSearchParams = ref({ username: undefined, nickname: undefined });

// 添加用户相关
const addUserOpen = ref(false);
const addUserLoading = ref(false);
const addUserList = ref([]);
const addUserTotal = ref(0);
const addSelectedUsers = ref([]);
const addUserKeyword = ref("");
const addUserStartTime = ref(null)
const addUserEndTime = ref(null)
const bindLoading = ref(false);
const addUserParams = ref({ current: 1, size: 10, keyword: undefined });

// 标记各 Tab 是否已加载过
const menuLoaded = ref(false);
const userLoaded = ref(false);

function loadRoleUsers() {
  userLoading.value = true;
  userParams.value.username = userSearchParams.value.username || undefined;
  userParams.value.nickname = userSearchParams.value.nickname || undefined;
  roleUserPage(userParams.value).then(res => {
    userList.value = res.data?.records || [];
    userTotal.value = res.data?.total || 0;
  }).finally(() => {
    userLoading.value = false;
  });
}

// 用户搜索
function handleUserSearch() {
  userParams.value.current = 1;
  loadRoleUsers();
}

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(item => item.id);
}

function handleBatchUnbind() {
  roleUserUnbind({ ids: selectedIds.value }).then(() => {
    proxy.$modal.msgSuccess("批量移除成功");
    emit("change");
    loadRoleUsers();
  });
}

function handleSingleUnbind(row) {
  roleUserUnbind({ ids: [row.id] }).then(() => {
    proxy.$modal.msgSuccess("移除成功");
    emit("change");
    loadRoleUsers();
  });
}

function handleAddUser() {
  addUserOpen.value = true;
  addUserKeyword.value = "";
  addSelectedUsers.value = [];
  addUserParams.value.current = 1;
  addUserParams.value.keyword = undefined;
  // 设置有效期默认值：当前时间 ~ 一年后当天 23:59:59
  const now = new Date()
  addUserStartTime.value = now
  const oneYearLater = new Date(now)
  oneYearLater.setFullYear(oneYearLater.getFullYear() + 1)
  oneYearLater.setHours(23, 59, 59, 0)
  addUserEndTime.value = oneYearLater
  loadAllUsers();
}

function loadAllUsers() {
  addUserLoading.value = true;
  userPage(addUserParams.value).then(res => {
    addUserList.value = res.data?.records || [];
    addUserTotal.value = res.data?.total || 0;
  }).finally(() => {
    addUserLoading.value = false;
  });
}

function handleAddUserSearch() {
  addUserParams.value.keyword = addUserKeyword.value || undefined;
  addUserParams.value.current = 1;
  loadAllUsers();
}

function handleAddSelectionChange(selection) {
  addSelectedUsers.value = selection;
}

function handleBindConfirm() {
  if (!addUserStartTime.value || !addUserEndTime.value) {
    proxy.$modal.msgWarning("请选择有效时间");
    return;
  }
  const userCodes = addSelectedUsers.value.map(u => u.userCode);
  const data = {
    roleCode: roleInfo.value?.roleCode,
    userCodes: userCodes,
    startTime: addUserStartTime.value,
    endTime: addUserEndTime.value,
  };
  bindLoading.value = true;
  roleUserBind(data).then(() => {
    proxy.$modal.msgSuccess("添加用户成功");
    addUserOpen.value = false;
    emit("change");
    // 修复：添加用户后刷新用户列表
    loadRoleUsers();
  }).finally(() => {
    bindLoading.value = false;
  });
}

// 切换 Tab 时按需加载数据
watch(activeTab, (val) => {
  if (val === "menu" && !menuLoaded.value) {
    menuLoaded.value = true;
    loadMenuData();
  }
  if (val === "user" && !userLoaded.value) {
    userLoaded.value = true;
    loadRoleUsers();
  }
});

// ========== 公共方法 ==========
function init(row) {
  open.value = true;
  activeTab.value = "basic";
  roleInfo.value = row;

  // 重置菜单数据
  menuTreeData.value = [];
  defaultCheckedKeys.value = [];
  menuLoaded.value = false;

  // 重置用户数据
  userParams.value.roleCode = row.roleCode;
  userParams.value.current = 1;
  userSearchParams.value = { username: undefined, nickname: undefined };
  selectedIds.value = [];
  userList.value = [];
  userTotal.value = 0;
  userLoaded.value = false;

  // 基本信息 Tab 无需额外加载，菜单和用户 Tab 按需加载
}

function close() {
  open.value = false;
}
</script>

<style scoped>
.custom-tree-node {
  display: flex;
  align-items: center;
  font-size: 14px;
}
</style>
