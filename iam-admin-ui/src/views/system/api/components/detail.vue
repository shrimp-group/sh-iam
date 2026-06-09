<template>
  <el-dialog :title="'API 详情 - ' + apiInfo?.apiUri" v-model="open" width="1200px" :close-on-click-modal="false">
    <!-- API 基本信息 -->
    <el-descriptions :column="2" border size="small" style="margin-bottom: 16px;">
      <el-descriptions-item label="模块">{{ apiInfo?.module }}</el-descriptions-item>
      <el-descriptions-item label="应用编码">{{ apiInfo?.appCode }}</el-descriptions-item>
      <el-descriptions-item label="请求方法">
        <dict-tag :options="API_METHOD" :value="apiInfo?.apiMethod" />
      </el-descriptions-item>
      <el-descriptions-item label="URI">{{ apiInfo?.apiUri }}</el-descriptions-item>
      <el-descriptions-item label="名称">{{ apiInfo?.apiName }}</el-descriptions-item>
      <el-descriptions-item label="白名单">
        <dict-tag :options="BOOLEAN" :value="apiInfo?.writeFlag" />
      </el-descriptions-item>
      <el-descriptions-item label="备注" :span="2">{{ apiInfo?.remark }}</el-descriptions-item>
    </el-descriptions>

    <!-- Tab 区域 -->
    <el-tabs v-model="activeTab">
      <!-- 字段权限 -->
      <el-tab-pane label="字段权限" name="field">
        <div style="margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center;">
          <span style="font-size: 14px; font-weight: 600; color: #303133;">
            字段权限列表 ({{ fieldList.length }})
          </span>
          <el-button type="primary" plain icon="Plus" size="small" @click="handleFieldAdd">新增</el-button>
        </div>
        <el-table v-loading="fieldLoading" :data="fieldList" max-height="400" empty-text="暂无字段权限">
          <el-table-column label="字段名称" prop="fieldName" min-width="200" />
          <el-table-column label="JSON路径" prop="jsonPath" min-width="240" />
          <el-table-column label="权限动作" prop="action" width="120">
            <template #default="{ row }">
              <el-tag :type="actionTagType(row.action)" size="small">{{ actionLabel(row.action) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="脱敏规则" prop="maskRule" min-width="120">
            <template #default="{ row }">{{ row.maskRule || '-' }}</template>
          </el-table-column>
          <el-table-column label="描述" prop="description" min-width="120">
            <template #default="{ row }">{{ row.description || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" icon="Edit" @click="handleFieldEdit(row)">编辑</el-button>
              <el-popconfirm :title="'确认删除字段: ' + row.fieldName + '?'" placement="top-end" @confirm="handleFieldDelete(row)">
                <template #reference><el-button link type="danger" icon="Delete">删除</el-button></template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 已绑定的菜单 -->
      <el-tab-pane label="已绑定菜单" name="menu">
        <el-table :data="boundMenuPaths" v-if="boundMenuPaths.length > 0" max-height="300">
          <el-table-column label="序号" type="index" width="60" />
          <el-table-column label="菜单全路径" prop="path" min-width="300" />
        </el-table>
        <el-empty v-else description="暂无绑定的菜单" :image-size="60" />
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="close">关 闭</el-button>
      </div>
    </template>

    <!-- 字段权限编辑弹窗 -->
    <api-field-edit ref="apiFieldEditRef" @change="loadFieldList" />
  </el-dialog>
</template>

<script setup name="IamApiDetail">
import { apiDetail } from "@/api/system/api";
import { apiFieldListByApi, apiFieldRemove } from "@/api/system/api-field";
import ApiFieldEdit from "./api-field-edit.vue";

defineExpose({ init });
const { proxy } = getCurrentInstance();
const { API_METHOD, BOOLEAN } = proxy.useDict("API_METHOD", "BOOLEAN");

const open = ref(false);
const activeTab = ref("field");
const apiInfo = ref(null);
const boundMenuPaths = ref([]);

// 字段权限相关
const fieldLoading = ref(false);
const fieldList = ref([]);
const apiFieldEditRef = ref(null);

// 权限动作标签映射
const actionMap = {
  HIDDEN: { label: '隐藏', type: 'danger' },
  MASK: { label: '脱敏', type: 'warning' },
  READ_ONLY: { label: '只读', type: 'info' }
}

function actionLabel(action) {
  return actionMap[action]?.label || action
}

function actionTagType(action) {
  return actionMap[action]?.type || 'info'
}

// 初始化
function init(row) {
  open.value = true;
  activeTab.value = "field";
  fieldList.value = [];
  apiDetail({ id: row.id }).then(res => {
    apiInfo.value = res.data;
    // 将 boundMenuPaths 字符串列表转为对象列表以适配表格
    boundMenuPaths.value = (res.data?.boundMenuPaths || []).map(path => ({ path }));
    // 加载字段权限列表（依赖 apiInfo 中的 apiCode）
    loadFieldList();
  });
}

// 加载字段权限列表
function loadFieldList() {
  if (!apiInfo.value?.apiCode) return
  fieldLoading.value = true
  apiFieldListByApi({ apiCode: apiInfo.value.apiCode }).then(res => {
    fieldList.value = res.data || []
  }).finally(() => {
    fieldLoading.value = false
  })
}

// 新增字段权限
function handleFieldAdd() {
  apiFieldEditRef.value.init(null, apiInfo.value)
}

// 编辑字段权限
function handleFieldEdit(row) {
  apiFieldEditRef.value.init(row, apiInfo.value)
}

// 删除字段权限
function handleFieldDelete(row) {
  apiFieldRemove({ id: row.id }).then(() => {
    proxy.$modal.msgSuccess("删除成功")
    loadFieldList()
  })
}

// 关闭弹窗
function close() {
  open.value = false;
}
</script>
