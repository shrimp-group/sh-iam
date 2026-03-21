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

      <!-- 创建成功后展示的密钥信息 -->
      <div v-if="showKeyInfo" class="key-info-section">
        <el-alert title="SecretKey 只会展示一次，请妥善保管，不可泄漏！若遗忘，只能重新生成。" type="warning" :closable="false" show-icon style="margin-bottom: 15px;"/>
        <el-form-item label="密钥信息">
          <div class="key-display-area">
            <el-input v-model="keyInfoText" type="textarea" :rows="6" readonly class="key-textarea"/>
            <div class="key-actions">
              <el-button type="success" icon="CopyDocument" @click="copyAk">复制</el-button>
            </div>
          </div>
        </el-form-item>
      </div>

    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button v-if="!showKeyInfo" type="primary" @click="submitForm">确 定</el-button>
        <el-button type="primary" @click="cancel">{{ showKeyInfo ? '完成' : '取消' }}</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="IamAccessKeyEdit">
import {accesskeyInfo, accesskeyCreate, accesskeyUpdate} from "@/api/system/ak";
import { copy } from "@/utils/shrimp";

defineExpose({init});
const emit = defineEmits(["change"]);
const { proxy } = getCurrentInstance();
const { BOOLEAN } = proxy.useDict("BOOLEAN");
const open = ref(false);
const title = ref("");
const form = ref({});
const showKeyInfo = ref(false);
const keyInfoText = ref('');

const rules = ref({
  appCode: [{ required: true, message: "所属应用 不能为空", trigger: "blur"}],
  enableStatus: [{ required: true, message: "生效状态 不能为空", trigger: "blur"}],
  enableStart: [{ required: true, message: "生效时间开始 不能为空", trigger: "blur"}],
  enableStop: [{ required: true, message: "生效时间结束 不能为空", trigger: "blur"}],
  sort: [{ required: true, message: "排序 不能为空", trigger: "blur"}],
})


/** 表单重置 */
function reset() {
  form.value = {};
  showKeyInfo.value = false;
  proxy.resetForm("editRef");
}

/** 完成/取消按钮 */
function cancel() {
  open.value = false;
  if (showKeyInfo.value) {
    emit("change", true);
  }
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
    form.value.enableStart = proxy.parseTime(new Date(), '{y}-{m}-{d}T{h}:{i}:{s}');
    form.value.enableStop = '2099-12-31T23:59:59';
    form.value.sort = 99;
  } else {
    accesskeyInfo({id: row.id}).then(res => {
      form.value = res.data;
      open.value = true;
      title.value = "修改";
    });
  }
}

/** 一键复制全部 */
function copyAk() {
  copy(keyInfoText.value, '全部密钥信息已复制到剪贴板');
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
          const data = res.data;
          form.value.appId = data.appId;
          form.value.accessKey = data.accessKey;
          form.value.secretKey = data.secretKey;

          keyInfoText.value = ''
          + 'AppId: ' + data.appId + '\r\n'
          + 'AccessKey: ' + data.accessKey + '\r\n'
          + 'SecretKey: ' + data.secretKey + '\r\n'
          + '有效期开始时间: ' + form.value.enableStart + '\r\n'
          + '有效期结束时间: ' + form.value.enableStop + '\r\n'
          + '请妥善管理好密钥信息!'

          showKeyInfo.value = true;
          proxy.$modal.msgSuccess("创建成功，请妥善保管密钥信息");
        });
      }
    }
  });
}
</script>

<style scoped>
.key-info-section {
  margin-top: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.key-display-area {
  width: 100%;
}

.key-textarea {
  font-family: 'Courier New', monospace;
  font-size: 14px;
}

.key-actions {
  margin-top: 15px;
  text-align: center;
}
</style>
