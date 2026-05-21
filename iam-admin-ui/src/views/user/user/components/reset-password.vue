<template>
  <el-dialog title="重置密码" v-model="open" width="500px">
    <el-form ref="resetPwdRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="用户名">
        <el-input v-model="form.username" disabled />
      </el-form-item>
      <el-form-item label="新密码" prop="password">
        <el-input v-model="form.password" placeholder="请输入新密码" type="password" show-password />
        <form-tip text="新密码长度8-20个字符，不能与最近3次使用过的密码相同"/>
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" placeholder="请确认新密码" type="password" show-password />
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

<script setup name="IamUserResetPassword">
import { userAuthResetPassword } from "@/api/user/user";

defineExpose({init});
const emit = defineEmits(["change"]);
const { proxy } = getCurrentInstance();
const open = ref(false);
const form = ref({});

const equalToPassword = (rule, value, callback) => {
  if (form.value.password !== value) {
    callback(new Error("两次输入的密码不一致"));
  } else {
    callback();
  }
};

const rules = ref({
  password: [
    { required: true, message: "新密码不能为空", trigger: "blur" },
    { min: 8, max: 20, message: "密码长度应在 8-20 个字符之间", trigger: "blur" }
  ],
  confirmPassword: [
    { required: true, message: "确认密码不能为空", trigger: "blur" },
    { validator: equalToPassword, trigger: "blur" }
  ]
});

function reset() {
  form.value = {};
  proxy.resetForm("resetPwdRef");
}

function cancel() {
  open.value = false;
  reset();
}

function init(row) {
  reset();
  form.value.userCode = row.userCode;
  form.value.username = row.username;
  open.value = true;
}

function submitForm() {
  proxy.$refs["resetPwdRef"].validate(valid => {
    if (valid) {
      userAuthResetPassword({
        userCode: form.value.userCode,
        password: form.value.password
      }).then(() => {
        proxy.$modal.msgSuccess("密码重置成功");
        open.value = false;
        emit("change", true);
      });
    }
  });
}
</script>
