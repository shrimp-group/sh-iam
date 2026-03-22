<template>
  <div class="app-container">
    <layout-split>
      <template #left>
        <app-options @select="selectApp"/>
      </template>
      <template #right>
        <el-form :model="queryParams" ref="queryRef" :inline="true" @submit.native.prevent>
          <el-form-item prop="appCode">
            <el-input v-model="queryParams.appCode" placeholder="所属应用" disabled style="width: 120px"/>
          </el-form-item>
          <el-form-item prop="appId">
            <el-input v-model="queryParams.appId" placeholder="应用id" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
          </el-form-item>
          <el-form-item prop="enableStatus">
            <el-select v-model="queryParams.enableStatus" placeholder="生效状态" clearable filterable style="width: 120px" @change="handleQuery">
              <el-option v-for="item in BOOLEAN" :key="item.value" :label="item.label" :value="item.value" />
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
          <el-table-column label="所属应用" prop="appCode" width="120"/>
          <el-table-column label="应用id" prop="appId" width="200"/>
          <el-table-column label="AK" prop="accessKey" min-width="160"/>
          <el-table-column label="生效状态" prop="enableStatus" width="130">
            <template #default="{row}"><dict-tag :options="BOOLEAN" :value="row.enableStatus" /></template>
          </el-table-column>
          <el-table-column label="生效时间开始" prop="enableStart" width="160">
            <template #default="{row}">{{ parseTime(row.enableStart) }}</template>
          </el-table-column>
          <el-table-column label="生效时间结束" prop="enableStop" width="160">
            <template #default="{row}">{{ parseTime(row.enableStop) }}</template>
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
          <el-table-column fixed="right" width="180">
            <template #header><table-setting v-model:columns="columns"/></template>
            <template #default="{row}">
              <el-button link type="primary" icon="Edit" @click="handleUpdate(row)" title="编辑"/>
              <el-button link type="success" icon="Link" @click="handleAkApiRef(row)" title="API授权">API授权</el-button>
              <el-popconfirm :title="'确认删除:' + row.appId + '?'" placement="top-end" @confirm="handleDelete(row)">
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
      </template>
    </layout-split>
    <edit ref="editRef" @change="getList"/>
    <ak-api ref="akApiRef" />
  </div>
</template>

<script setup name="IamAccessKey">
import { accesskeyPage, accesskeyRemove} from "@/api/system/ak";
import Edit from "./components/edit"
import AkApi from "./components/ak-api"
import AppOptions from "@/views/components/AppOptions/index.vue";

const { proxy } = getCurrentInstance();

const { BOOLEAN } = proxy.useDict("BOOLEAN");
const dataList = ref([]);
const loading = ref(false);
const total = ref(0);

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
  appId: undefined,
  accessKey: undefined,
  secretKey: undefined,
  enableStatus: undefined,
  enableStart: undefined,
  enableStop: undefined,
});

function selectApp(row) {
  queryParams.value.appCode = row.appCode;
  handleQuery();
}

/** 查询参数列表 */
function getList() {
  loading.value = true;
  accesskeyPage(queryParams.value).then(res => {
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

/** API授权 */
function handleAkApiRef(row) {
  proxy.$refs["akApiRef"].init(row);
}

/** 删除按钮操作 */
function handleDelete(row) {
  accesskeyRemove({id: row.id}).then(res => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  })
}

</script>
