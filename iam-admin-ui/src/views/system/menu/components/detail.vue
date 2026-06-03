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
      <!-- 左边：未绑定的 API -->
      <el-col :span="12">
        <div class="api-section">
          <div class="section-header">
            <span class="section-title">未绑定的 API</span>
            <el-input v-model="leftSearchKeyword" placeholder="输入接口地址或名称" clearable size="small" style="width: 200px"/>
          </div>
          <el-table v-loading="leftLoading" :data="filteredLeftApis">
            <el-table-column label="方法" prop="apiMethod" width="80">
              <template #default="{row}"><dict-tag :options="API_METHOD" :value="row.apiMethod" /></template>
            </el-table-column>
            <el-table-column label="URI" prop="apiUri" min-width="150" />
            <el-table-column label="名称" prop="apiName" min-width="120" />
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{row}">
                <el-button link type="primary" size="small" @click="handleBind(row)">绑定</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>

      <!-- 右边：已绑定的 API -->
      <el-col :span="12">
        <div class="api-section">
          <div class="section-header">
            <span class="section-title">已绑定的 API</span>
            <el-input v-model="rightSearchKeyword" placeholder="输入接口地址或名称" clearable size="small" style="width: 200px"/>
          </div>
          <el-table v-loading="rightLoading" :data="filteredRightApis">
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
import { menuApiBoundList, menuApiUnboundList, menuApiBind, menuApiUnbind } from "@/api/system/menu-api";

defineExpose({ init });
const { proxy } = getCurrentInstance();
const { API_METHOD, MENU_TYPE } = proxy.useDict("API_METHOD", "MENU_TYPE");

const open = ref(false);
const menuInfo = ref(null);

// 左边：未绑定的 API
const unboundApis = ref([]);
const leftLoading = ref(false);
const leftSearchKeyword = ref("");

// 右边：已绑定的 API
const boundApis = ref([]);
const rightLoading = ref(false);
const rightSearchKeyword = ref("");

// 过滤后的左边 API 列表
const filteredLeftApis = computed(() => {
  if (!leftSearchKeyword.value) {
    return unboundApis.value;
  }
  const keyword = leftSearchKeyword.value.toLowerCase();
  return unboundApis.value.filter(api => (api.apiUri && api.apiUri.toLowerCase().includes(keyword)) || (api.apiName && api.apiName.toLowerCase().includes(keyword)));
});

// 过滤后的右边 API 列表
const filteredRightApis = computed(() => {
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
  // 加载菜单详情
  menuDetail({ id: row.id }).then(res => {
    menuInfo.value = res.data;
    // 加载绑定/未绑定列表
    loadBoundApis();
    loadUnboundApis();
  });
}

// 加载已绑定的 API
function loadBoundApis() {
  rightLoading.value = true;
  menuApiBoundList({ menuCode: menuInfo.value?.menuCode }).then(res => {
    boundApis.value = res.data || [];
  }).finally(() => {
    rightLoading.value = false;
  });
}

// 加载未绑定的 API
function loadUnboundApis() {
  leftLoading.value = true;
  menuApiUnboundList({ menuCode: menuInfo.value?.menuCode, appCode: menuInfo.value?.appCode }).then(res => {
    unboundApis.value = res.data || [];
  }).finally(() => {
    leftLoading.value = false;
  });
}

// 绑定 API
function handleBind(api) {
  const data = {
    appCode: menuInfo.value?.appCode,
    menuCode: menuInfo.value?.menuCode,
    apiCode: api?.apiCode,
  };
  menuApiBind(data).then(() => {
    proxy.$modal.msgSuccess("绑定成功");
    loadBoundApis();
    loadUnboundApis();
  });
}

// 解绑 API
function handleUnbind(api) {
  const data = { id: api.menuApiId };
  menuApiUnbind(data).then(() => {
    proxy.$modal.msgSuccess("解绑成功");
    loadBoundApis();
    loadUnboundApis();
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
