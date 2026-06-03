<template>
  <el-dialog :title="'API 详情 - ' + apiInfo?.apiUri" v-model="open" width="800px" :close-on-click-modal="false">
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

    <!-- 已绑定的菜单列表 -->
    <div style="margin-top: 16px;">
      <div style="font-size: 14px; font-weight: 600; margin-bottom: 8px; color: #303133;">
        已绑定的菜单 ({{ boundMenuPaths.length }})
      </div>
      <el-table :data="boundMenuPaths" v-if="boundMenuPaths.length > 0" max-height="300">
        <el-table-column label="序号" type="index" width="60" />
        <el-table-column label="菜单全路径" prop="path" min-width="300" />
      </el-table>
      <el-empty v-else description="暂无绑定的菜单" :image-size="60" />
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="close">关 闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="IamApiDetail">
import { apiDetail } from "@/api/system/api";

defineExpose({ init });
const { proxy } = getCurrentInstance();
const { API_METHOD, BOOLEAN } = proxy.useDict("API_METHOD", "BOOLEAN");

const open = ref(false);
const apiInfo = ref(null);
const boundMenuPaths = ref([]);

// 初始化
function init(row) {
  open.value = true;
  apiDetail({ id: row.id }).then(res => {
    apiInfo.value = res.data;
    // 将 boundMenuPaths 字符串列表转为对象列表以适配表格
    boundMenuPaths.value = (res.data?.boundMenuPaths || []).map(path => ({ path }));
  });
}

// 关闭弹窗
function close() {
  open.value = false;
}
</script>
