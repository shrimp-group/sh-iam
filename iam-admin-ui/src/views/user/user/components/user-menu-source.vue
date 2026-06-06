<template>
  <layout-split>
    <template #left>
      <app-options @select="selectApp"/>
    </template>
    <template #right>
      <!-- 菜单来源列表 -->
      <el-table v-loading="loading" :data="menuSourceData" size="small" max-height="500" min-height="200">
        <el-table-column label="菜单名称" prop="menuName" min-width="140" />
        <el-table-column label="来源角色" prop="roleName" min-width="140" />
        <el-table-column label="有效开始时间" prop="startTime" min-width="140">
          <template #default="{ row }">{{ parseTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="有效结束时间" prop="endTime" min-width="140">
          <template #default="{ row }">{{ parseTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" prop="enableStatus" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enableStatus === 1 ? 'success' : 'danger'" size="small">
              {{ row.enableStatus === 1 ? '有效' : '无效' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </layout-split>
</template>

<script setup name="IamUserMenuSource">
import { userMenuSourceList } from "@/api/user/user-role";
import { parseTime } from "@/utils/ruoyi";
import AppOptions from "@/views/components/AppOptions";

const props = defineProps({
  userCode: { type: String, default: '' }
});

// 当前选中的应用编码
const currentAppCode = ref('');

// 菜单来源数据
const loading = ref(false);
const menuSourceData = ref([]);

// 监听 userCode 变化重新加载
watch(() => props.userCode, (val) => {
  if (val && currentAppCode.value) {
    loadMenuSource();
  }
});

/** 选择应用 */
function selectApp(row) {
  currentAppCode.value = row.appCode;
  if (props.userCode) {
    loadMenuSource();
  } else {
    menuSourceData.value = [];
  }
}

/** 加载菜单来源数据 */
function loadMenuSource() {
  if (!props.userCode || !currentAppCode.value) return;
  loading.value = true;
  userMenuSourceList({ userCode: props.userCode, appCode: currentAppCode.value }).then(res => {
    menuSourceData.value = res.data || [];
  }).finally(() => {
    loading.value = false;
  });
}
</script>
