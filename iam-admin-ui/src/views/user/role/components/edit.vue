<template>
  <el-dialog :title="title" v-model="open" width="600px">
    <el-form ref="editRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="父角色" prop="parentCode">
        <div class="parent-breadcrumb">
          <el-breadcrumb separator-class="el-icon-arrow-right" class="role-breadcrumb">
            <el-breadcrumb-item v-for="(item, index) in parents" :key="index" class="role-breadcrumb-item">
              <span>{{ item.roleName }}</span>
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="应用编码" prop="appCode">
            <el-input v-model="form.appCode" placeholder="请输入应用编码" disabled/>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="是否可申请" prop="applicable">
            <el-radio-group v-model="form.applicable">
              <el-radio-button v-for="item in BOOLEAN" :key="item.value" :label="item.label" :value="item.value" />
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="角色名称" prop="roleName">
        <el-input v-model="form.roleName" placeholder="请输入角色名称" />
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="form.sort" placeholder="请输入排序" />
        <form-tip text="数值越小越靠前"/>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" placeholder="请输入备注" type="textarea" :rows="4"/>
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

<script setup name="IamRoleEdit">
import {roleInfo, roleCreate, roleUpdate} from "@/api/user/role";

defineExpose({init});
const emit = defineEmits(["change"]);
const { proxy } = getCurrentInstance();
const { BOOLEAN } = proxy.useDict("BOOLEAN");
const open = ref(false);
const title = ref("");
const form = ref({});
const parents = ref([]);
const rules = ref({
  appCode: [
    { required: true, message: "应用编码不能为空", trigger: "blur" },
  ],
  parentCode: [
    { required: true, message: "父角色不能为空", trigger: "blur" }
  ],
  roleName: [
    { required: true, message: "角色名称不能为空", trigger: "blur" },
    { min: 1, max: 50, message: "角色名称长度应在 1-50 个字符之间", trigger: "blur" }
  ],
  applicable: [
    { required: true, message: "是否可申请不能为空", trigger: "blur" }
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
  parents.value = [];
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
  parents.value = row.parents;
  if (!row || !row.id) {
    open.value = true;
    title.value = "添加角色";
    form.value.appCode = row?.appCode;
    form.value.sort = 99;
    form.value.applicable = 1;
    form.value.parentCode = row.parentCode;
  } else {
    roleInfo({id: row.id}).then(res => {
      form.value = res.data;
      open.value = true;
      title.value = "修改角色";
    });
  }
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["editRef"].validate(valid => {
    if (valid) {
      if (form.value.id) {
        roleUpdate(form.value).then(res => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          emit("change", true);
        });
      } else {
        roleCreate(form.value).then(res => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          emit("change", true);
        });
      }
    }
  });
}
</script>

<style scoped lang="scss">
.role-breadcrumb {
  font-size: 14px;
  padding: 8px 16px;
  background-color: #f9fafc;
  border-radius: 6px;
  border: 1px solid #e9ecef;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  transition: all 0.3s ease;
}

.role-breadcrumb-item {
  color: #495057;
  font-weight: 500;
  display: flex;
  align-items: center;
}
</style>
