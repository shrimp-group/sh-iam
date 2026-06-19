<template>
  <div v-loading="loading">
    <!-- 未匹配提示 -->
    <el-alert v-if="noMatch" title="无法获取接口文档信息，请确认接口已注册" type="warning" :closable="false" show-icon />

    <template v-if="docData && !noMatch">
      <!-- 接口基本信息 -->
      <el-descriptions :column="2" border size="small" style="margin-bottom: 16px;">
        <el-descriptions-item label="请求方法">
          <el-tag :type="methodTagType(docData.method)" size="small">{{ docData.method }}</el-tag>
          <el-tag v-if="docData.deprecated" type="danger" size="small" style="margin-left: 4px;">已废弃</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="URI">{{ docData.uri }}</el-descriptions-item>
        <el-descriptions-item label="接口名称">{{ docData.operationSummary || docData.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="标签">
          <el-tag v-if="docData.tag" size="small" type="info">{{ docData.tag }}</el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="docData.operationDescription" label="详细描述" :span="2">{{ docData.operationDescription }}</el-descriptions-item>
        <el-descriptions-item label="接口描述">{{ docData.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Content-Type">
          <span v-if="docData.consumes && docData.consumes.length > 0">{{ docData.consumes.join(', ') }}</span>
          <span v-else-if="docData.produces && docData.produces.length > 0">{{ docData.produces.join(', ') }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 请求参数（树形表格） -->
      <div style="margin-bottom: 16px;">
        <div style="font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 8px;">请求参数</div>
        <el-table v-if="requestParamsTree.length > 0" :data="requestParamsTree" border size="small" row-key="key" default-expand-all :tree-props="{children: 'children'}" height="auto">
          <el-table-column label="参数名" prop="name" min-width="160" />
          <el-table-column label="类型" width="140">
            <template #default="{ row }">{{ formatType(row.type) }}</template>
          </el-table-column>
          <el-table-column label="来源" width="110">
            <template #default="{ row }">
              <el-tag v-if="row.annotationType" size="small" type="info">{{ row.annotationType }}</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="必需" width="70" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.required" size="small" type="danger">是</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="描述" prop="description" min-width="140">
            <template #default="{ row }">{{ row.description || '-' }}</template>
          </el-table-column>
          <el-table-column label="示例值" prop="example" width="120">
            <template #default="{ row }">{{ row.example || '-' }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="无请求参数" :image-size="40" />
      </div>

      <!-- 返回参数（树形表格） -->
      <div>
        <div style="font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 8px;">返回参数</div>
        <!-- 优先使用 returnSchemaFields (ApiDocFieldResp 结构) -->
        <template v-if="docData.returnSchemaFields && docData.returnSchemaFields.length > 0">
          <el-table :data="docData.returnSchemaFields" border size="small" row-key="key" default-expand-all :tree-props="{children: 'fields'}" height="auto">
            <el-table-column label="字段名" prop="name" min-width="160" />
            <el-table-column label="类型" width="140">
              <template #default="{ row }">{{ formatType(row.type) }}</template>
            </el-table-column>
            <el-table-column label="描述" prop="description" min-width="140">
              <template #default="{ row }">{{ row.description || '-' }}</template>
            </el-table-column>
            <el-table-column label="示例" prop="example" width="120">
              <template #default="{ row }">{{ row.example || '-' }}</template>
            </el-table-column>
            <el-table-column label="必需" width="70" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.required" size="small" type="danger">是</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </template>
        <!-- 兼容回退: returnFields (EntityFieldNode 结构) -->
        <template v-else-if="docData.returnFields && docData.returnFields.length > 0">
          <el-table :data="docData.returnFields" border size="small" row-key="jsonPath" default-expand-all :tree-props="{children: 'children'}">
            <el-table-column label="字段名" prop="fieldName" min-width="160" />
            <el-table-column label="描述" prop="fieldDesc" min-width="140">
              <template #default="{ row }">{{ row.fieldDesc || '-' }}</template>
            </el-table-column>
            <el-table-column label="类型" prop="fieldType" width="120" />
            <el-table-column label="列表" prop="isList" width="80" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.isList" size="small" type="warning">是</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="JSON路径" prop="jsonPath" min-width="240" />
          </el-table>
        </template>
        <el-empty v-else description="无返回参数信息" :image-size="40" />
      </div>
    </template>
  </div>
</template>

<script setup name="ApiDoc">
import { apiDoc } from "@/api/system/api";

const props = defineProps({
  apiInfo: { type: Object, default: null }
});

defineExpose({ loadDoc });

const loading = ref(false);
const docData = ref(null);
const noMatch = ref(false);
const loaded = ref(false);

// 请求参数树形数据：将 fields 映射为 children，使 el-table tree-props 可识别
const requestParamsTree = computed(() => {
  if (!docData.value?.requestParams) return [];
  return docData.value.requestParams.map(param => {
    const node = { ...param, key: param.name };
    if (param.fields && param.fields.length > 0) {
      // Fields from backend already have key set by convertRestFields
      node.children = param.fields;
    }
    return node;
  });
});

// 请求方法标签颜色
function methodTagType(method) {
  const map = { GET: 'success', POST: 'warning', PUT: '', DELETE: 'danger', PATCH: 'info', REQUEST: 'info' };
  return map[method] || 'info';
}

// 格式化类型名（取简单类名）
function formatType(type) {
  if (!type) return '-';
  const parts = type.split('.');
  return parts[parts.length - 1];
}

// 加载接口文档
function loadDoc() {
  if (!props.apiInfo?.apiMethod || !props.apiInfo?.apiUri) return;
  if (loaded.value) return;
  loading.value = true;
  noMatch.value = false;
  apiDoc({ method: props.apiInfo.apiMethod, uri: props.apiInfo.apiUri }).then(res => {
    docData.value = res.data;
    if (!res.data?.name && !res.data?.requestParams && !res.data?.returnFields && !res.data?.returnSchemaFields) {
      noMatch.value = true;
    }
    loaded.value = true;
  }).catch(() => {
    noMatch.value = true;
  }).finally(() => {
    loading.value = false;
  });
}

// 监听 apiInfo 变化时重置加载状态
watch(() => props.apiInfo, () => {
  loaded.value = false;
  docData.value = null;
  noMatch.value = false;
});
</script>
