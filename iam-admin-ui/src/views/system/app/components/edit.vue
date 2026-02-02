<template>
  <el-dialog :title="title" v-model="open" width="1080px">
    <el-form ref="editRef" :model="form" :rules="rules" label-width="80px">

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="应用编码" prop="appCode">
            <el-input v-model="form.appCode" placeholder="请输入应用编码" />
            <form-tip text="全局唯一, 英文或数字,中横线或下划线"/>
          </el-form-item>
          <el-form-item label="应用名称" prop="appName">
            <el-input v-model="form.appName" placeholder="请输入应用名称" />
            <form-tip text="应用的显示名称，用于前端展示，长度2-30个字符"/>
          </el-form-item>
          <el-form-item label="应用域名" prop="domain">
            <el-input v-model="form.domain" placeholder="请输入应用域名" />
            <form-tip html="应用的访问域名，支持HTTP/HTTPS协议<br>例如：https://example.com"/>
          </el-form-item>
          <el-form-item label="鉴权类型" prop="authType">
            <el-radio-group v-model="form.authType" placeholder="鉴权类型">
              <el-radio-button v-for="item in AUTH_TYPE" :key="item.value" :label="item.label" :value="item.value" />
            </el-radio-group>
            <form-tip html="1. TOKEN: 用户登录后可访问此应用的所有功能<br />2. TOKEN 和接口: 用户需要登录并拥有接口权限才能访问对应接口"/>
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="form.remark" placeholder="请输入备注" />
          </el-form-item>
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="form.sort" type="number" placeholder="请输入排序" />
            <form-tip text="数值越小越靠前"/>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="图标" prop="appIcon">
            <el-input v-model="form.appIcon" placeholder="请输入图标" />
            <form-tip html="应用的图标路径，支持相对路径或完整URL<br>例如：/assets/icon.png 或 https://example.com/icon.png"/>
          </el-form-item>
          <el-form-item label="登录背景" prop="loginBgp">
            <el-input v-model="form.loginBgp" placeholder="请输入登录页背景" />
            <form-tip html="应用登录页面的背景图片路径，支持相对路径或完整URL<br>例如：/assets/bg.jpg 或 https://example.com/bg.jpg"/>
          </el-form-item>
        </el-col>
      </el-row>

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
import FormTip from "@/components/FormTip/index.vue";

defineExpose({init});
const emit = defineEmits(["change"]);
const { proxy } = getCurrentInstance();
const { AUTH_TYPE } = proxy.useDict("AUTH_TYPE");
const open = ref(false);
const title = ref("");
const form = ref({});
const rules = ref({
  appCode: [
    { required: true, message: "应用编码不能为空", trigger: "blur" },
    { min: 2, max: 30, message: "应用编码长度应在 2-30 个字符之间", trigger: "blur" },
    { pattern: /^[a-zA-Z0-9_-]+$/, message: "应用编码只能包含字母、数字、下划线和连字符", trigger: "blur" }
  ],
  appName: [
    { required: true, message: "应用名称不能为空", trigger: "blur" },
    { min: 2, max: 30, message: "应用名称长度应在 2-30 个字符之间", trigger: "blur" }
  ],
  domain: [
    { required: true, message: "应用域名不能为空", trigger: "blur" },
    { min: 3, max: 50, message: "应用域名长度应在 3-50 个字符之间", trigger: "blur" },
    { pattern: /^(https?:\/\/)?([\da-z.-]+)\.([a-z.]{2,6})([/\w .-]*)*\/?$/, message: "应用域名格式不正确", trigger: "blur" }
  ],
  authType: [
    { required: true, message: "鉴权类型不能为空", trigger: "blur" }
  ],
  sort: [
    { required: true, message: "排序不能为空", trigger: "blur" }
  ],
  remark: [
    { max: 200, message: "备注长度不能超过 200 个字符", trigger: "blur" }
  ]
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
    form.value.authType = 'TOKEN_API';
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

