<template>
  <el-dialog :title="title" v-model="open" width="1080px">
    <el-form ref="editRef" :model="form" :rules="rules" label-width="80px">

      <el-row :gutter="20">
        <el-col :span="14">
          <el-form-item label="父菜单" prop="parentCode">
            <div class="parent-breadcrumb">
              <el-breadcrumb separator-class="el-icon-arrow-right" class="role-breadcrumb">
                <el-breadcrumb-item v-for="(item, index) in parents" :key="index" class="role-breadcrumb-item">
                  <span>{{ item.roleName }}</span>
                </el-breadcrumb-item>
              </el-breadcrumb>
            </div>
          </el-form-item>
          <el-form-item label="应用编码" prop="appCode">
            <el-input v-model="form.appCode" disabled />
          </el-form-item>
          <el-form-item label="角色名称" prop="roleName">
            <el-input v-model="form.roleName" placeholder="请输入角色名称" />
          </el-form-item>

        </el-col>
        <el-col :span="10">
          <el-form-item label="排序" prop="sort">
            <el-input v-model="form.sort" type="number" placeholder="请输入排序" />
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" :rows="4"/>
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

<script setup name="IamUserRoleEdit">
import {roleInfo, roleCreate, roleUpdate} from "@/api/user/role";
import {ref} from "vue";

defineExpose({init});
const emit = defineEmits(["change"]);
const { proxy } = getCurrentInstance();
const open = ref(false);
const title = ref("");
const form = ref({});
const parents = ref([]);

const rules = ref({
  appCode: [{ required: true, message: "应用编码 不能为空", trigger: "blur"}],
  roleName: [{ required: true, message: "名称编码 不能为空", trigger: "blur"}],
  sort: [{ required: true, message: "排序 不能为空", trigger: "blur"}],
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
    title.value = "添加";
    form.value.sort = 99;
    form.value.appCode = row?.appCode;
    form.value.parentCode = row.parentCode;
  } else {
    roleInfo({id: row.id}).then(res => {
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

