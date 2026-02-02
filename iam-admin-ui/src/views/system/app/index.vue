<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" @submit.native.prevent label-width="1px;">
      <el-form-item prop="appCode">
        <el-input v-model="queryParams.appCode" placeholder="应用编码,模糊搜索" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item prop="appName">
        <el-input v-model="queryParams.appName" placeholder="应用名称,模糊搜索" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item prop="authType">
        <el-select v-model="queryParams.authType" placeholder="鉴权类型" clearable filterable style="width: 160px" @change="handleQuery">
          <el-option v-for="item in AUTH_TYPE" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
      <el-form-item style="float: right;">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="ID" prop="id" width="80" v-if="columns.id.visible"/>
      <el-table-column label="应用编码" prop="appCode" min-width="120"/>
      <el-table-column label="应用名称" prop="appName" min-width="120"/>
      <el-table-column label="鉴权类型" prop="authType" width="130">
        <template #default="{row}"><dict-tag :options="AUTH_TYPE" :value="row.authType" /></template>
      </el-table-column>
      <el-table-column label="应用域名" prop="domain" min-width="120"/>
      <el-table-column label="备注" prop="remark" min-width="120" v-if="columns.remark.visible"/>
      <el-table-column label="排序" prop="sort" width="80" v-if="columns.sort.visible"/>
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
    <edit ref="editRef" @change="getList"/>
  </div>
</template>

<script setup name="IamApp">
import { appPage, appRemove} from "@/api/system/app";
import Edit from "./components/edit.vue"

const { proxy } = getCurrentInstance();
const { AUTH_TYPE } = proxy.useDict("AUTH_TYPE");
const dataList = ref([]);
const loading = ref(false);
const total = ref(0);

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
  appCode: undefined,
  appName: undefined,
  domain: undefined,
  authType: undefined,
  appIcon: undefined,
  loginBgp: undefined,
});

function init() {
  getList();
}

/** 查询参数列表 */
function getList() {
  loading.value = true;
  appPage(queryParams.value).then(res => {
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
  proxy.$refs["editRef"].init();
}
function handleUpdate(row) {
  proxy.$refs["editRef"].init(row);
}

/** 删除按钮操作 */
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除 :"' + row.id + '"？').then(() => {
    appRemove({id: row.id}).then(res => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
  }).catch(() => {});
}

init();
</script>

