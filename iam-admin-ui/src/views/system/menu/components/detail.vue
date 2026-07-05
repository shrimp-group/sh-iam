<template>
  <el-dialog :title="'菜单详情 - ' + menuInfo?.menuName" v-model="open" width="1200px">
    <el-tabs v-model="activeTab">
      <!-- 基本信息 -->
      <el-tab-pane label="基本信息" name="basic">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="菜单名称">{{ menuInfo?.menuName }}</el-descriptions-item>
          <el-descriptions-item label="菜单类型">
            <dict-tag :options="MENU_TYPE" :value="menuInfo?.menuType" />
          </el-descriptions-item>
          <el-descriptions-item label="应用编码">{{ menuInfo?.appCode }}</el-descriptions-item>
          <el-descriptions-item label="路由地址">{{ menuInfo?.routePath || '-' }}</el-descriptions-item>
          <el-descriptions-item label="组件">{{ menuInfo?.component || '-' }}</el-descriptions-item>
          <el-descriptions-item label="按钮编码">{{ menuInfo?.buttonCode || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <!-- 接口授权 -->
      <el-tab-pane label="接口授权" name="apiBind">
        <el-row :gutter="20">
          <!-- 左边：全量 API（已绑定的绑定按钮禁用） -->
          <el-col :span="12">
            <div class="api-section">
              <div class="section-header">
                <span class="section-title">全部接口</span>
                <el-input v-model="leftSearchKeyword" placeholder="输入接口地址或名称" clearable size="small" style="width: 200px"/>
              </div>
              <el-table v-loading="loading" :data="filteredAllApis" max-height="500" min-height="200">
                <el-table-column label="方法" prop="apiMethod" width="80">
                  <template #default="{row}"><dict-tag :options="API_METHOD" :value="row.apiMethod" /></template>
                </el-table-column>
                <el-table-column label="URI" prop="apiUri" min-width="150" />
                <el-table-column label="名称" prop="apiName" min-width="120" />
                <el-table-column label="操作" width="80" fixed="right">
                  <template #default="{row}">
                    <el-button link type="primary" size="small" :disabled="isBound(row)" @click="handleBind(row)">绑定</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-col>

          <!-- 右边：已绑定的 API -->
          <el-col :span="12">
            <div class="api-section">
              <div class="section-header">
                <span class="section-title">已绑定的接口</span>
                <el-input v-model="rightSearchKeyword" placeholder="输入接口地址或名称" clearable size="small" style="width: 200px"/>
              </div>
              <el-table :data="filteredBoundApis" max-height="500" min-height="200">
                <el-table-column label="方法" prop="apiMethod" width="80">
                  <template #default="{row}"><dict-tag :options="API_METHOD" :value="row.apiMethod" /></template>
                </el-table-column>
                <el-table-column label="URI" prop="apiUri" min-width="150" />
                <el-table-column label="名称" prop="apiName" min-width="120" />
                <el-table-column label="操作" width="80" fixed="right">
                  <template #default="{row}">
                    <el-button link type="danger" size="small" @click="handleUnbind(row)">解绑</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 字段绑定管理（仅 FIELDS 类型菜单显示） -->
      <el-tab-pane v-if="menuInfo?.menuType === 'FIELDS'" label="字段绑定" name="fieldBind">
        <el-row :gutter="20">
          <!-- 左边：可选的 API 字段 -->
          <el-col :span="12">
            <div class="api-section">
              <div class="section-header">
                <span class="section-title">可选字段</span>
                <el-input v-model="fieldSearchKeyword" placeholder="搜索字段名称" clearable size="small" style="width: 180px"/>
              </div>
              <el-table v-loading="fieldLoading" :data="filteredAvailableFields" max-height="500" min-height="200">
                <el-table-column label="字段名称" prop="fieldName" min-width="120" />
                <el-table-column label="JSON路径" prop="jsonPath" min-width="150" />
                <el-table-column label="权限动作" prop="action" width="100">
                  <template #default="{ row }">
                    <el-tag :type="actionTagType(row.action)" size="small">{{ actionLabel(row.action) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80" fixed="right">
                  <template #default="{row}">
                    <el-button link type="primary" size="small" :disabled="isFieldBound(row)" @click="handleFieldBind(row)">
                      {{ isFieldBound(row) ? '已绑定' : '绑定' }}
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-col>

          <!-- 右边：已绑定的字段 -->
          <el-col :span="12">
            <div class="api-section">
              <div class="section-header">
                <span class="section-title">已绑定的字段</span>
              </div>
              <el-table :data="boundFields" max-height="500" min-height="200">
                <el-table-column label="字段名称" prop="fieldName" min-width="120" />
                <el-table-column label="JSON路径" prop="jsonPath" min-width="150" />
                <el-table-column label="权限动作" prop="action" width="100">
                  <template #default="{ row }">
                    <el-tag :type="actionTagType(row.action)" size="small">{{ actionLabel(row.action) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="来源接口" min-width="120">
                  <template #default="{ row }">
                    <span v-if="row.apiMethod || row.apiUri">{{ row.apiMethod }} {{ row.apiUri }}</span>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80" fixed="right">
                  <template #default="{row}">
                    <el-button link type="danger" size="small" @click="handleFieldUnbind(row)">解绑</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-if="boundFields.length === 0" description="暂无绑定的字段" :image-size="40" />
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 已绑角色 -->
      <el-tab-pane label="已绑角色" name="boundRoles">
        <el-table :data="boundRoles" size="small" max-height="500" min-height="200">
          <el-table-column label="角色编码" prop="roleCode" min-width="150" />
          <el-table-column label="角色名称" prop="roleName" min-width="150" />
        </el-table>
      </el-tab-pane>

      <!-- 关联用户 -->
      <el-tab-pane label="关联用户" name="boundUsers">
        <el-table :data="boundUsers" size="small" max-height="500" min-height="200">
          <el-table-column label="用户名" prop="username" min-width="100" />
          <el-table-column label="昵称" prop="nickname" min-width="100" />
          <el-table-column label="来源角色" prop="roleName" min-width="120" />
          <el-table-column label="有效开始" prop="startTime" width="160">
            <template #default="{row}">{{ parseTime(row.startTime) }}</template>
          </el-table-column>
          <el-table-column label="有效结束" prop="endTime" width="160">
            <template #default="{row}">{{ parseTime(row.endTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" prop="enableStatus" width="80">
            <template #default="{row}">
              <el-tag :type="row.enableStatus === 1 ? 'success' : 'danger'" size="small">{{ row.enableStatus === 1 ? '有效' : '无效' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="close">关 闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="IamMenuDetail">
import { menuDetail } from "@/api/system/menu";
import { menuApiBoundList, menuApiBind, menuApiUnbind } from "@/api/system/menu-api";
import { menuBoundRoles, menuBoundUsers } from "@/api/user/user-role";
import { apiOptions } from "@/api/system/api";
import { menuFieldList, menuFieldBind, menuFieldUnbind, apiFieldListByApi } from "@/api/system/api-field";
import { parseTime } from "@/utils/ruoyi";

defineExpose({ init });
const { proxy } = getCurrentInstance();
const { API_METHOD, MENU_TYPE } = proxy.useDict("API_METHOD", "MENU_TYPE");

const open = ref(false);
const activeTab = ref("basic");
const menuInfo = ref(null);
const loading = ref(false);

// 全量 API 列表
const allApis = ref([]);
// 已绑定的 API 列表（含 menuApiId）
const boundApis = ref([]);
// 已绑定的角色列表
const boundRoles = ref([]);
// 关联用户列表
const boundUsers = ref([]);

// 字段绑定相关
const fieldLoading = ref(false);
const availableFields = ref([]);
const boundFields = ref([]);
const fieldSearchKeyword = ref("");

// 搜索关键词
const leftSearchKeyword = ref("");
const rightSearchKeyword = ref("");

// 权限动作标签映射
const actionMap = {
  HIDDEN: { label: '隐藏', type: 'danger' },
  MASK: { label: '脱敏', type: 'warning' },
  READ_ONLY: { label: '只读', type: 'info' }
}

function actionLabel(action) {
  return actionMap[action]?.label || action
}

function actionTagType(action) {
  return actionMap[action]?.type || 'info'
}

// 已绑定的 apiCode 集合（用于快速判断绑定状态）
const boundApiCodeSet = computed(() => {
  return new Set(boundApis.value.map(api => api.apiCode));
});

// 已绑定的字段 fieldCode 集合
const boundFieldCodeSet = computed(() => {
  return new Set(boundFields.value.map(f => f.fieldCode || f.id));
});

// 过滤后的全量 API 列表
const filteredAllApis = computed(() => {
  if (!leftSearchKeyword.value) {
    return allApis.value;
  }
  const keyword = leftSearchKeyword.value.toLowerCase();
  return allApis.value.filter(api => (api.apiUri && api.apiUri.toLowerCase().includes(keyword)) || (api.apiName && api.apiName.toLowerCase().includes(keyword)));
});

// 过滤后的已绑定 API 列表
const filteredBoundApis = computed(() => {
  if (!rightSearchKeyword.value) {
    return boundApis.value;
  }
  const keyword = rightSearchKeyword.value.toLowerCase();
  return boundApis.value.filter(api => (api.apiUri && api.apiUri.toLowerCase().includes(keyword)) || (api.apiName && api.apiName.toLowerCase().includes(keyword)));
});

// 过滤后的可选字段列表
const filteredAvailableFields = computed(() => {
  if (!fieldSearchKeyword.value) return availableFields.value
  const keyword = fieldSearchKeyword.value.toLowerCase()
  return availableFields.value.filter(f =>
    (f.fieldName && f.fieldName.toLowerCase().includes(keyword)) ||
    (f.jsonPath && f.jsonPath.toLowerCase().includes(keyword))
  )
})

// 判断 API 是否已绑定
function isBound(api) {
  return boundApiCodeSet.value.has(api.apiCode);
}

// 判断字段是否已绑定
function isFieldBound(field) {
  return boundFieldCodeSet.value.has(field.fieldCode || field.id);
}

// 初始化
function init(row) {
  open.value = true;
  activeTab.value = "basic";
  leftSearchKeyword.value = "";
  rightSearchKeyword.value = "";
  fieldSearchKeyword.value = "";
  allApis.value = [];
  boundApis.value = [];
  boundRoles.value = [];
  boundUsers.value = [];
  availableFields.value = [];
  boundFields.value = [];
  // 加载菜单详情
  menuDetail({ id: row.id }).then(res => {
    menuInfo.value = res.data;
    // 并行加载全量 API、已绑定 API、已绑定角色和关联用户
    loading.value = true;
    const promises = [
      apiOptions({ appCode: menuInfo.value?.appCode }),
      menuApiBoundList({ menuCode: menuInfo.value?.menuCode }),
      menuBoundRoles({ menuCode: menuInfo.value?.menuCode }),
      menuBoundUsers({ menuCode: menuInfo.value?.menuCode })
    ];
    // FIELDS 类型菜单额外加载字段绑定数据
    if (menuInfo.value?.menuType === 'FIELDS') {
      promises.push(loadMenuFields());
    }
    Promise.all(promises).then(([allRes, boundRes, rolesRes, usersRes]) => {
      allApis.value = allRes.data || [];
      boundApis.value = boundRes.data || [];
      boundRoles.value = rolesRes.data || [];
      boundUsers.value = usersRes.data || [];
    }).finally(() => {
      loading.value = false;
    });
  });
}

// 加载菜单字段绑定数据
function loadMenuFields() {
  fieldLoading.value = true;
  const menuCode = menuInfo.value?.menuCode;
  const appCode = menuInfo.value?.appCode;
  // 加载已绑定的字段和该应用下所有 API 的字段
  Promise.all([
    menuFieldList({ menuCode }),
    loadAppApiFields(appCode)
  ]).then(([boundRes, allFieldsRes]) => {
    boundFields.value = boundRes.data || [];
    availableFields.value = allFieldsRes;
  }).finally(() => {
    fieldLoading.value = false;
  });
}

// 加载应用下所有 API 的字段（去重）
function loadAppApiFields(appCode) {
  return apiOptions({ appCode }).then(res => {
    const apis = res.data || [];
    if (apis.length === 0) return [];
    // 并行查询每个 API 的字段
    const fieldPromises = apis.map(api =>
      apiFieldListByApi({ apiCode: api.apiCode }).then(fieldRes => {
        const fields = fieldRes.data || [];
        // 为每个字段附加 API 信息
        return fields.map(f => ({
          ...f,
          apiMethod: api.apiMethod,
          apiUri: api.apiUri,
          apiName: api.apiName
        }));
      })
    );
    return Promise.all(fieldPromises).then(results => {
      // 合并并去重（按 fieldCode 去重）
      const allFields = results.flat();
      const seen = new Set();
      return allFields.filter(f => {
        const key = f.fieldCode || f.jsonPath;
        if (seen.has(key)) return false;
        seen.add(key);
        return true;
      });
    });
  });
}

// 绑定 API（前端本地更新，不重新查询列表）
function handleBind(api) {
  const data = {
    appCode: menuInfo.value?.appCode,
    menuCode: menuInfo.value?.menuCode,
    apiCode: api?.apiCode,
  };
  menuApiBind(data).then(res => {
    proxy.$modal.msgSuccess("绑定成功");
    // 前端本地更新：将 API 添加到已绑定列表
    const boundItem = { ...api, menuApiId: res.data?.id };
    boundApis.value.push(boundItem);
  });
}

// 解绑 API（前端本地更新，不重新查询列表）
function handleUnbind(api) {
  const data = { id: api.menuApiId };
  menuApiUnbind(data).then(() => {
    proxy.$modal.msgSuccess("解绑成功");
    // 前端本地更新：从已绑定列表移除
    boundApis.value = boundApis.value.filter(item => item.apiCode !== api.apiCode);
  });
}

// 绑定字段
function handleFieldBind(field) {
  const data = {
    appCode: menuInfo.value?.appCode,
    menuCode: menuInfo.value?.menuCode,
    fieldCode: field.fieldCode,
  };
  menuFieldBind(data).then(res => {
    proxy.$modal.msgSuccess("绑定成功");
    boundFields.value.push({ ...field, menuFieldId: res.data?.id });
  });
}

// 解绑字段
function handleFieldUnbind(field) {
  const data = { id: field.menuFieldId || field.id };
  menuFieldUnbind(data).then(() => {
    proxy.$modal.msgSuccess("解绑成功");
    boundFields.value = boundFields.value.filter(f => (f.fieldCode || f.id) !== (field.fieldCode || field.id));
  });
}

// 关闭弹窗
function close() {
  open.value = false;
}
</script>

<style scoped>
.api-section {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 15px;
  background-color: #f5f7fa;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
</style>
