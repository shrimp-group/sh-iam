<template>
  <el-dialog :title="'用户详情 - ' + form?.nickname" v-model="open" width="1080px">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="基本信息" name="basic">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="用户编码">{{ form?.userCode }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ form?.username }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ form?.nickname }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ form?.email }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ form?.phone }}</el-descriptions-item>
          <el-descriptions-item label="启用状态">
            <dict-tag :options="BOOLEAN" :value="form?.userStatus" />
          </el-descriptions-item>
          <el-descriptions-item label="头像">{{ form?.avatar }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ form?.remark }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ parseTime(form?.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ form?.createBy }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>
      <el-tab-pane label="角色管理" name="role" v-if="roleLoaded">
        <user-role :userCode="form?.userCode" />
      </el-tab-pane>
      <el-tab-pane label="菜单来源" name="menuSource" v-if="menuSourceLoaded">
        <user-menu-source :userCode="form?.userCode" />
      </el-tab-pane>
    </el-tabs>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="open = false">关 闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="IamUserDetail">
import { userInfo } from "@/api/user/user";
import { parseTime } from "@/utils/ruoyi";
import UserRole from "./user-role";
import UserMenuSource from "./user-menu-source";

defineExpose({ init });
const { proxy } = getCurrentInstance();
const { BOOLEAN } = proxy.useDict("BOOLEAN");

const open = ref(false);
const activeTab = ref("basic");
const form = ref({});

// 标记各 Tab 是否已加载过
const roleLoaded = ref(false);
const menuSourceLoaded = ref(false);

// 切换 Tab 时按需加载
watch(activeTab, (val) => {
  if (val === "role" && !roleLoaded.value) {
    roleLoaded.value = true;
  }
  if (val === "menuSource" && !menuSourceLoaded.value) {
    menuSourceLoaded.value = true;
  }
});

function init(row) {
  activeTab.value = "basic";
  // 重置懒加载标记
  roleLoaded.value = false;
  menuSourceLoaded.value = false;
  if (row && row.id) {
    userInfo({ id: row.id }).then(res => {
      form.value = res.data;
      open.value = true;
    });
  }
}
</script>
