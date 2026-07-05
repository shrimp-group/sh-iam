<template>
  <el-dialog :title="'API 详情 - ' + apiInfo?.apiUri" v-model="open" width="1200px" :close-on-click-modal="false">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- Tab 1: 接口内容 -->
      <el-tab-pane label="接口内容" name="info">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="模块">{{ apiInfo?.module }}</el-descriptions-item>
          <el-descriptions-item label="应用编码">{{ apiInfo?.appCode }}</el-descriptions-item>
          <el-descriptions-item label="接口编码">{{ apiInfo?.apiCode }}</el-descriptions-item>
          <el-descriptions-item label="请求方法">
            <dict-tag :options="API_METHOD" :value="apiInfo?.apiMethod" />
          </el-descriptions-item>
          <el-descriptions-item label="URI">{{ apiInfo?.apiUri }}</el-descriptions-item>
          <el-descriptions-item label="接口名称">{{ apiInfo?.apiName }}</el-descriptions-item>
          <el-descriptions-item label="白名单">
            <dict-tag :options="BOOLEAN" :value="apiInfo?.writeFlag" />
          </el-descriptions-item>
          <el-descriptions-item label="排序">{{ apiInfo?.sort }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ apiInfo?.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ parseTime(apiInfo?.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ apiInfo?.createBy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ parseTime(apiInfo?.updateTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新人">{{ apiInfo?.updateBy || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <!-- Tab 2: 接口文档 -->
      <el-tab-pane label="接口文档" name="doc">
        <api-doc ref="apiDocRef" :api-info="apiInfo" />
      </el-tab-pane>

      <!-- Tab 3: 接口 Mock -->
      <el-tab-pane label="接口 Mock" name="mock">
        <api-mock ref="apiMockRef" :api-info="apiInfo" />
      </el-tab-pane>

      <!-- Tab 4: 字段权限（只读） -->
      <el-tab-pane label="字段权限" name="field">
        <div style="margin-bottom: 8px;">
          <span style="font-size: 14px; font-weight: 600; color: #303133;">
            字段权限列表 ({{ fieldList.length }})
          </span>
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
        </el-table>
      </el-tab-pane>

      <!-- Tab 5: 已授权菜单 -->
      <el-tab-pane label="已授权菜单" name="menu">
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
  </el-dialog>
</template>

<script setup name="IamApiDetail">
import { apiDetail } from "@/api/system/api";
import { apiFieldListByApi } from "@/api/system/api-field";
import ApiDoc from "./api-doc.vue";
import ApiMock from "./api-mock.vue";

defineExpose({ init });
const { proxy } = getCurrentInstance();
const { API_METHOD, BOOLEAN } = proxy.useDict("API_METHOD", "BOOLEAN");

const open = ref(false);
const activeTab = ref("info");
const apiInfo = ref(null);
const boundMenuPaths = ref([]);

// 字段权限相关
const fieldLoading = ref(false);
const fieldList = ref([]);

// 子组件引用
const apiDocRef = ref(null);
const apiMockRef = ref(null);

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
  activeTab.value = "info";
  fieldList.value = [];
  apiDetail({ id: row.id }).then(res => {
    apiInfo.value = res.data;
    // 将 boundMenuPaths 字符串列表转为对象列表以适配表格
    boundMenuPaths.value = (res.data?.boundMenuPaths || []).map(path => ({ path }));
    // 加载字段权限列表（依赖 apiInfo 中的 apiCode）
    loadFieldList();
  });
}

// Tab 切换时触发子组件加载
function handleTabChange(tab) {
  if (tab === 'doc' && apiDocRef.value) {
    apiDocRef.value.loadDoc();
  }
  if (tab === 'mock' && apiMockRef.value) {
    apiMockRef.value.loadDoc();
  }
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

// 关闭弹窗
function close() {
  open.value = false;
}
</script>
