<template>
  <el-dialog title="导入API" v-model="open" width="900px" :close-on-click-modal="false">
    <monaco-editor v-model="content" language="json" height="420px"/>
    <el-alert type="info" :closable="false" style="margin-top: 12px;"
      title="粘贴导出的 JSON 数组后导入：按「接口编码」判断存在性，已存在则更新、不存在则新增，全程不做删除。字段权限项的「字段编码」为必填精准标识。"/>

    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="ApiImport">
import { apiImport } from "@/api/system/api";
import MonacoEditor from "@sh/ui-core/components/MonacoEditor/index.vue.js";

defineExpose({init});
const emit = defineEmits(["change"]);
const { proxy } = getCurrentInstance();
const open = ref(false);
const content = ref("");
const submitLoading = ref(false);

/** 字段权限合法动作类型 */
const FIELD_ACTIONS = ["HIDDEN", "MASK", "READ_ONLY"];

/** 弹窗重置 */
function init() {
  content.value = "";
  open.value = true;
}

function cancel() {
  open.value = false;
  content.value = "";
}

/** 提交：JSON 解析 → 前置校验（任一不合法整体阻止）→ 导入 */
function submitForm() {
  let list;
  try {
    list = JSON.parse(content.value);
  } catch (e) {
    proxy.$modal.msgError("JSON 格式不合法：" + e.message);
    return;
  }
  if (!Array.isArray(list) || list.length === 0) {
    proxy.$modal.msgError("导入内容必须为非空的 JSON 数组");
    return;
  }
  const errors = validate(list);
  if (errors.length > 0) {
    const shown = errors.slice(0, 10).join("；");
    proxy.$modal.msgError(`导入校验未通过（共 ${errors.length} 项）：` + shown + (errors.length > 10 ? " …" : ""));
    return;
  }
  submitLoading.value = true;
  apiImport({apis: list}).then(res => {
    const d = res.data || {};
    proxy.$modal.msgSuccess(`导入成功：新增API ${d.insertApiCount || 0} 条，更新API ${d.updateApiCount || 0} 条，新增字段权限 ${d.insertFieldCount || 0} 项，更新字段权限 ${d.updateFieldCount || 0} 项`);
    open.value = false;
    emit("change", true);
  }).finally(() => {
    submitLoading.value = false;
  });
}

/** 逐条校验必填字段，返回错误明细 */
function validate(list) {
  const errors = [];
  const apiCodes = new Set();
  const fieldKeys = new Set();
  list.forEach((item, index) => {
    const tag = `第${index + 1}条`;
    if (!item || typeof item !== "object" || Array.isArray(item)) {
      errors.push(`${tag}：不是对象`);
      return;
    }
    if (!item.appCode) {
      errors.push(`${tag}：缺少应用编码`);
    }
    if (!item.apiCode) {
      errors.push(`${tag}：缺少接口编码`);
    }
    if (!item.apiMethod) {
      errors.push(`${tag}：缺少请求方法`);
    }
    if (!item.apiUri) {
      errors.push(`${tag}：缺少请求URI`);
    }
    if (item.apiCode) {
      if (apiCodes.has(item.apiCode)) {
        errors.push(`${tag}：接口编码 ${item.apiCode} 重复出现`);
      }
      apiCodes.add(item.apiCode);
    }
    (Array.isArray(item.fields) ? item.fields : []).forEach((field, j) => {
      const fieldTag = `${tag}字段权限第${j + 1}项`;
      if (!field.fieldCode) {
        errors.push(`${fieldTag}：缺少字段编码`);
      }
      if (!field.fieldName) {
        errors.push(`${fieldTag}：缺少字段名称`);
      }
      if (!field.jsonPath) {
        errors.push(`${fieldTag}：缺少JSON路径`);
      }
      if (!field.action) {
        errors.push(`${fieldTag}：缺少权限动作`);
      } else if (!FIELD_ACTIONS.includes(field.action)) {
        errors.push(`${fieldTag}：权限动作 ${field.action} 不合法，应为 HIDDEN/MASK/READ_ONLY`);
      }
      if (field.action === "MASK" && !field.maskRule) {
        errors.push(`${fieldTag}：动作类型为脱敏时，脱敏规则不能为空`);
      }
      if (item.apiCode && field.fieldCode) {
        const key = item.apiCode + "|" + field.fieldCode;
        if (fieldKeys.has(key)) {
          errors.push(`${fieldTag}：字段编码 ${field.fieldCode} 重复出现`);
        }
        fieldKeys.add(key);
      }
    });
  });
  return errors;
}
</script>
