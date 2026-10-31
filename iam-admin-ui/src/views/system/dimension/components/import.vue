<template>
  <el-dialog title="导入数据维度" v-model="open" width="900px" :close-on-click-modal="false">
    <monaco-editor v-model="content" language="json" height="420px"/>
    <el-alert type="info" :closable="false" style="margin-top: 12px;"
      title="粘贴导出的数据维度 JSON 数组后导入：按「维度编码」判断存在性，已存在则更新、不存在则新增，全程不删除。"/>

    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="DataDimImport">
import { datadimImport } from "@/api/system/dim";
import MonacoEditor from "@sh/ui-core/components/MonacoEditor/index.vue.js";

defineExpose({init});
const emit = defineEmits(["change"]);
const { proxy } = getCurrentInstance();
const open = ref(false);
const content = ref("");
const submitLoading = ref(false);

/** 弹窗重置 */
function init() {
  content.value = "";
  open.value = true;
}

function cancel() {
  open.value = false;
  content.value = "";
}

/** 提交：JSON 解析 → 校验（任一不合法整体阻止）→ 导入 */
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
  datadimImport({dims: list}).then(res => {
    const d = res.data || {};
    proxy.$modal.msgSuccess(`导入成功：新增 ${d.insertCount || 0} 条，更新 ${d.updateCount || 0} 条`);
    open.value = false;
    emit("change", true);
  }).finally(() => {
    submitLoading.value = false;
  });
}

/** 空白判断（镜像后端 StringUtils.isBlank：null/undefined/空白字符串） */
function isBlank(v) {
  return v == null || (typeof v === "string" && v.trim() === "");
}

/** 校验维度列表（镜像后端 validateImport），返回错误明细 */
function validate(list) {
  const errors = [];
  const dimensionCodes = new Set();
  list.forEach((item, index) => {
    const isNode = item && typeof item === "object" && !Array.isArray(item);
    const tag = `第${index + 1}项(${isNode && item.dimensionCode ? item.dimensionCode : "?"})`;
    if (!isNode) {
      errors.push(`${tag}：不是对象`);
      return;
    }
    if (isBlank(item.appCode)) {
      errors.push(`${tag}：缺少应用编码`);
    }
    if (isBlank(item.dimensionCode)) {
      errors.push(`${tag}：缺少维度编码`);
    }
    if (isBlank(item.dimensionName)) {
      errors.push(`${tag}：缺少维度名称`);
    }
    if (!isBlank(item.dimensionCode)) {
      if (dimensionCodes.has(item.dimensionCode)) {
        errors.push(`${tag}：维度编码 ${item.dimensionCode} 重复出现`);
      }
      dimensionCodes.add(item.dimensionCode);
    }
    if (typeof item.dimensionDataJson === "string" && item.dimensionDataJson.trim() !== "") {
      try {
        JSON.parse(item.dimensionDataJson);
      } catch (e) {
        errors.push(`${tag}：维度数据JSON不合法`);
      }
    }
  });
  return errors;
}
</script>
