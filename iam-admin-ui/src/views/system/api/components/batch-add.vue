<template>
  <el-dialog title="批量新增API" v-model="open" width="1440px" :close-on-click-modal="false" top="3vh">
    <el-alert
      v-if="appCode"
      :title="'当前应用：' + appCode + '（新增的接口将归属此应用）'"
      type="info"
      :closable="false"
      style="margin-bottom: 12px;" />
    <!-- 搜索条件区 -->
    <el-form :inline="true" :model="searchForm" @submit.native.prevent>
      <el-form-item label="模块">
        <el-input v-model="searchForm.module" placeholder="模块" clearable style="width: 130px" />
      </el-form-item>
      <el-form-item label="请求方法">
        <el-select v-model="searchForm.apiMethod" placeholder="请求方法" clearable style="width: 110px">
          <el-option v-for="item in API_METHOD" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="URI">
        <el-input v-model="searchForm.apiUri" placeholder="URI" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item label="接口名称">
        <el-input v-model="searchForm.apiName" placeholder="接口名称" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item label="入库状态">
        <el-select v-model="searchForm.status" placeholder="入库状态" clearable style="width: 110px">
          <el-option label="可新增" value="addable" />
          <el-option label="已入库" value="exists" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
        <el-button icon="Refresh" @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 扫描结果表格 -->
    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="scanList"
      height="600"
      @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" :selectable="row => !row.exists" />
      <el-table-column label="模块" prop="module" width="100" show-overflow-tooltip />
      <el-table-column label="请求方法" prop="apiMethod" width="90">
        <template #default="{row}"><dict-tag :options="API_METHOD" :value="row.apiMethod" /></template>
      </el-table-column>
      <el-table-column label="URI" prop="apiUri" min-width="170" show-overflow-tooltip />
      <el-table-column label="接口名称" width="160">
        <template #default="{row}">
          <el-input
            v-model="row.apiName"
            placeholder="请输入接口名称"
            :disabled="row.exists"
            size="small"
            clearable />
        </template>
      </el-table-column>
      <el-table-column label="白名单" width="80" align="center">
        <template #default="{row}">
          <el-switch
            v-model="row.writeFlag"
            :active-value="1"
            :inactive-value="0"
            :disabled="row.exists" />
        </template>
      </el-table-column>
      <el-table-column label="排序" width="80">
        <template #default="{row}">
          <el-input-number
            v-model="row.sort"
            :min="0"
            :disabled="row.exists"
            size="small"
            controls-position="right"
            style="width: 70px" />
        </template>
      </el-table-column>
      <el-table-column label="备注" width="130">
        <template #default="{row}">
          <el-input
            v-model="row.remark"
            placeholder="备注"
            :disabled="row.exists"
            size="small" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{row}">
          <el-tag :type="row.exists ? 'info' : 'success'" size="small">
            {{ row.exists ? '已入库' : '可新增' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="70" fixed="right" align="center">
        <template #default="{row}">
          <el-button
            v-if="!row.exists"
            link
            type="primary"
            size="small"
            :loading="row.submitting"
            @click="handleSingleAdd(row)">入库</el-button>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <div class="dialog-footer">
        <span style="float: left; color: #909399; line-height: 32px;">
          已选 {{ selectedRows.length }} 条 / 共 {{ scanList.length }} 条
        </span>
        <el-button
          type="primary"
          :loading="submitLoading"
          :disabled="selectedRows.length === 0"
          @click="handleBatchSubmit">批量提交</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="ApiBatchAdd">
import { apiScan, apiBatchCreate } from "@/api/system/api";

defineExpose({init});
const emit = defineEmits(["change"]);
const { proxy } = getCurrentInstance();
const { API_METHOD } = proxy.useDict("API_METHOD");

const open = ref(false);
const loading = ref(false);
const submitLoading = ref(false);
const tableRef = ref(null);
const scanList = ref([]);
const selectedRows = ref([]);
const appCode = ref("");

const searchForm = ref({
  module: undefined,
  apiMethod: undefined,
  apiUri: undefined,
  apiName: undefined,
  status: undefined,
});

/** 打开弹窗（默认不搜索，需手动点击搜索） */
function init(code) {
  appCode.value = code || "";
  open.value = true;
}

/** 加载扫描结果（后端过滤） */
function loadScan() {
  loading.value = true;
  const params = buildScanParams();
  apiScan(params).then(res => {
    const list = res.data || [];
    scanList.value = list.map(item => ({
      ...item,
      writeFlag: item.writeFlag || 0,
      sort: item.sort || 99,
      remark: item.remark || "",
      submitting: false,
    }));
  }).finally(() => {
    loading.value = false;
  });
}

/** 构建后端扫描过滤参数 */
function buildScanParams() {
  const sf = searchForm.value;
  const params = {};
  if (sf.module) params.module = sf.module;
  if (sf.apiMethod) params.apiMethod = sf.apiMethod;
  if (sf.apiUri) params.apiUri = sf.apiUri;
  if (sf.apiName) params.apiName = sf.apiName;
  if (sf.status === "addable") params.exists = false;
  else if (sf.status === "exists") params.exists = true;
  return params;
}

/** 搜索：将条件提交后端过滤 */
function handleSearch() {
  loadScan();
}

/** 重置搜索条件并清空表格 */
function resetSearch() {
  searchForm.value = {
    module: undefined,
    apiMethod: undefined,
    apiUri: undefined,
    apiName: undefined,
    status: undefined,
  };
  scanList.value = [];
  selectedRows.value = [];
}

/** 表格选择变化 */
function handleSelectionChange(selection) {
  selectedRows.value = selection;
}

/** 单条入库 */
function handleSingleAdd(row) {
  if (!appCode.value) {
    proxy.$modal.msgWarning("请先在左侧选择应用");
    return;
  }
  if (!row.apiName) {
    proxy.$modal.msgWarning("请先填写接口名称");
    return;
  }
  row.submitting = true;
  apiBatchCreate({items: [buildItem(row)]}).then(res => {
    const d = res.data || {};
    if (d.successCount > 0) {
      proxy.$modal.msgSuccess("入库成功");
      markRowExists(row);
    } else {
      const reason = (d.failedItems && d.failedItems[0] && d.failedItems[0].reason) || "入库失败";
      proxy.$modal.msgError(reason);
    }
    emit("change", true);
  }).finally(() => {
    row.submitting = false;
  });
}

/** 批量提交 */
function handleBatchSubmit() {
  if (!appCode.value) {
    proxy.$modal.msgWarning("请先在左侧选择应用");
    return;
  }
  const rows = selectedRows.value.filter(r => !r.exists);
  if (rows.length === 0) {
    proxy.$modal.msgWarning("请勾选可新增的接口");
    return;
  }
  const emptyName = rows.find(r => !r.apiName);
  if (emptyName) {
    proxy.$modal.msgWarning(`接口 ${emptyName.apiMethod} ${emptyName.apiUri} 的接口名称不能为空`);
    return;
  }
  submitLoading.value = true;
  apiBatchCreate({items: rows.map(buildItem)}).then(res => {
    const d = res.data || {};
    proxy.$modal.msgSuccess(`批量提交完成：成功 ${d.successCount || 0} 条，失败 ${d.failedCount || 0} 条`);
    // 成功行标记为已入库
    const failedKeys = new Set(
      (d.failedItems || []).map(f => (f.apiMethod || "").toUpperCase() + "|" + f.apiUri)
    );
    rows.forEach(r => {
      const key = (r.apiMethod || "").toUpperCase() + "|" + r.apiUri;
      if (!failedKeys.has(key)) {
        markRowExists(r);
      }
    });
    emit("change", true);
  }).finally(() => {
    submitLoading.value = false;
  });
}

/** 构建提交项（appCode 使用页面已选中的应用编码） */
function buildItem(row) {
  return {
    module: row.module,
    appCode: appCode.value,
    apiMethod: row.apiMethod,
    apiUri: row.apiUri,
    apiName: row.apiName,
    writeFlag: row.writeFlag,
    sort: row.sort,
    remark: row.remark,
  };
}

/** 将行标记为已入库，并从选中项移除 */
function markRowExists(row) {
  row.exists = true;
  if (tableRef.value) {
    tableRef.value.toggleRowSelection(row, false);
  }
}

/** 取消并重置 */
function cancel() {
  open.value = false;
  appCode.value = "";
  scanList.value = [];
  selectedRows.value = [];
  resetSearch();
}
</script>
