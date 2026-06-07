<template>
  <div class="user-role-container">
    <layout-split>
      <template #left>
        <app-options @select="selectApp" height="660px"/>
      </template>
      <template #right>
        <!-- 操作栏 -->
        <div class="top-bar">
          <el-button type="primary" plain icon="Plus" @click="handleBind" :disabled="!currentAppCode">绑定角色</el-button>
        </div>

        <!-- 角色树 -->
        <el-tree
          v-loading="treeLoading"
          :data="roleTreeData"
          :props="{ label: 'roleName', children: 'children' }"
          node-key="roleCode"
          default-expand-all
          :expand-on-click-node="false"
        >
          <template #default="{ node, data }">
            <span class="custom-tree-node">
              <span>{{ data.roleName }}</span>
              <el-popover
                v-if="data.bindCount > 0"
                placement="right"
                :width="620"
                trigger="click"
                @before-enter="loadBindDetails(data.roleCode)"
              >
                <template #reference>
                  <el-tag size="small" type="warning" class="bind-count-tag" @click.stop>
                    {{ data.bindCount }}
                  </el-tag>
                </template>
                <el-table v-loading="popoverLoading" :data="bindDetails" size="small" height="auto">
                  <el-table-column label="绑定时间" prop="createTime" width="160">
                    <template #default="{ row }">{{ parseTime(row.createTime) }}</template>
                  </el-table-column>
                  <el-table-column label="有效开始" prop="startTime" width="160">
                    <template #default="{ row }">{{ parseTime(row.startTime) }}</template>
                  </el-table-column>
                  <el-table-column label="有效结束" prop="endTime" width="160">
                    <template #default="{ row }">{{ parseTime(row.endTime) }}</template>
                  </el-table-column>
                  <el-table-column label="状态" prop="enableStatus" width="70">
                    <template #default="{ row }">
                      <el-tag :type="row.enableStatus === 1 ? 'success' : 'danger'" size="small">
                        {{ row.enableStatus === 1 ? '有效' : '无效' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="60" fixed="right">
                    <template #default="{ row }">
                      <el-popconfirm title="确认移除该绑定?" @confirm="handleUnbind(row.id)">
                        <template #reference>
                          <el-button link type="danger" size="small">移除</el-button>
                        </template>
                      </el-popconfirm>
                    </template>
                  </el-table-column>
                </el-table>
              </el-popover>
            </span>
          </template>
        </el-tree>

        <!-- 绑定角色弹窗 -->
        <el-dialog title="绑定角色" v-model="bindDialogVisible" width="500px" :close-on-click-modal="false" append-to-body>
          <el-form ref="bindFormRef" :model="bindForm" :rules="bindRules" label-width="100px">
            <el-form-item label="选择角色" prop="roleCode">
              <el-tree-select
                v-model="bindForm.roleCode"
                :data="roleTreeData"
                :props="{ label: 'roleName', children: 'children', value: 'roleCode' }"
                placeholder="请选择角色"
                check-strictly
                filterable
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker v-model="bindForm.startTime" type="datetime" placeholder="请选择开始时间" style="width: 100%" />
            </el-form-item>
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker v-model="bindForm.endTime" type="datetime" placeholder="请选择结束时间" style="width: 100%" />
            </el-form-item>
          </el-form>
          <template #footer>
            <div class="dialog-footer">
              <el-button type="primary" :loading="bindLoading" @click="handleBindConfirm">确 定</el-button>
              <el-button @click="bindDialogVisible = false">取 消</el-button>
            </div>
          </template>
        </el-dialog>
      </template>
    </layout-split>
  </div>
</template>

<script setup name="IamUserRole">
import { userRoleList, userRoleBind, userRoleUnbind, userRoleTree } from "@/api/user/user-role";
import { parseTime } from "@/utils/ruoyi";
import LayoutSplit from "@/components/LayoutSplit";
import AppOptions from "@/views/components/AppOptions";

const props = defineProps({
  userCode: { type: String, default: '' }
});

const { proxy } = getCurrentInstance();

// 应用相关
const currentAppCode = ref('');

// 角色树相关
const treeLoading = ref(false);
const roleTreeData = ref([]);

// 绑定详情
const popoverLoading = ref(false)
const bindDetails = ref([]);

// 绑定角色弹窗
const bindDialogVisible = ref(false);
const bindLoading = ref(false);
const bindForm = ref({
  roleCode: '',
  startTime: null,
  endTime: null
});
const bindRules = ref({
  roleCode: [{ required: true, message: '请选择角色', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
});

// 监听 userCode 变化重新加载
watch(() => props.userCode, (val) => {
  if (val && currentAppCode.value) {
    loadRoleTree();
  }
});

/** 选择应用 */
function selectApp(row) {
  currentAppCode.value = row.appCode;
  if (row.appCode && props.userCode) {
    loadRoleTree();
  } else {
    roleTreeData.value = [];
  }
}

/** 加载角色树 */
function loadRoleTree() {
  if (!props.userCode || !currentAppCode.value) return;
  treeLoading.value = true;
  userRoleTree({ userCode: props.userCode, appCode: currentAppCode.value }).then(res => {
    roleTreeData.value = res.data || [];
  }).finally(() => {
    treeLoading.value = false;
  });
}

/** 加载绑定详情 */
function loadBindDetails(roleCode) {
  popoverLoading.value = true
  userRoleList({ userCode: props.userCode, roleCode: roleCode }).then(res => {
    bindDetails.value = res.data || []
  }).finally(() => {
    popoverLoading.value = false
  })
}

/** 打开绑定角色弹窗 */
function handleBind() {
  // 设置有效期默认值：当前时间 ~ 一年后当天 23:59:59
  const now = new Date()
  const oneYearLater = new Date(now)
  oneYearLater.setFullYear(oneYearLater.getFullYear() + 1)
  oneYearLater.setHours(23, 59, 59, 0)
  bindForm.value = { roleCode: '', startTime: now, endTime: oneYearLater }
  bindDialogVisible.value = true
}

/** 确认绑定角色 */
function handleBindConfirm() {
  proxy.$refs["bindFormRef"].validate(valid => {
    if (valid) {
      const data = {
        userCode: props.userCode,
        roleCode: bindForm.value.roleCode,
        startTime: bindForm.value.startTime,
        endTime: bindForm.value.endTime
      }
      bindLoading.value = true
      userRoleBind(data).then(() => {
        proxy.$modal.msgSuccess("绑定成功")
        bindDialogVisible.value = false
        loadRoleTree()
      }).finally(() => {
        bindLoading.value = false
      })
    }
  })
}

/** 解绑角色 */
function handleUnbind(id) {
  userRoleUnbind({ id: id }).then(() => {
    proxy.$modal.msgSuccess("移除成功")
    // 刷新绑定详情和角色树
    bindDetails.value = bindDetails.value.filter(item => item.id !== id)
    loadRoleTree()
  })
}
</script>

<style scoped>
.user-role-container {
  padding: 8px 0;
}

.top-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.custom-tree-node {
  display: flex;
  align-items: center;
  font-size: 14px;
  flex: 1;
}

.bind-count-tag {
  margin-left: 8px;
  cursor: pointer;
}
</style>
