<template>
  <el-dialog :title="title" v-model="open" width="1080px">
    <el-form ref="editRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="应用编码" prop="appCode">
        <el-input v-model="form.appCode" placeholder="请输入应用编码" />
      </el-form-item>
      <el-form-item label="应用名称" prop="appName">
        <el-input v-model="form.appName" placeholder="请输入应用名称" />
      </el-form-item>
      <el-form-item label="应用域名" prop="domain">
        <el-input v-model="form.domain" placeholder="请输入应用域名" />
      </el-form-item>
      <el-form-item label="鉴权类型" prop="authType">
        <el-input v-model="form.authType" placeholder="请输入鉴权类型" />
      </el-form-item>
      <el-form-item label="图标" prop="appIcon">
        <el-input v-model="form.appIcon" placeholder="请输入图标" />
      </el-form-item>
      <el-form-item label="登录页背景" prop="loginBgp">
        <el-input v-model="form.loginBgp" placeholder="请输入登录页背景" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input v-model="form.sort" type="number" placeholder="请输入排序" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" placeholder="请输入备注" />
      </el-form-item>
      <el-form-item label="是否" prop="userDefine">
        <el-select v-model="form.userDefine" placeholder="是否" clearable filterable style="width: 120px">
          <el-option v-for="item in BOOLEAN" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="IamAppEdit">
import {appInfo, appCreate, appUpdate} from "@/api/system/app";

defineExpose({init});
const emit = defineEmits(["change"]);
const { proxy } = getCurrentInstance();
const { BOOLEAN } = proxy.useDict("BOOLEAN");
const open = ref(false);
const title = ref("");
const form = ref({});
const rules = ref({
  appCode: [{ required: true, message: "应用编码 不能为空", trigger: "blur"}],
  appName: [{ required: true, message: "应用名称 不能为空", trigger: "blur"}],
  domain: [{ required: true, message: "应用域名 不能为空", trigger: "blur"}],
  authType: [{ required: true, message: "鉴权类型 不能为空", trigger: "blur"}],
  appIcon: [{ required: true, message: "图标 不能为空", trigger: "blur"}],
  loginBgp: [{ required: true, message: "登录页背景 不能为空", trigger: "blur"}],
  sort: [{ required: true, message: "排序 不能为空", trigger: "blur"}],
  remark: [{ required: true, message: "备注 不能为空", trigger: "blur"}],
})

/** 表单重置 */
function reset() {
  form.value = {};
  proxy.resetForm("editRef");
}

/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
}

// 新增/修改按钮操作
function init(row) {
  reset();
  if (!row || !row.id) {
    open.value = true;
    title.value = "添加";
    form.value.sort = 99;
  } else {
    appInfo({id: row.id}).then(res => {
      form.value = res.data;
      open.value = true;
      title.value = "修改";
    });
  }
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["editRef"].validate(valid => {
    if (valid) {
      if (form.value.id) {
        appUpdate(form.value).then(res => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          emit("change", true);
        });
      } else {
        appCreate(form.value).then(res => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          emit("change", true);
        });
      }
    }
  });
}
</script>

