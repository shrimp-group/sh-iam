<template>
  <el-dialog :title="title" v-model="open" width="800px">
    <el-form ref="editRef" :model="form" :rules="rules" label-width="80px">

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="所属应用" prop="appCode">
            <el-input v-model="form.appCode" disabled placeholder="请输入所属应用" />
          </el-form-item>
          <el-form-item label="生效时间" prop="enableStart">
            <el-date-picker v-model="form.enableStart" type="datetime" placeholder="请选择 生效时间" />
          </el-form-item>
          <el-form-item label="失效时间" prop="enableStop">
            <el-date-picker v-model="form.enableStop" type="datetime" placeholder="请选择 失效时间" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="备注" prop="remark">
            <el-input v-model="form.remark" placeholder="请输入备注" />
          </el-form-item>
          <el-form-item label="排序" prop="sort">
            <el-input v-model="form.sort" type="number" placeholder="请输入排序" />
          </el-form-item>
          <el-form-item label="生效状态" prop="enableStatus">
            <el-radio-group v-model="form.enableStatus">
              <el-radio-button v-for="item in BOOLEAN" :key="item.value" :label="item.label" :value="item.value" />
            </el-radio-group>
          </el-form-item>

        </el-col>
      </el-row>

      <el-divider content-position="left" content="密钥信息"/>

      <el-form-item label="应用id" prop="appId">
        <el-input v-model="form.appId" placeholder="请输入应用id" />
      </el-form-item>
      <el-form-item label="AK" prop="accessKey">
        <el-input v-model="form.accessKey" placeholder="请输入AK" />
      </el-form-item>
      <el-form-item label="SK" prop="secretKey">
        <el-input v-model="form.secretKey" placeholder="请输入SK" />
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

<script setup name="IamAccessKeyEdit">
import {accesskeyInfo, accesskeyCreate, accesskeyUpdate} from "@/api/system/ak";

defineExpose({init});
const emit = defineEmits(["change"]);
const { proxy } = getCurrentInstance();
const { BOOLEAN } = proxy.useDict("BOOLEAN");
const open = ref(false);
const title = ref("");
const form = ref({});
const rules = ref({
  appCode: [{ required: true, message: "所属应用 不能为空", trigger: "blur"}],
  enableStatus: [{ required: true, message: "生效状态 不能为空", trigger: "blur"}],
  enableStart: [{ required: true, message: "生效时间开始 不能为空", trigger: "blur"}],
  enableStop: [{ required: true, message: "生效时间结束 不能为空", trigger: "blur"}],
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
    form.value.appCode = row?.appCode;
    form.value.enableStatus = 1;
    form.value.sort = 99;
  } else {
    accesskeyInfo({id: row.id}).then(res => {
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
        accesskeyUpdate(form.value).then(res => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          emit("change", true);
        });
      } else {
        accesskeyCreate(form.value).then(res => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          emit("change", true);
        });
      }
    }
  });
}
</script>

