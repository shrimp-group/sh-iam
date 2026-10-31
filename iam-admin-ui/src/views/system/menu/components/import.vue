<template>
  <el-dialog title="导入菜单" v-model="open" width="900px" :close-on-click-modal="false">
    <monaco-editor v-model="content" language="json" height="420px"/>
    <el-alert type="info" :closable="false" style="margin-top: 12px;"
      title="粘贴导出的菜单 JSON 数组后导入：按「菜单编码」判断存在性，已存在则更新、不存在则新增，全程不删除菜单。节点绑定的接口编码会一并写入关联表，接口可后续经「API 导入」迁移后自动生效。"/>

    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="MenuImport">
import { menuImport } from "@/api/system/menu";
import MonacoEditor from "@sh/ui-core/components/MonacoEditor/index.vue.js";

defineExpose({init});
const emit = defineEmits(["change"]);
const { proxy } = getCurrentInstance();
const open = ref(false);
const content = ref("");
const submitLoading = ref(false);

/** 菜单类型合法值 */
const MENU_TYPES = ["MENU", "BUTTON", "FIELDS"];

/** 弹窗重置 */
function init() {
  content.value = "";
  open.value = true;
}

function cancel() {
  open.value = false;
  content.value = "";
}

/** 提交：JSON 解析 → 递归校验（任一不合法整体阻止）→ 导入 */
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
  menuImport({menus: list}).then(res => {
    const d = res.data || {};
    proxy.$modal.msgSuccess(`导入成功：新增菜单 ${d.insertMenuCount || 0} 条，更新菜单 ${d.updateMenuCount || 0} 条，重建绑定 ${d.rebuildBindCount || 0} 条`);
    if (d.parentFixHints && d.parentFixHints.length > 0) {
      proxy.$modal.msgWarning(d.parentFixHints.join("；"));
    }
    open.value = false;
    emit("change", true);
  }).finally(() => {
    submitLoading.value = false;
  });
}

/** 递归校验菜单树（镜像后端 validateImport），返回错误明细 */
function validate(list) {
  const errors = [];
  const menuCodes = new Set();
  checkNodes(list, "", errors, menuCodes);
  return errors;
}

/** 先序递归遍历：顶层标签「第N项(menuCode)」，子级在前缀上追加「>子级第M项(menuCode)」 */
function checkNodes(nodes, prefix, errors, menuCodes) {
  nodes.forEach((item, index) => {
    const isNode = item && typeof item === "object" && !Array.isArray(item);
    const tag = `${prefix}第${index + 1}项(${isNode && item.menuCode ? item.menuCode : "?"})`;
    if (!isNode) {
      errors.push(`${tag}：不是对象`);
      return;
    }
    if (!item.menuCode) {
      errors.push(`${tag}：缺少菜单编码`);
    }
    if (!item.menuName) {
      errors.push(`${tag}：缺少菜单名称`);
    }
    if (!MENU_TYPES.includes(item.menuType)) {
      errors.push(`${tag}：菜单类型 ${item.menuType || "空"} 不合法，应为 MENU/BUTTON/FIELDS`);
    }
    if (item.menuCode) {
      if (menuCodes.has(item.menuCode)) {
        errors.push(`${tag}：菜单编码 ${item.menuCode} 重复出现`);
      }
      menuCodes.add(item.menuCode);
    }
    if (item.apiCodes !== undefined && item.apiCodes !== null) {
      const legal = Array.isArray(item.apiCodes) && item.apiCodes.every(code => typeof code === "string" && code !== "");
      if (!legal) {
        errors.push(`${tag}：接口编码列表不合法，须为非空字符串数组`);
      }
    }
    if (Array.isArray(item.children) && item.children.length > 0) {
      checkNodes(item.children, tag + ">子级", errors, menuCodes);
    }
  });
}
</script>
