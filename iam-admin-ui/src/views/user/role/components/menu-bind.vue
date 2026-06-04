<template>
  <el-dialog :title="'角色菜单绑定 - ' + roleInfo?.roleName" v-model="open" width="600px" :close-on-click-modal="false">
    <!-- 角色基本信息 -->
    <el-descriptions :column="2" border size="small" style="margin-bottom: 16px;">
      <el-descriptions-item label="角色名称">{{ roleInfo?.roleName }}</el-descriptions-item>
      <el-descriptions-item label="是否可申请">
        <dict-tag :options="BOOLEAN" :value="roleInfo?.applicable" />
      </el-descriptions-item>
    </el-descriptions>

    <el-tree
      ref="treeRef"
      v-loading="loading"
      :data="menuTreeData"
      :props="{ label: 'menuName', children: 'children' }"
      node-key="menuCode"
      show-checkbox
      default-expand-all
      :default-checked-keys="defaultCheckedKeys"
    >
      <template #default="{ node, data }">
        <span class="custom-tree-node">
          <span>{{ data.menuName }}</span>
          <el-tag v-if="data.menuType" size="small" style="margin-left: 8px;">{{ data.menuType === 'MENU' ? '菜单' : '按钮' }}</el-tag>
        </span>
      </template>
    </el-tree>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="close">取 消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="handleSave">保 存</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="RoleMenuBind">
import { menuTree } from "@/api/system/menu";
import { roleMenuList, roleMenuSave } from "@/api/system/role-menu";

defineExpose({ init });
const emit = defineEmits(["change"]);
const { proxy } = getCurrentInstance();
const { BOOLEAN } = proxy.useDict("BOOLEAN");

const open = ref(false);
const roleInfo = ref(null);
const loading = ref(false);
const saveLoading = ref(false);
const treeRef = ref(null);

const menuTreeData = ref([]);
const defaultCheckedKeys = ref([]);

// 获取树中所有叶子节点的 menuCode（仅在已绑定集合中的）
function getLeafKeys(tree, boundCodeSet) {
  const leafKeys = [];
  function traverse(nodes) {
    if (!nodes) return;
    for (const node of nodes) {
      if (node.children && node.children.length > 0) {
        traverse(node.children);
      } else {
        // 叶子节点：如果在已绑定列表中，加入默认勾选
        if (boundCodeSet.has(node.menuCode)) {
          leafKeys.push(node.menuCode);
        }
      }
    }
  }
  traverse(tree);
  return leafKeys;
}

function init(row) {
  open.value = true;
  menuTreeData.value = [];
  defaultCheckedKeys.value = [];
  roleInfo.value = row;

  loading.value = true;
  Promise.all([
    menuTree({ appCode: row.appCode }),
    roleMenuList({ roleCode: row.roleCode })
  ]).then(([treeRes, boundRes]) => {
    menuTreeData.value = treeRes.data || [];
    // roleMenuList 直接返回 menuCode 字符串数组
    const boundMenuCodes = boundRes.data || [];
    const boundCodeSet = new Set(boundMenuCodes);
    // el-tree 的 default-checked-keys 仅设置叶子节点，父节点会自动勾选
    defaultCheckedKeys.value = getLeafKeys(menuTreeData.value, boundCodeSet);
  }).finally(() => {
    loading.value = false;
  });
}

function handleSave() {
  const tree = treeRef.value;
  if (!tree) return;

  // 获取所有勾选的节点 key（全选的）+ 半选的父节点
  const checkedKeys = tree.getCheckedKeys();
  const halfCheckedKeys = tree.getHalfCheckedKeys();
  const allKeys = [...checkedKeys, ...halfCheckedKeys];

  saveLoading.value = true;
  roleMenuSave({
    roleCode: roleInfo.value?.roleCode,
    menuCodes: allKeys
  }).then(() => {
    proxy.$modal.msgSuccess("保存成功");
    emit("change");
    close();
  }).finally(() => {
    saveLoading.value = false;
  });
}

function close() {
  open.value = false;
}
</script>

<style scoped>
.custom-tree-node {
  display: flex;
  align-items: center;
  font-size: 14px;
}
</style>
