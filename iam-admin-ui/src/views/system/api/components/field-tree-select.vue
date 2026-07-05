<template>
  <el-dialog title="选择字段" v-model="open" width="600px" :close-on-click-modal="false" append-to-body>
    <!-- 自动定位成功提示 -->
    <el-alert v-if="resolvedEntityName" :title="'已定位返回结构: ' + resolvedEntityName" type="success" :closable="false" show-icon style="margin-bottom: 8px;" />
    <!-- 自动定位失败提示 -->
    <el-alert v-if="resolveMessage" :title="resolveMessage" type="warning" :closable="false" show-icon style="margin-bottom: 8px;" />

    <!-- 字段树 -->
    <div class="field-section">
      <div class="section-header">
        <span class="section-title">字段列表</span>
        <el-input v-model="fieldFilter" placeholder="搜索字段名" clearable size="small" style="width: 180px;" />
      </div>
      <el-tree
        ref="fieldTreeRef"
        v-loading="treeLoading"
        :data="fieldTreeData"
        :props="{ label: 'fieldName', children: 'children' }"
        :filter-node-method="filterNode"
        node-key="jsonPath"
        show-checkbox
        check-strictly
        :default-expand-all="true"
        max-height="400px"
        @check="handleCheck"
      >
        <template #default="{ node, data }">
          <span class="field-node">
            <span>{{ data.fieldName }}<span v-if="data.fieldDesc" style="color: #909399; margin-left: 4px;">- {{ data.fieldDesc }}</span></span>
            <span v-if="data.jsonPath" class="field-path">{{ data.jsonPath }}</span>
          </span>
        </template>
      </el-tree>
      <el-empty v-if="!treeLoading && fieldTreeData.length === 0 && !resolveMessage" description="无法获取接口返回结构，请手动维护字段" :image-size="60" />
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="open = false">取 消</el-button>
        <el-button type="primary" @click="handleConfirm" :disabled="!checkedNode">确 定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="FieldTreeSelect">
import { entityFieldResolve } from '@/api/system/api-field'

defineExpose({ init })

const emit = defineEmits(['confirm'])

const open = ref(false)
const fieldFilter = ref('')
const treeLoading = ref(false)
const fieldTreeData = ref([])
const checkedNode = ref(null)
const fieldTreeRef = ref(null)

// 自动定位相关
const resolvedEntityName = ref('')
const resolveMessage = ref('')

// 监听字段搜索关键词变化
watch(fieldFilter, (val) => {
  fieldTreeRef.value?.filter(val)
})

// 树节点过滤方法
function filterNode(value, data) {
  if (!value) return true
  const keyword = value.toLowerCase()
  return (data.fieldName && data.fieldName.toLowerCase().includes(keyword)) ||
    (data.fieldDesc && data.fieldDesc.toLowerCase().includes(keyword)) ||
    (data.jsonPath && data.jsonPath.toLowerCase().includes(keyword))
}

// 初始化弹窗
function init(apiContext) {
  open.value = true
  fieldFilter.value = ''
  fieldTreeData.value = []
  checkedNode.value = null
  resolvedEntityName.value = ''
  resolveMessage.value = ''

  // 通过 API 接口获取返回值字段树
  if (apiContext?.apiMethod && apiContext?.apiUri) {
    treeLoading.value = true
    entityFieldResolve({ method: apiContext.apiMethod, uri: apiContext.apiUri }).then(res => {
      const data = res.data
      if (data && data.entityClass && data.fieldTree) {
        // 有实体类和字段树
        resolvedEntityName.value = data.entityClass.simpleName
        fieldTreeData.value = data.fieldTree
        // R未指定泛型时，同时有 message 和 fieldTree
        if (data.message) {
          resolveMessage.value = data.message
        }
      } else if (data && data.message) {
        // 仅有提示信息，无字段树
        resolveMessage.value = data.message
      } else {
        resolveMessage.value = '无法获取接口返回结构，请手动维护字段'
      }
    }).catch(() => {
      // 接口异常
      resolveMessage.value = '无法获取接口返回结构，请手动维护字段'
    }).finally(() => {
      treeLoading.value = false
    })
  } else {
    resolveMessage.value = '缺少接口信息，无法自动获取返回结构'
  }
}

// 勾选节点
function handleCheck(data, { checkedNodes }) {
  // check-strictly 模式下只允许单选
  if (checkedNodes.length > 1) {
    fieldTreeRef.value.setCheckedKeys([data.jsonPath])
  }
  checkedNode.value = data
}

// 确认选择
function handleConfirm() {
  if (!checkedNode.value) return
  emit('confirm', {
    fieldName: checkedNode.value.fieldName,
    fieldDesc: checkedNode.value.fieldDesc,
    jsonPath: checkedNode.value.jsonPath
  })
  open.value = false
}
</script>

<style scoped>
.field-section {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
  background-color: #f5f7fa;
  min-height: 460px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.field-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.field-path {
  font-size: 12px;
  color: #909399;
}
</style>
