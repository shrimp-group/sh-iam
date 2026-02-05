<template>
  <el-dialog :title="title" v-model="open" width="1080px">
    <el-form ref="editRef" :model="form" :rules="rules" label-width="80px">

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="用户编码" prop="userCode">
            <el-input v-model="form.userCode" placeholder="请输入用户编码" />
            <form-tip text="全局唯一, 英文或数字,中横线或下划线"/>
          </el-form-item>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" />
            <form-tip text="用户登录名，长度3-20个字符"/>
          </el-form-item>
          <el-form-item label="姓名" prop="nickname">
            <el-input v-model="form.nickname" placeholder="请输入姓名" />
            <form-tip text="用户的姓名，用于前端展示"/>
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="请输入邮箱" />
            <form-tip text="用户的邮箱地址，一般为公司邮箱"/>
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" />
            <form-tip text="用户的手机号码，用于找回密码等"/>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="头像" prop="avatar">
            <el-input v-model="form.avatar" placeholder="请输入头像" />
            <form-tip html="用户的头像图片路径，支持相对路径或完整URL<br>例如：/assets/avatar.png 或 https://example.com/avatar.png"/>
          </el-form-item>
          <el-form-item label="启用状态" prop="userStatus">
            <el-radio-group v-model="form.userStatus">
              <el-radio-button v-for="item in BOOLEAN" :key="item.value" :label="item.label" :value="item.value" />
            </el-radio-group>
            <form-tip html="是：正常使用；否：此用户不可再使用；"/>
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

<script setup name="IamUserEdit">
import {userInfo, userCreate, userUpdate} from "@/api/user/user";
import FormTip from "@/components/FormTip/index.vue";

defineExpose({init});
const emit = defineEmits(["change"]);
const { proxy } = getCurrentInstance();
const { BOOLEAN } = proxy.useDict("BOOLEAN");
const open = ref(false);
const title = ref("");
const form = ref({});
const rules = ref({
  username: [
    { required: true, message: "用户名不能为空", trigger: "blur" },
    { min: 3, max: 20, message: "用户名长度应在 3-20 个字符之间", trigger: "blur" }
  ],
  nickname: [
    { required: true, message: "昵称不能为空", trigger: "blur" },
    { min: 1, max: 30, message: "昵称长度应在 1-30 个字符之间", trigger: "blur" }
  ],
  email: [
    { required: true, message: "邮箱不能为空", trigger: "blur" },
    { type: "email", message: "邮箱格式不正确", trigger: "blur" }
  ],
  phone: [
    { required: true, message: "手机号不能为空", trigger: "blur" },
    { pattern: /^1[3-9]\d{9}$/, message: "手机号格式不正确", trigger: "blur" }
  ],
  avatar: [
    { required: false, message: "头像不能为空", trigger: "blur" }
  ],
  userStatus: [
    { required: true, message: "用户状态不能为空", trigger: "blur" }
  ],
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
    form.value.userStatus = 1;
  } else {
    userInfo({id: row.id}).then(res => {
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
        userUpdate(form.value).then(res => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          emit("change", true);
        });
      } else {
        userCreate(form.value).then(res => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          emit("change", true);
        });
      }
    }
  });
}
</script>

