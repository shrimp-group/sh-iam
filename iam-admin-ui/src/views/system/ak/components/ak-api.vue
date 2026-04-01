<template>
  <el-dialog :title="'API 授权 - ' + currentRow?.appId" v-model="open" width="1200px" :close-on-click-modal="false">
    <el-row :gutter="20">
      <!-- 左边：全部 API -->
      <el-col :span="12">
        <div class="api-section">
          <div class="section-header">
            <span class="section-title">全部 API</span>
            <el-input v-model="leftSearchKeyword" placeholder="输入接口地址或名称" clearable size="small" style="width: 200px" @input="filterLeftApis"/>
          </div>
          <el-table v-loading="leftLoading" :data="filteredLeftApis">
            <el-table-column label="模块" prop="module" width="100" />
            <el-table-column label="请求方法" prop="apiMethod" width="80">
              <template #default="{row}"><dict-tag :options="API_METHOD" :value="row.apiMethod" /></template>
            </el-table-column>
            <el-table-column label="URI" prop="apiUri" min-width="150" />
            <el-table-column label="名称" prop="apiName" min-width="120" />
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{row}">
                <el-button link type="primary" size="small" :disabled="isApiBound(row.apiCode)" @click="handleBind(row)">
                  {{ isApiBound(row.apiCode) ? '已绑定' : '绑定' }}
                </el-button>
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
            <el-input v-model="rightSearchKeyword" placeholder="输入接口地址或名称" clearable size="small" style="width: 200px" @input="filterRightApis"/>
          </div>
          <el-table v-loading="rightLoading" :data="filteredRightApis">
            <el-table-column label="模块" prop="module" width="100" />
            <el-table-column label="请求方法" prop="apiMethod" width="80">
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

<script setup name="IamAccessKeyApiAuth">
import { apiOptions } from "@/api/system/api";
import { accesskeyapiList, accesskeyapiBind, accesskeyapiUnbind } from "@/api/system/ak-api";

defineExpose({ init });
const { proxy } = getCurrentInstance();
const { API_METHOD } = proxy.useDict("API_METHOD");

const open = ref(false);
const currentRow = ref(null);

// 左边：全部 API
const allApis = ref([]);
const leftLoading = ref(false);
const leftSearchKeyword = ref("");

// 右边：已绑定的 API
const boundApis = ref([]);
const rightLoading = ref(false);
const rightSearchKeyword = ref("");

// 已绑定的 apiCode 集合，用于快速判断
const boundApiCodes = ref(new Set());

// 过滤后的左边 API 列表
const filteredLeftApis = computed(() => {
  if (!leftSearchKeyword.value) {
    return allApis.value;
  }
  const keyword = leftSearchKeyword.value.toLowerCase();
  return allApis.value.filter(api => (api.apiUri && api.apiUri.toLowerCase().includes(keyword)) || (api.apiName && api.apiName.toLowerCase().includes(keyword)));
});

// 过滤后的右边 API 列表
const filteredRightApis = computed(() => {
  if (!rightSearchKeyword.value) {
    return boundApis.value;
  }
  const keyword = rightSearchKeyword.value.toLowerCase();
  return boundApis.value.filter(api => (api.apiUri && api.apiUri.toLowerCase().includes(keyword)) || (api.apiName && api.apiName.toLowerCase().includes(keyword)));
});

// 判断 API 是否已绑定
function isApiBound(apiCode) {
  return boundApiCodes.value.has(apiCode);
}

// 初始化
function init(row) {
  currentRow.value = row;
  open.value = true;
  leftSearchKeyword.value = "";
  rightSearchKeyword.value = "";
  loadAllApis();
  loadBoundApis();
}

// 加载全部 API
function loadAllApis() {
  leftLoading.value = true;
  apiOptions({ appCode: currentRow.value?.appCode }).then(res => {
    allApis.value = res.data || [];
  }).finally(() => {
    leftLoading.value = false;
  });
}

// 加载已绑定的 API
function loadBoundApis() {
  rightLoading.value = true;
  accesskeyapiList({ appId: currentRow.value?.appId }).then(res => {
    boundApis.value = res.data || [];
    // 更新已绑定的 apiCode 集合
    boundApiCodes.value = new Set(boundApis.value.map(api => api.apiCode));
  }).finally(() => {
    rightLoading.value = false;
  });
}

// 绑定 API
function handleBind(api) {
  const data = {
    appCode: currentRow.value?.appCode,
    appId: currentRow.value?.appId,
    apiCode: api?.apiCode,
  };
  accesskeyapiBind(data).then(() => {
    proxy.$modal.msgSuccess("绑定成功");
    loadBoundApis();
  });
}

// 解绑 API
function handleUnbind(api) {
  const data = {id: api.id};
  accesskeyapiUnbind(data).then(() => {
    proxy.$modal.msgSuccess("解绑成功");
    loadBoundApis();
  });
}

// 过滤左边 API（前端实时过滤）
function filterLeftApis() {
  // 使用 computed 属性自动过滤
}

// 过滤右边 API（前端实时过滤）
function filterRightApis() {
  // 使用 computed 属性自动过滤
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
  height: calc(100vh - 400px);
}

</style>
