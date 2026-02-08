<template>
  <el-dialog :title="title" v-model="open" width="1080px">
    <el-form ref="editRef" :model="form" :rules="rules" label-width="96px">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="应用编码" prop="appCode">
            <el-input v-model="form.appCode" disabled />
            <form-tip text="当前应用的编码，自动填充"/>
          </el-form-item>
          <el-form-item label="编码" prop="dimensionCode">
            <el-input v-model="form.dimensionCode" placeholder="请输入元数据编码" />
            <form-tip text="元数据的唯一标识，英文或数字，中横线或下划线"/>
          </el-form-item>
          <el-form-item label="名称" prop="dimensionName">
            <el-input v-model="form.dimensionName" placeholder="请输入元数据名称" />
            <form-tip text="元数据的显示名称，用于前端展示"/>
          </el-form-item>
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="form.sort" placeholder="请输入排序" />
            <form-tip text="数值越小越靠前"/>
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="form.remark" placeholder="请输入备注" type="textarea" :rows="6"/>
            <form-tip text="元数据的描述信息"/>
          </el-form-item>
        </el-col>
        <el-col :span="16">
          <el-form-item label="元数据数组" prop="dimensionDataJson">
            <monaco-editor v-model="form.dimensionDataJson" language="json" height="200px"/>
            <form-tip html='元数据的具体内容，是一个包含 label 和 value 字段的 JSON 数组<br>例如：[{"label": "选项1", "value": "1"}, {"label": "选项2", "value": "2"}]'/>
          </el-form-item>
          <el-form-item label="元数据脚本" prop="dimensionScript">
            <monaco-editor v-model="form.dimensionScript" language="sql" height="200px"/>
            <form-tip text="当元数据数组为空时，会执行此脚本获取元数据（优先级低于元数据数组）"/>
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

<script setup name="IamDataDimensionEdit">
import {datadimInfo, datadimCreate, datadimUpdate} from "@/api/system/dim";
import MonacoEditor from "@/components/MonacoEditor/index.vue";

defineExpose({init});
const emit = defineEmits(["change"]);
const { proxy } = getCurrentInstance();
const open = ref(false);
const title = ref("");
const form = ref({});
const rules = ref({
  appCode: [{ required: true, message: "应用编码 不能为空", trigger: "blur"}],
  dimensionCode: [{ required: true, message: "元数据编码 不能为空", trigger: "blur"}],
  dimensionName: [{ required: true, message: "元数据名称 不能为空", trigger: "blur"}],
  dimensionScript: [{ required: false, message: "元数据脚本(优先级低) 不能为空", trigger: "blur"}],
  dimensionDataJson: [{ required: false, message: "元数据数组(优先于脚本) 不能为空", trigger: "blur"}],
  sort: [{ required: true, message: "排序 不能为空", trigger: "blur"}],
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
    form.value.appCode = row?.appCode;
  } else {
    datadimInfo({id: row.id}).then(res => {
      const data = res.data;
      open.value = true;
      title.value = "修改";
      if (data.dimensionDataJson) {
        try {
          // JSON 格式化
          data.dimensionDataJson = JSON.stringify(JSON.parse(data.dimensionDataJson), null, 4);
        } catch (e) {
          // who care ?
        }
      }
      form.value = data;
    });
  }
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["editRef"].validate(valid => {
    if (valid) {
      if (form.value.id) {
        datadimUpdate(form.value).then(res => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          emit("change", true);
        });
      } else {
        datadimCreate(form.value).then(res => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          emit("change", true);
        });
      }
    }
  });
}
</script>

