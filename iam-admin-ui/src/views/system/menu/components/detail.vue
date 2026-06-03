<template>
  <el-dialog :title="'菜单详情 - ' + menuInfo?.menuName" v-model="open" width="90%" :close-on-click-modal="false">
    <!-- 菜单基本信息（不可编辑） -->
    <el-descriptions :column="3" border size="small" style="margin-bottom: 16px;">
      <el-descriptions-item label="菜单名称">{{ menuInfo?.menuName }}</el-descriptions-item>
      <el-descriptions-item label="菜单类型">
        <dict-tag :options="MENU_TYPE" :value="menuInfo?.menuType" />
      </el-descriptions-item>
      <el-descriptions-item label="应用编码">{{ menuInfo?.appCode }}</el-descriptions-item>
      <el-descriptions-item label="路由地址">{{ menuInfo?.routePath }}</el-descriptions-item>
      <el-descriptions-item label="组件">{{ menuInfo?.component }}</el-descriptions-item>
      <el-descriptions-item label="按钮编码">{{ menuInfo?.buttonCode }}</el-descriptions-item>
    </el-descriptions>

    <el-row :gutter="20">
      <!-- 左边：全量 API（已绑定的绑定按钮禁用） -->
      <el-col :span="12">
        <div class="api-section">
          <div class="section-header">
            <span class="section-title">全部接口</span>
            <el-input v-model="leftSearchKeyword" placeholder="输入接口地址或名称" clearable size="small" style="width: 200px"/>
          </div>
          <el-table v-loading="loading" :data="filteredAllApis">
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
          <el-table :data="filteredBoundApis">
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
import { apiOptions } from "@/api/system/api";

defineExpose({ init });
const { proxy } = getCurrentInstance();
const { API_METHOD, MENU_TYPE } = proxy.useDict("API_METHOD", "MENU_TYPE");

const open = ref(false);
const menuInfo = ref(null);
const loading = ref(false);

// 全量 API 列表
const allApis = ref([]);
// 已绑定的 API 列表（含 menuApiId）
const boundApis = ref([]);

// 搜索关键词
const leftSearchKeyword = ref("");
const rightSearchKeyword = ref("");

// 已绑定的 apiCode 集合（用于快速判断绑定状态）
const boundApiCodeSet = computed(() => {
  return new Set(boundApis.value.map(api => api.apiCode));
});

// 判断 API 是否已绑定
function isBound(api) {
  return boundApiCodeSet.value.has(api.apiCode);
}

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

// 初始化
function init(row) {
  open.value = true;
  leftSearchKeyword.value = "";
  rightSearchKeyword.value = "";
  allApis.value = [];
  boundApis.value = [];
  // 加载菜单详情
  menuDetail({ id: row.id }).then(res => {
    menuInfo.value = res.data;
    // 并行加载全量 API 和已绑定 API
    loading.value = true;
    Promise.all([
      apiOptions({ appCode: menuInfo.value?.appCode }),
      menuApiBoundList({ menuCode: menuInfo.value?.menuCode })
    ]).then(([allRes, boundRes]) => {
      allApis.value = allRes.data || [];
      boundApis.value = boundRes.data || [];
    }).finally(() => {
      loading.value = false;
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

.el-table {
  min-height: 320px;
  height: calc(100vh - 500px);
}
</style>
