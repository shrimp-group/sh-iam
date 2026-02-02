<template>
  <div>
    <el-form :model="queryParams" ref="queryRef" :inline="true">
      <el-form-item prop="keyword" style="width: 100%">
        <el-input v-model="queryParams.keyword" placeholder="实时查询应用" clearable style="width: 100%" @input="handleFilter"/>
      </el-form-item>
    </el-form>
    <el-table
        v-loading="loading"
        :style="{ maxHeight: height || 'auto' }"
        highlight-current-row
        :data="displayDataList"
        ref="tableRef"
        @current-change="handleSelect"
        :show-header="false">
      <el-table-column label="应用" min-width="160">
        <template #default="{row}">
          <el-tooltip class="box-item" effect="dark" :content="row.domain" placement="right">
            {{ row.displayName }}
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup name="AppSelect">
import {appOptions} from "@/api/system/app";

const props = defineProps({
  height: {
    type: [Number, String],
    default: undefined,
  },
});

const current_app_code_key = "current_app";

const { proxy } = getCurrentInstance();
const emit = defineEmits(['select']);
const dataList = ref([]);
const displayDataList = ref([]);
const loading = ref(true);
const queryParams = ref({
  keyword: undefined,
});

let currentAppCode = undefined;

function init() {
  getList();
}

function getList() {
  loading.value = true;
  appOptions(queryParams.value).then(res => {
    dataList.value = res.data;
    if (dataList.value.length > 0) {
      for (const e of dataList.value) {
        e.displayName = e.appCode + ':' + e.appName
      }
      handleFilter();
      handleSelect();
    }
  }).finally(() => {
    loading.value = false;
  });
}

function handleFilter() {
  displayDataList.value = [];
  const tmp = [];
  const keyword = queryParams.value.keyword;
  for (const data of dataList.value) {
    if (!keyword || (data.displayName.indexOf(keyword) > -1)) {
      tmp.push(data);
    }
  }
  displayDataList.value = tmp;
}
function handleSelect(row) {
  // 手动选择
  if (row && row.appCode) {
    invokeSelect(row);
    return;
  }
  // 没有可选项
  if (!dataList.value || dataList.value.length === 0) {
    return;
  }
  // 匹配上次选择
  const appCode = localStorage.getItem(current_app_code_key);
  if (appCode) {
    for (const d of dataList.value) {
      if (d.appCode === appCode) {
        invokeSelect(d);
        return;
      }
    }
  }
  // 默认选择
  invokeSelect(dataList.value[0]);
}

function invokeSelect(app) {
  localStorage.setItem(current_app_code_key, app.appCode);
  if (currentAppCode && currentAppCode === app.appCode) {
    return;
  }
  currentAppCode = app.appCode;
  proxy.$refs["tableRef"].setCurrentRow(app);
  emit("select", app);
}

init();
</script>

<style lang="scss" scoped>
.el-table {
  height: calc(100vh - 130px);
}
</style>
