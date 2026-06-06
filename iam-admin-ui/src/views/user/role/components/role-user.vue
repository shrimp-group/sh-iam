<template>
  <el-dialog :title="'角色用户管理 - ' + roleInfo?.roleName" v-model="open" width="900px" :close-on-click-modal="false">
    <!-- 角色基本信息 -->
    <el-descriptions :column="2" border size="small" style="margin-bottom: 16px;">
      <el-descriptions-item label="角色名称">{{ roleInfo?.roleName }}</el-descriptions-item>
      <el-descriptions-item label="应用编码">{{ roleInfo?.appCode }}</el-descriptions-item>
    </el-descriptions>

    <!-- 操作栏 -->
    <div style="margin-bottom: 12px;">
      <el-button type="primary" plain icon="Plus" @click="handleAddUser">添加用户</el-button>
      <el-button type="danger" plain icon="Delete" :disabled="selectedIds.length === 0" @click="handleBatchUnbind">批量移除</el-button>
    </div>

    <!-- 用户列表 -->
    <el-table v-loading="loading" :data="userList" max-height="500" min-height="200" @selection-change="handleSelectionChange">
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
        v-model:current-page="queryParams.current"
        v-model:page-size="queryParams.size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadRoleUsers"
        @current-change="loadRoleUsers"
      />
    </div>

    <!-- 添加用户弹窗（嵌套） -->
    <el-dialog title="添加用户" v-model="addUserOpen" width="800px" :close-on-click-modal="false" append-to-body>
      <div style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center;">
        <el-input v-model="addUserKeyword" placeholder="用户名/昵称搜索" clearable style="width: 200px" @input="handleAddUserSearch" />
        <div>
          <span style="margin-right: 8px; font-size: 14px; color: #606266;">有效期:</span>
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 360px;"
          />
        </div>
      </div>
      <el-table v-loading="addUserLoading" :data="addUserList" max-height="400" min-height="200" @selection-change="handleAddSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column label="用户名" prop="username" min-width="120" />
        <el-table-column label="昵称" prop="nickname" min-width="120" />
        <el-table-column label="邮箱" prop="email" min-width="150" />
        <el-table-column label="手机号" prop="phone" min-width="120" />
      </el-table>
      <!-- 添加用户分页 -->
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

<script setup name="RoleUser">
import { roleUserPage, roleUserBind, roleUserUnbind } from "@/api/user/role-user";
import { userPage } from "@/api/user/user";
import { parseTime } from "@/utils/ruoyi";

defineExpose({ init });
const emit = defineEmits(["change"]);
const { proxy } = getCurrentInstance();

const open = ref(false);
const roleInfo = ref(null);
const loading = ref(false);
const userList = ref([]);
const total = ref(0);
const selectedIds = ref([]);

const queryParams = ref({
  current: 1,
  size: 10,
  roleCode: undefined,
});

// 添加用户相关
const addUserOpen = ref(false);
const addUserLoading = ref(false);
const addUserList = ref([]);
const addUserTotal = ref(0);
const addSelectedUsers = ref([]);
const addUserKeyword = ref("");
const dateRange = ref(null);
const bindLoading = ref(false);

const addUserParams = ref({
  current: 1,
  size: 10,
  keyword: undefined,
});

function init(row) {
  open.value = true;
  roleInfo.value = row;
  queryParams.value.roleCode = row.roleCode;
  queryParams.value.current = 1;
  selectedIds.value = [];
  loadRoleUsers();
}

function loadRoleUsers() {
  loading.value = true;
  roleUserPage(queryParams.value).then(res => {
    userList.value = res.data?.records || [];
    total.value = res.data?.total || 0;
  }).finally(() => {
    loading.value = false;
  });
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
  dateRange.value = null;
  addUserParams.value.current = 1;
  addUserParams.value.keyword = undefined;
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
  const userCodes = addSelectedUsers.value.map(u => u.userCode);
  const data = {
    roleCode: roleInfo.value?.roleCode,
    userCodes: userCodes,
    startTime: dateRange.value ? dateRange.value[0] : undefined,
    endTime: dateRange.value ? dateRange.value[1] : undefined,
  };
  bindLoading.value = true;
  roleUserBind(data).then(() => {
    proxy.$modal.msgSuccess("添加用户成功");
    addUserOpen.value = false;
    emit("change");
    loadRoleUsers();
  }).finally(() => {
    bindLoading.value = false;
  });
}

function close() {
  open.value = false;
}
</script>

<style scoped>
</style>
