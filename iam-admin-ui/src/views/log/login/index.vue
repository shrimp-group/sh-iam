<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" @submit.native.prevent>
      <el-form-item prop="authIdentifier">
        <el-input v-model="queryParams.authIdentifier" placeholder="认证标识" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item prop="userCode">
        <el-input v-model="queryParams.userCode" placeholder="用户编码" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item prop="username">
        <el-input v-model="queryParams.username" placeholder="登录用户名" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item prop="loginStatus">
        <el-select v-model="queryParams.loginStatus" placeholder="登录状态" clearable filterable style="width: 120px" @change="handleQuery">
          <el-option v-for="item in LOGIN_STATUS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item prop="ipAddress">
        <el-input v-model="queryParams.ipAddress" placeholder="登录IP地址" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-date-picker v-model="dateRange" style='width: 380px'
                        value-format="YYYY-MM-DD HH:mm:ss"
                        :shortcuts="timeRangeShortcuts"
                        type="datetimerange" range-separator="-" start-placeholder="开始时间" end-placeholder="结束时间"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="ID" prop="id" width="80" v-if="columns.id.visible"/>
      <el-table-column label="认证标识" prop="authIdentifier" width="120"/>
      <el-table-column label="用户编码" prop="userCode" width="120"/>
      <el-table-column label="登录用户名" prop="username" width="120"/>
      <el-table-column label="登录类型" prop="authType" width="120"/>
      <el-table-column label="登录状态" prop="loginStatus" width="130">
        <template #default="{row}"><dict-tag :options="LOGIN_STATUS" :value="row.loginStatus" /></template>
      </el-table-column>
      <el-table-column label="登录结果消息" prop="message" width="160"/>
      <el-table-column label="登录IP地址" prop="ipAddress" width="160"/>
      <el-table-column label="用户代理信息" prop="userAgent" min-width="160"/>
      <el-table-column label="创建时间" prop="createTime" width="160" v-if="columns.createTime.visible">
        <template #default="{row}">{{ parseTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="创建人" prop="createBy" width="120" v-if="columns.createBy.visible"/>
    </el-table>

    <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.current"
        v-model:limit="queryParams.size"
        @pagination="getList"
    />
  </div>
</template>

<script setup name="IamLoginLog">
import {loginLogPage} from "@/api/log/loginlog";
import {timeRangeShortcuts} from "@/utils/shrimp";

const { proxy } = getCurrentInstance();

const { BOOLEAN, AUTH_TYPE, LOGIN_STATUS } = proxy.useDict("BOOLEAN", "AUTH_TYPE", "LOGIN_STATUS");
const dataList = ref([]);
const loading = ref(false);
const total = ref(0);
const dateRange = ref([]);

const columns = ref({
  id: {label: "ID", visible: false},
  sort: {label: "排序", visible: true},
  remark: {label: "备注", visible: true},
  updateBy: {label: "修改人", visible: false},
  updateTime: {label: "修改时间", visible: false},
  createBy: {label: "创建人", visible: false},
  createTime: {label: "创建时间", visible: true},
})

const queryParams = ref({
  current: 1,
  size: 20,
  authIdentifier: undefined,
  userCode: undefined,
  username: undefined,
  authType: undefined,
  message: undefined,
  ipAddress: undefined,
});

function init() {
  const now = new Date();
  dateRange.value = [
    proxy.parseTime(now.setDate(now.getDate()-7), '{y}-{m}-{d}') + 'T00:00:00',
    proxy.parseTime(new Date(), '{y}-{m}-{d}') + 'T23:59:59'
  ];
  getList();
}

/** 查询参数列表 */
function getList() {
  loading.value = true;
  loginLogPage(proxy.addDateRange(queryParams.value, dateRange.value)).then(res => {
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

init();
</script>

