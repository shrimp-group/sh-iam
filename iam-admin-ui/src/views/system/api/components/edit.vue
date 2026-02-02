<template>
  <el-dialog :title="title" v-model="open" width="1080px">
    <el-form ref="editRef" :model="form" :rules="rules" label-width="80px">
      <el-row :gutter="20">
        <el-col :span="14">
          <el-form-item label="模块" prop="module">
            <el-input v-model="form.module" placeholder="请输入模块" />
            <form-tip text="API所属的模块，用于分类管理"/>
          </el-form-item>
          <el-form-item label="接口名称" prop="apiName">
            <el-input v-model="form.apiName" placeholder="请输入 接口名称" />
            <form-tip text="API的显示名称，用于前端展示"/>
          </el-form-item>
          <el-form-item label="请求方法" prop="apiMethod">
            <el-radio-group v-model="form.apiMethod" placeholder="请求方法">
              <el-radio-button v-for="item in API_METHOD" :key="item.value" :label="item.label" :value="item.value" />
            </el-radio-group>
            <form-tip html="REQUEST 可匹配任意请求方法"/>
          </el-form-item>
          <el-form-item label="URI" prop="apiUri">
            <el-input v-model="form.apiUri" placeholder="请输入路由映射URI" />
            <form-tip html="API的访问路径，例如：/api/user/list<br>以斜杠开头, 支持 AntPathMatcher 匹配机制"/>
          </el-form-item>
        </el-col>
        <el-col :span="10">
          <el-form-item label="应用编码" prop="appCode">
            <el-input v-model="form.appCode" placeholder="请输入应用编码" />
            <form-tip text="关联的应用编码，用于权限控制"/>
          </el-form-item>
          <el-form-item label="白名单" prop="writeFlag">
            <el-radio-group v-model="form.writeFlag" placeholder="请求方法">
              <el-radio-button v-for="item in BOOLEAN" :key="item.value" :label="item.label" :value="item.value" />
            </el-radio-group>
            <form-tip html="白名单的接口，无需要登录也能被直接请求，请注意数据安全!"/>
          </el-form-item>
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="form.sort" placeholder="请输入排序" />
            <form-tip text="数值越小越靠前"/>
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="form.remark" placeholder="请输入备注" type="textarea" :rows="3"/>
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

<script setup name="IamApiEdit">
import {apiInfo, apiCreate, apiUpdate} from "@/api/system/api";

defineExpose({init});
const emit = defineEmits(["change"]);
const { proxy } = getCurrentInstance();
const { BOOLEAN, API_METHOD } = proxy.useDict("BOOLEAN", "API_METHOD");
const open = ref(false);
const title = ref("");
const form = ref({});
const rules = ref({
  module: [
    { required: true, message: "模块不能为空", trigger: "blur" },
    { min: 1, max: 20, message: "模块长度应在 1-20 个字符之间", trigger: "blur" }
  ],
  appCode: [
    { required: true, message: "应用编码不能为空", trigger: "blur" },
    { min: 2, max: 20, message: "应用编码长度应在 2-20 个字符之间", trigger: "blur" }
  ],
  apiCode: [
    { required: true, message: "路由映射编码不能为空", trigger: "blur" },
    { min: 2, max: 50, message: "路由映射编码长度应在 2-50 个字符之间", trigger: "blur" }
  ],
  apiMethod: [
    { required: true, message: "路由映射方法不能为空", trigger: "blur" },
  ],
  apiUri: [
    { required: true, message: "路由映射URI不能为空", trigger: "blur" },
    { pattern: /^\/.+/, message: "路由映射URI必须以斜杠开头", trigger: "blur" }
  ],
  apiName: [
    { required: true, message: "路由映射名称不能为空", trigger: "blur" },
    { min: 1, max: 50, message: "路由映射名称长度应在 1-50 个字符之间", trigger: "blur" }
  ],
  writeFlag: [
    { required: true, message: "白名单不能为空", trigger: "blur" },
  ],
  sort: [
    { required: true, message: "排序不能为空", trigger: "blur" }
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
    form.value.apiMethod = 'GET';
    form.value.sort = 99;
    form.value.writeFlag = 0;
  } else {
    apiInfo({id: row.id}).then(res => {
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
        apiUpdate(form.value).then(res => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          emit("change", true);
        });
      } else {
        apiCreate(form.value).then(res => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          emit("change", true);
        });
      }
    }
  });
}
</script>

