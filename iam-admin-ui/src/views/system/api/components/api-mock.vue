<template>
  <div>
    <!-- 未加载文档时的提示 -->
    <el-alert v-if="!docData" title="请先加载接口文档信息" type="info" :closable="false" show-icon style="margin-bottom: 12px;" />

    <template v-if="docData">
      <!-- 请求信息 -->
      <div style="margin-bottom: 16px;">
        <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 12px;">
          <el-tag :type="methodTagType(docData.method)" size="small">{{ docData.method }}</el-tag>
          <el-input v-model="requestUrl" placeholder="请求地址" style="flex: 1;" readonly />
        </div>

        <!-- Header 设置 -->
        <el-collapse v-model="activeCollapse">
          <el-collapse-item title="请求头 (Headers)" name="headers">
            <div v-for="(header, index) in headers" :key="index" style="display: flex; gap: 8px; margin-bottom: 8px;">
              <el-input v-model="header.key" placeholder="Header 名称" style="flex: 1;" />
              <el-input v-model="header.value" placeholder="Header 值" style="flex: 1;" />
              <el-button link type="danger" icon="Delete" @click="headers.splice(index, 1)" />
            </div>
            <el-button type="primary" plain icon="Plus" size="small" @click="addHeader">添加 Header</el-button>
          </el-collapse-item>
        </el-collapse>
      </div>

      <!-- 请求参数 -->
      <div style="margin-bottom: 16px;">
        <div style="font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 8px;">请求参数</div>

        <!-- GET 请求: Query 参数输入 -->
        <template v-if="isGetMethod">
          <div v-if="queryParams.length > 0">
            <div v-for="(param, index) in queryParams" :key="index" style="display: flex; gap: 8px; margin-bottom: 8px; align-items: center;">
              <el-input v-model="param.name" placeholder="参数名" style="flex: 1;" disabled />
              <el-input v-model="param.value" :placeholder="param.required ? '必需' : '可选'" style="flex: 1;" />
              <el-tag v-if="param.required" size="small" type="danger">必需</el-tag>
            </div>
          </div>
          <el-empty v-else description="无 Query 参数" :image-size="40" />
        </template>

        <!-- POST/PUT/DELETE 请求: JSON Body 编辑器 -->
        <template v-else>
          <el-input v-model="requestBody" type="textarea" :rows="10" placeholder="请输入 JSON 格式的请求体" />
          <div style="margin-top: 4px; font-size: 12px; color: #909399;">
            <span v-if="docData.requestParams && docData.requestParams.length > 0">
              参考 Body 结构:
              <span v-for="(param, index) in docData.requestParams.filter(p => p.annotationType === 'RequestBody')" :key="index">
                <span v-if="param.fields && param.fields.length > 0">
                  {{ param.name }}: { <span v-for="(field, fi) in param.fields" :key="fi">{{ field.name }}{{ field.required ? '*' : '' }}: {{ formatType(field.type || field.simpleType) }}{{ field.example ? ` (${field.example})` : '' }}{{ fi < param.fields.length - 1 ? ', ' : '' }}</span> } }
                </span>
                <span v-else>
                  {{ param.name }}: {{ formatType(param.type) }}{{ param.example ? ` (${param.example})` : '' }}
                </span>
                {{ index < docData.requestParams.filter(p => p.annotationType === 'RequestBody').length - 1 ? ', ' : '' }}
              </span>
            </span>
          </div>
        </template>
      </div>

      <!-- 操作按钮 -->
      <div style="margin-bottom: 16px; display: flex; gap: 8px;">
        <el-button type="primary" icon="Promotion" @click="sendRequest" :loading="sending">发送请求</el-button>
        <el-button icon="Refresh" @click="resetForm">重置</el-button>
      </div>

      <!-- 响应结果 -->
      <div v-if="response">
        <div style="font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 8px;">响应结果</div>
        <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px;">
          <el-tag :type="response.status >= 200 && response.status < 300 ? 'success' : 'danger'" size="small">
            {{ response.status }} {{ response.statusText }}
          </el-tag>
          <span style="font-size: 12px; color: #909399;">耗时: {{ response.costTime }}ms</span>
        </div>
        <el-input v-model="response.body" type="textarea" :rows="12" readonly />
      </div>
    </template>
  </div>
</template>

<script setup name="ApiMock">
import { apiDoc } from "@/api/system/api";
import axios from 'axios';
import { getToken } from '@/utils/auth';
import { baseApi } from '~/env';

const props = defineProps({
  apiInfo: { type: Object, default: null }
});

defineExpose({ loadDoc });

