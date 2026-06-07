<template>
  <layout-split>
    <template #left>
      <app-options @select="selectApp" height="660px"/>
    </template>
    <template #right>
      <!-- 菜单来源树形表格 -->
      <el-table
        v-loading="loading"
        :data="menuTreeData"
        row-key="menuCode"
        :tree-props="{ children: 'children' }"
        size="small"
        max-height="500"
        min-height="200"
        default-expand-all
      >
        <el-table-column label="菜单名称" prop="menuName" min-width="200">
          <template #default="{ row }">
            <svg-icon v-if="row.icon" :icon-class="row.icon" style="margin-right: 8px;" />
            <span>{{ row.menuName }}</span>
            <el-tag v-if="row.menuType" size="small" style="margin-left: 8px;">{{ row.menuType === 'MENU' ? '菜单' : '按钮' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源角色" min-width="200">
          <template #default="{ row }">
            <template v-if="row.roleSources && row.roleSources.length > 0">
              <template v-for="(source, idx) in row.roleSources" :key="idx">
                <el-popover placement="top" :width="420" trigger="hover">
                  <template #reference>
                    <el-tag size="small" style="margin-right: 4px; cursor: pointer; margin-bottom: 2px;">{{ source.roleName }}</el-tag>
                  </template>
                  <div v-if="source.timeRanges && source.timeRanges.length > 0">
                    <el-table :data="source.timeRanges" size="small" height="auto">
                      <el-table-column label="有效开始时间" width="160">
                        <template #default="{ row: timeRow }">{{ parseTime(timeRow.startTime) }}</template>
                      </el-table-column>
                      <el-table-column label="有效结束时间" width="160">
                        <template #default="{ row: timeRow }">{{ parseTime(timeRow.endTime) }}</template>
                      </el-table-column>
                      <el-table-column label="状态" width="70">
                        <template #default="{ row: timeRow }">
                          <el-tag :type="timeRow.enableStatus === 1 ? 'success' : 'danger'" size="small">
                            {{ timeRow.enableStatus === 1 ? '有效' : '无效' }}
                          </el-tag>
                        </template>
                      </el-table-column>
                    </el-table>
                  </div>
                  <div v-else style="color: #909399; font-size: 12px;">暂无有效时间范围</div>
                </el-popover>
              </template>
            </template>
            <span v-else style="color: #c0c4cc;">-</span>
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
const menuTreeData = ref([]);

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
    menuTreeData.value = [];
  }
}

/** 加载菜单来源数据 */
function loadMenuSource() {
  if (!props.userCode || !currentAppCode.value) return;
  loading.value = true;
  userMenuSourceList({ userCode: props.userCode, appCode: currentAppCode.value }).then(res => {
    const flatList = res.data || [];
    // 构建菜单树并合并角色来源
    menuTreeData.value = buildMenuSourceTree(flatList);
  }).finally(() => {
    loading.value = false;
  });
}

/**
 * 构建菜单来源树
 * 1. 按 menuCode 分组，合并同一菜单下的角色来源
 * 2. 基于 parentCode 构建树形结构
 */
function buildMenuSourceTree(flatList) {
  // 按 menuCode 分组，合并角色来源
  const menuMap = new Map();
  for (const item of flatList) {
    if (!menuMap.has(item.menuCode)) {
      menuMap.set(item.menuCode, {
        menuCode: item.menuCode,
        menuName: item.menuName,
        parentCode: item.parentCode,
        menuType: item.menuType,
        sort: item.sort,
        roleSources: []
      });
    }
    // 添加角色来源（可能同一菜单来自同一角色的不同有效期）
    const menu = menuMap.get(item.menuCode);
    if (item.roleCode) {
      // 查找是否已有该角色的来源
      let existingSource = menu.roleSources.find(s => s.roleCode === item.roleCode);
      if (!existingSource) {
        existingSource = {
          roleCode: item.roleCode,
          roleName: item.roleName,
          timeRanges: []
        };
        menu.roleSources.push(existingSource);
      }
      // 添加时间范围
      existingSource.timeRanges.push({
        startTime: item.startTime,
        endTime: item.endTime,
        enableStatus: item.enableStatus
      });
    }
  }

  // 基于 parentCode 构建树形结构
  const tree = [];
  for (const menu of menuMap.values()) {
    const parentCode = menu.parentCode;
    if (!parentCode || parentCode === '0') {
      tree.push(menu);
    } else {
      const parent = menuMap.get(parentCode);
      if (parent) {
        if (!parent.children) {
          parent.children = [];
        }
        parent.children.push(menu);
      }
    }
  }

  return tree;
}
</script>
