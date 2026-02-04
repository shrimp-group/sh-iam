<template>
  <div class="app-container">
    <layout-split>
      <template #left>
        <app-options @select="selectApp"/>
      </template>
      <template #right>
        <el-form :model="queryParams" ref="queryRef" :inline="true" @submit.native.prevent label-width="1px;">
          <el-form-item prop="appCode">
            <el-input v-model="queryParams.appCode" disabled style="width: 160px" @keyup.enter.native="handleQuery" />
          </el-form-item>
          <el-form-item prop="module">
            <el-input v-model="queryParams.module" placeholder="模块" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
          </el-form-item>
          <el-form-item prop="apiMethod">
            <el-select v-model="queryParams.apiMethod" placeholder="请求方法" clearable filterable style="width: 120px" @change="handleQuery">
              <el-option v-for="item in API_METHOD" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item prop="apiUri">
            <el-input v-model="queryParams.apiUri" placeholder="URI,模糊搜索" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
          </el-form-item>
          <el-form-item prop="apiName">
            <el-input v-model="queryParams.apiName" placeholder="名称,模糊搜索" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
          </el-form-item>
          <el-form-item prop="writeFlag">
            <el-select v-model="queryParams.writeFlag" placeholder="白名单" clearable filterable style="width: 100px" @change="handleQuery">
              <el-option v-for="item in BOOLEAN" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
          <el-form-item style="float: right;">
            <el-button type="primary" plain icon="Plus" @click="handleAdd" :disabled="!queryParams.appCode">新增</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="loading" :data="dataList">
          <el-table-column label="ID" prop="id" width="80" v-if="columns.id.visible"/>
          <el-table-column label="模块" prop="module" width="120"/>
          <el-table-column label="应用编码" prop="appCode" width="120"/>
          <el-table-column label="请求方法" prop="apiMethod" width="120">
            <template #default="{row}"><dict-tag :options="API_METHOD" :value="row.apiMethod" /></template>
          </el-table-column>
          <el-table-column label="URI" prop="apiUri" min-width="120"/>
          <el-table-column label="接口名称" prop="apiName" min-width="120"/>
          <el-table-column label="白名单" prop="writeFlag" width="80">
            <template #default="{row}"><dict-tag :options="BOOLEAN" :value="row.writeFlag" /></template>
          </el-table-column>
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
              <el-button link type="danger" icon="Delete" @click="handleDelete(row)" title="删除"/>
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
      </template>
    </layout-split>

    <edit ref="editRef" @change="getList"/>
  </div>
</template>

<script setup name="IamApi">
import { apiPage, apiRemove} from "@/api/system/api";
import Edit from "./components/edit.vue"
import AppOptions from "@/views/components/AppOptions"

const { proxy } = getCurrentInstance();
const { BOOLEAN, API_METHOD } = proxy.useDict("BOOLEAN", "API_METHOD");
const dataList = ref([]);
const loading = ref(false);
const total = ref(0);

const appName = ref('');

const columns = ref({
  id: {label: "ID", visible: false},
  sort: {label: "排序", visible: true},
  remark: {label: "备注", visible: true},
  updateBy: {label: "修改人", visible: false},
  updateTime: {label: "修改时间", visible: false},
  createBy: {label: "创建人", visible: false},
  createTime: {label: "创建时间", visible: false},
})

const queryParams = ref({
  current: 1,
  size: 20,
  module: undefined,
  appCode: undefined,
  apiMethod: undefined,
  apiUri: undefined,
  apiName: undefined,
  writeFlag: undefined,
});

function selectApp(row) {
  queryParams.value.appCode = row.appCode;
  appName.value = row.appName;
  handleQuery();
}

/** 查询参数列表 */
function getList() {
  loading.value = true;
  apiPage(queryParams.value).then(res => {
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
  proxy.$modal.confirm('是否确认删除 :"' + row.id + '"？').then(() => {
    apiRemove({id: row.id}).then(res => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
  }).catch(() => {});
}

</script>
