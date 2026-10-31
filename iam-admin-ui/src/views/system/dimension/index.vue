<template>
  <div class="app-container">
    <layout-split>
      <template #left>
        <app-options @select="selectApp"/>
      </template>
      <template #right>
        <el-form :model="queryParams" ref="queryRef" :inline="true" @submit.native.prevent>
          <el-form-item prop="appCode">
            <el-input v-model="queryParams.appCode" disabled style="width: 120px" @keyup.enter.native="handleQuery" />
          </el-form-item>
          <el-form-item prop="dimensionCode">
            <el-input v-model="queryParams.dimensionCode" placeholder="编码, 模糊搜索" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
          </el-form-item>
          <el-form-item prop="dimensionName">
            <el-input v-model="queryParams.dimensionName" placeholder="名称, 模糊搜索" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
          <el-form-item style="float: right;">
            <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55" />
          <el-table-column label="ID" prop="id" width="80" v-if="columns.id.visible"/>
          <el-table-column label="应用编码" prop="appCode" width="120"/>
          <el-table-column label="元数据编码" prop="dimensionCode" width="120"/>
          <el-table-column label="元数据名称" prop="dimensionName" min-width="120"/>
          <el-table-column label="备注" prop="remark" min-width="120" v-if="columns.remark.visible"/>
          <el-table-column label="创建时间" prop="createTime" width="160" v-if="columns.createTime.visible">
            <template #default="{row}">{{ parseTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="创建人" prop="createBy" width="120" v-if="columns.createBy.visible"/>
          <el-table-column label="更新时间" prop="updateTime" width="160" v-if="columns.updateTime.visible">
            <template #default="{row}">{{ parseTime(row.updateTime) }}</template>
          </el-table-column>
          <el-table-column label="更新人" prop="updateBy" width="120" v-if="columns.updateBy.visible"/>
          <el-table-column fixed="right" width="98">
            <template #header><table-setting v-model:columns="columns"/></template>
            <template #default="{row}">
              <el-button link type="primary" icon="Edit" @click="handleUpdate(row)" title="编辑"/>
          <el-popconfirm :title="'确认删除数据维度:' + row.dimensionName + '?'" placement="top-end" @confirm="handleDelete(row)">
            <template #reference><el-button link type="danger" icon="Delete" title="删除"/></template>
          </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.current"
        v-model:limit="queryParams.size"
        @pagination="getList"
    />
        <div style="position: absolute; margin-top: -32px;">
          <el-dropdown @command="handleExport" style="vertical-align: middle;">
            <el-button type="warning" plain :loading="exportLoading">
              导出<el-icon class="el-icon--right"><arrow-down /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="all">导出全部（按当前筛选）</el-dropdown-item>
                <el-dropdown-item command="selected" :disabled="selectedRows.length === 0" divided>
                  导出选中（{{ selectedRows.length }} 条）
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button type="success" plain icon="Upload" @click="handleImport">导入</el-button>
        </div>
      </template>
    </layout-split>
    <edit ref="editRef" @change="getList"/>
    <import-dim ref="importRef" @change="getList"/>
  </div>
</template>

<script setup name="IamDataDimension">
import { datadimPage, datadimRemove, datadimExport } from "@/api/system/dim";
import Edit from "./components/edit"
import ImportDim from "./components/import"
import AppOptions from "@/views/components/AppOptions";
import { copy } from "@sh/ui-core/utils/shrimp"
import { ArrowDown } from "@element-plus/icons-vue"

const { proxy } = getCurrentInstance();

const dataList = ref([]);
const loading = ref(false);
const total = ref(0);
const selectedRows = ref([]);

const columns = ref({
  id: {label: "ID", visible: false},
  remark: {label: "备注", visible: true},
  updateBy: {label: "修改人", visible: false},
  updateTime: {label: "修改时间", visible: false},
  createBy: {label: "创建人", visible: false},
  createTime: {label: "创建时间", visible: false},
})

const queryParams = ref({
  current: 1,
  size: 20,
  appCode: undefined,
  dimensionCode: undefined,
  dimensionName: undefined,
  dimensionScript: undefined,
});


function selectApp(row) {
  queryParams.value.appCode = row.appCode;
  handleQuery();
}


/** 查询参数列表 */
function getList() {
  loading.value = true;
  datadimPage(queryParams.value).then(res => {
    const data = res.data;
    dataList.value = data.records;
    total.value = data.total;
  }).finally(() => {
    loading.value = false;
  });
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.current = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

function handleAdd() {
  if (!queryParams.value.appCode) {
    return;
  }
  proxy.$refs["editRef"].init({appCode: queryParams.value.appCode});
}
function handleUpdate(row) {
  proxy.$refs["editRef"].init(row);
}

/** 删除按钮操作 */
function handleDelete(row) {
  datadimRemove({id: row.id}).then(res => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  });
}

/** 处理表格选择变化 */
function handleSelectionChange(selection) {
  selectedRows.value = selection;
}

const exportLoading = ref(false);

/** 导出数据维度：selected=仅勾选项，all=按当前筛选全量；结果 JSON 复制到剪贴板 */
function handleExport(command) {
  const params = command === "selected"
    ? {ids: selectedRows.value.map(row => row.id).join(",")}
    : {
      appCode: queryParams.value.appCode,
      dimensionCode: queryParams.value.dimensionCode,
      dimensionName: queryParams.value.dimensionName,
    };
  exportLoading.value = true;
  datadimExport(params).then(res => {
    const list = res.data || [];
    if (list.length === 0) {
      proxy.$modal.msgWarning("没有可导出的数据");
      return;
    }
    copy(JSON.stringify(list, null, 2), "成功导出 " + list.length + " 条数据维度到剪贴板!");
  }).finally(() => {
    exportLoading.value = false;
  });
}

/** 导入按钮操作 */
function handleImport() {
  proxy.$refs["importRef"].init();
}

</script>