const docData = ref(null);
const loaded = ref(false);
const requestUrl = ref('');
const headers = ref([{ key: 'token', value: '' }]);
const activeCollapse = ref([]);
const sending = ref(false);
const response = ref(null);

// GET 请求参数
const queryParams = ref([]);

// POST 请求体
const requestBody = ref('');

const isGetMethod = computed(() => {
  return docData.value?.method === 'GET';
});

// 请求方法标签颜色
function methodTagType(method) {
  const map = { GET: 'success', POST: 'warning', PUT: '', DELETE: 'danger', PATCH: 'info', REQUEST: 'info' };
  return map[method] || 'info';
}

// 格式化类型名
function formatType(type) {
  if (!type) return '-';
  const parts = type.split('.');
  return parts[parts.length - 1];
}

// 添加 Header
function addHeader() {
  headers.value.push({ key: '', value: '' });
}

// 递归生成字段模板对象（基于 ApiDocFieldResp 结构）
function generateFieldTemplate(fields) {
  if (!fields || fields.length === 0) return null;
  const obj = {};
  fields.forEach(field => {
    if (field.fields && field.fields.length > 0 && !field.selfReferencing) {
      obj[field.name] = field.simpleType ? (field.example || null) : generateFieldTemplate(field.fields);
    } else {
      obj[field.name] = field.example || null;
    }
  });
  return obj;
}

// 加载接口文档
function loadDoc() {
  if (!props.apiInfo?.apiMethod || !props.apiInfo?.apiUri) return;
  if (loaded.value) return;
  apiDoc({ method: props.apiInfo.apiMethod, uri: props.apiInfo.apiUri }).then(res => {
    docData.value = res.data;
    requestUrl.value = res.data?.uri || '';
    // 初始化 token
    const token = getToken();
    if (token) {
      const tokenHeader = headers.value.find(h => h.key === 'token');
      if (tokenHeader) tokenHeader.value = token;
    }
    // 初始化 GET 参数（优先使用 example 预填充）
    if (res.data?.requestParams) {
      queryParams.value = res.data.requestParams
        .filter(p => p.annotationType !== 'RequestBody')
        .map(p => ({ name: p.name, value: p.example || p.defaultValue || '', required: p.required }));
    }
    // 初始化 POST Body 模板（使用 fields 结构和 example 值生成）
    if (res.data?.method !== 'GET' && res.data?.requestParams) {
      const bodyParams = res.data.requestParams.filter(p => p.annotationType === 'RequestBody');
      if (bodyParams.length > 0) {
        const template = {};
        bodyParams.forEach(p => {
          if (p.fields && p.fields.length > 0) {
            // 使用 fields 结构生成模板
            template[p.name] = generateFieldTemplate(p.fields);
          } else {
            // 使用 example 值或 null
            template[p.name] = p.example || null;
          }
        });
        requestBody.value = JSON.stringify(template, null, 2);
      }
    }
    loaded.value = true;
  }).catch(() => {
    // 接口文档加载失败
  });
}

// 发送请求
function sendRequest() {
  if (!docData.value) return;
  sending.value = true;
  response.value = null;

  const method = docData.value.method?.toLowerCase() || 'get';
  const url = baseApi + requestUrl.value;

  // 构建 headers
  const reqHeaders = { 'Content-Type': 'application/json;charset=utf-8' };
  headers.value.forEach(h => {
    if (h.key && h.value) reqHeaders[h.key] = h.value;
  });

  // 构建参数
  const config = { method, url, headers: reqHeaders };
  if (method === 'get') {
    const params = {};
    queryParams.value.forEach(p => {
      if (p.value) params[p.name] = p.value;
    });
    config.params = params;
  } else {
    try {
      config.data = requestBody.value ? JSON.parse(requestBody.value) : {};
    } catch (e) {
      config.data = requestBody.value;
    }
  }

  const startTime = Date.now();
  axios(config).then(res => {
    response.value = {
      status: res.status,
      statusText: res.statusText,
      costTime: Date.now() - startTime,
      body: typeof res.data === 'string' ? res.data : JSON.stringify(res.data, null, 2)
    };
  }).catch(err => {
    response.value = {
      status: err.response?.status || 0,
      statusText: err.response?.statusText || 'Error',
      costTime: Date.now() - startTime,
      body: err.response?.data ? JSON.stringify(err.response.data, null, 2) : err.message
    };
  }).finally(() => {
    sending.value = false;
  });
}

// 重置表单
function resetForm() {
  queryParams.value.forEach(p => { p.value = ''; });
  requestBody.value = '';
  response.value = null;
}

// 监听 apiInfo 变化时重置
watch(() => props.apiInfo, () => {
  loaded.value = false;
  docData.value = null;
  response.value = null;
});
</script>
