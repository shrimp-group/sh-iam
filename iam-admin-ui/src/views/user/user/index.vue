<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" @submit.native.prevent label-width="1px;">
      <el-form-item prop="userCode">
        <el-input v-model="queryParams.userCode" placeholder="用户编码,模糊搜索" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item prop="username">
        <el-input v-model="queryParams.username" placeholder="用户名,模糊搜索" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item prop="nickname">
        <el-input v-model="queryParams.nickname" placeholder="姓名,模糊搜索" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item prop="phone">
        <el-input v-model="queryParams.phone" placeholder="手机号,模糊搜索" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item prop="userStatus">
        <el-select v-model="queryParams.userStatus" placeholder="启用状态" clearable filterable style="width: 160px" @change="handleQuery">
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
      <el-table-column label="用户编码" prop="userCode" min-width="120"/>
      <el-table-column label="用户名" prop="username" min-width="120"/>
      <el-table-column label="姓名" prop="nickname" min-width="120"/>
      <el-table-column label="邮箱" prop="email" min-width="120"/>
      <el-table-column label="手机号" prop="phone" min-width="120"/>
      <el-table-column label="头像" prop="avatar" min-width="120"/>
      <el-table-column label="启用状态" prop="userStatus" width="130">
        <template #default="{row}"><dict-tag :options="BOOLEAN" :value="row.userStatus" /></template>
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
    <edit ref="editRef" @change="getList"/>
  </div>
</template>

<script setup name="IamUser">
import { userPage, userRemove} from "@/api/user/user";
import Edit from "./components/edit"

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
  userCode: undefined,
  username: undefined,
  nickname: undefined,
  email: undefined,
  phone: undefined,
});

function init() {
  getList();
}

/** 查询参数列表 */
function getList() {
  loading.value = true;
  userPage(queryParams.value).then(res => {
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
  proxy.$modal.confirm('是否确认删除用户 :"' + row.username + '"？').then(() => {
    userRemove({id: row.id}).then(res => {
      proxy.$modal.msgSuccess("删除成功");
      getList();
    })
  }).catch(() => {});
}

init();
</script>

