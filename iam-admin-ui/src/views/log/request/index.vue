<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" @submit.native.prevent>
      <el-form-item prop="appCode">
        <el-input v-model="queryParams.appCode" placeholder="应用编码" clearable style="width: 120px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item prop="requestUri">
        <el-input v-model="queryParams.requestUri" placeholder="URI,支持后模糊" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item prop="httpStatus">
        <el-input v-model="queryParams.httpStatus" placeholder="响应状态" clearable style="width: 120px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item prop="remoteAddr">
        <el-input v-model="queryParams.remoteAddr" placeholder="客户端地址" clearable style="width: 120px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item prop="userCode">
        <el-input v-model="queryParams.userCode" placeholder="用户编码" clearable style="width: 120px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item prop="username">
        <el-input v-model="queryParams.username" placeholder="用户名" clearable style="width: 120px" @keyup.enter.native="handleQuery" />
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
      <el-table-column label="租户编码" prop="tenantCode" min-width="100"/>
      <el-table-column label="应用编码" prop="appCode" min-width="120"/>
      <el-table-column label="用户编码" prop="userCode" min-width="100"/>
      <el-table-column label="用户名" prop="username" min-width="100"/>
      <el-table-column label="用户昵称" prop="nickname" min-width="100"/>
      <el-table-column label="用户IP" prop="remoteAddr" min-width="120"/>
      <el-table-column label="请求方式" prop="method" min-width="80"/>
      <el-table-column label="请求URI" prop="requestUri" min-width="200"/>
      <el-table-column label="查询内容" prop="queryString" min-width="120"/>
      <el-table-column label="响应状态" prop="httpStatus" min-width="80"/>
      <el-table-column label="耗时/ms" prop="costTime" min-width="76"/>
      <el-table-column label="请求时间" prop="createTime" width="160">
        <template #default="{row}">{{ parseTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="异常信息" prop="errorMsg" min-width="120"/>
      <el-table-column label="UA" prop="userAgent" min-width="280"/>
      <el-table-column label="浏览器名称" prop="browserName" min-width="120"/>
      <el-table-column label="浏览器版本" prop="browserVersion" min-width="120"/>
      <el-table-column label="引擎类型" prop="engineName" min-width="100"/>
      <el-table-column label="引擎版本" prop="engineVersion" min-width="100"/>
      <el-table-column label="用户系统" prop="userOs" min-width="100"/>
      <el-table-column label="用户平台" prop="userPlatform" min-width="100"/>
      <el-table-column label="请求编码" prop="characterEncoding" min-width="100"/>
      <el-table-column label="Accept" prop="accept" min-width="220"/>
      <el-table-column label="Accept-语言" prop="acceptLanguage" min-width="150"/>
      <el-table-column label="Accept-编码" prop="acceptEncoding" min-width="160"/>
      <el-table-column label="Cookie" prop="cookie" min-width="120"/>
      <el-table-column label="Origin" prop="origin" min-width="200"/>
      <el-table-column label="引用页" prop="referer" min-width="200"/>
      <el-table-column label="请求协议" prop="httpProtocol" min-width="100"/>
      <el-table-column label="地区" prop="location" min-width="120"/>
      <el-table-column label="ISP运营商" prop="isp" min-width="100"/>
      <el-table-column label="操作" fixed="right" width="56">
        <template #default="{row}">
          <el-button link type="primary" icon="View" @click="handleDetail(row)" title="详情"/>
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
    <detail ref="detailRef"/>
  </div>
</template>

<script setup name="IamRequestLog">
import { requestLogPage } from "@/api/log/requestlog";
import {timeRangeShortcuts} from "@/utils/shrimp.js";
import Detail from "./components/detail";

const { proxy } = getCurrentInstance();

const { BOOLEAN } = proxy.useDict("BOOLEAN");
const dataList = ref([]);
const loading = ref(false);
const total = ref(0);
const dateRange = ref([]);

const queryParams = ref({
  current: 1,
  size: 20,
  tenantCode: undefined,
  appCode: undefined,
  remoteAddr: undefined,
  requestUri: undefined,
  queryString: undefined,
  httpStatus: undefined,
  userCode: undefined,
  username: undefined
});

function init() {
  const now = new Date();
  dateRange.value = [
    proxy.parseTime(now.setMinutes(now.getMinutes()-30), '{y}-{m}-{d}T{h}:{i}:{s}'),
    proxy.parseTime(new Date(), '{y}-{m}-{d}') + 'T23:59:59'
  ];
  getList();
}

/** 查询参数列表 */
function getList() {
  loading.value = true;
  requestLogPage(proxy.addDateRange(queryParams.value, dateRange.value)).then(res => {
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

function handleDetail(row) {
  proxy.$refs["detailRef"].init(row);
}

init();
</script>

