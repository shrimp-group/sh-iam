<template>
  <el-dialog :title="title" v-model="open" width="600px" :close-on-click-modal="false">
    <el-form ref="editRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="JSON路径" prop="jsonPath">
        <div style="display: flex; gap: 8px; width: 100%;">
          <el-input v-model="form.jsonPath" placeholder="请输入JSON路径，如 $.data.salary" style="flex: 1;" />
          <el-button type="primary" plain @click="openFieldTree">选择字段</el-button>
        </div>
        <form-tip text="JSONPath 表达式，如 $.data.salary，可通过选择字段自动填充"/>
      </el-form-item>
      <el-form-item label="字段名称" prop="fieldName">
        <el-input v-model="form.fieldName" placeholder="请输入字段名称" />
      </el-form-item>
      <el-form-item label="权限动作" prop="action">
        <el-radio-group v-model="form.action">
          <el-radio-button label="HIDDEN">隐藏</el-radio-button>
          <el-radio-button label="MASK">脱敏</el-radio-button>
          <el-radio-button label="READ_ONLY">只读</el-radio-button>
        </el-radio-group>
        <form-tip html="HIDDEN: 字段不返回; MASK: 字段脱敏显示; READ_ONLY: 字段只读"/>
      </el-form-item>
      <el-form-item v-if="form.action === 'MASK'" label="脱敏规则" prop="maskRule">
        <el-input v-model="form.maskRule" placeholder="请输入脱敏规则，如 手机号: 3,4 邮箱: 1,*@*" />
        <form-tip html="脱敏规则格式：<br>手机号: 3,4 (前3后4可见)<br>邮箱: 1,*@* (前1可见，@后可见)<br>身份证: 3,4 (前3后4可见)"/>
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input v-model="form.description" placeholder="请输入描述" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </template>

    <!-- 字段树选择器 -->
    <field-tree-select ref="fieldTreeRef" @confirm="handleFieldSelect" />
  </el-dialog>
</template>

<script setup name="ApiFieldEdit">
import { apiFieldCreate, apiFieldUpdate } from '@/api/system/api-field'
import FieldTreeSelect from './field-tree-select.vue'

defineExpose({ init })
const emit = defineEmits(['change'])

const { proxy } = getCurrentInstance()
const open = ref(false)
const title = ref('')
const form = ref({})
const fieldTreeRef = ref(null)

// 当前 API 的上下文信息
const apiContext = ref({})

const rules = ref({
  fieldName: [
    { required: true, message: '字段名称不能为空', trigger: 'blur' }
  ],
  jsonPath: [
    { required: true, message: 'JSON路径不能为空', trigger: 'blur' }
  ],
  action: [
    { required: true, message: '权限动作不能为空', trigger: 'change' }
  ],
  maskRule: [
    { required: true, message: '脱敏规则不能为空', trigger: 'blur' }
  ]
})

/** 表单重置 */
function reset() {
  form.value = {}
  proxy.resetForm('editRef')
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 初始化 */
function init(row, apiInfo) {
  reset()
  apiContext.value = apiInfo
  if (!row || !row.id) {
    open.value = true
    title.value = '新增字段权限'
    form.value.appCode = apiInfo?.appCode
    form.value.apiCode = apiInfo?.apiCode
    form.value.action = 'HIDDEN'
  } else {
    form.value = { ...row }
    open.value = true
    title.value = '修改字段权限'
  }
}

/** 打开字段树选择器 */
function openFieldTree() {
  fieldTreeRef.value.init(apiContext.value)
}

/** 字段树选择回调 */
function handleFieldSelect(field) {
  form.value.jsonPath = field.jsonPath
  // 字段名称格式: 字段名-中文名
  const desc = field.fieldDesc ? '-' + field.fieldDesc : ''
  form.value.fieldName = field.fieldName + desc
  if (field.fieldCode) {
    form.value.fieldCode = field.fieldCode
  }
}

/** 提交表单 */
function submitForm() {
  proxy.$refs['editRef'].validate(valid => {
    if (valid) {
      // MASK 动作必须填写脱敏规则
      if (form.value.action === 'MASK' && !form.value.maskRule) {
        proxy.$modal.msgWarning('脱敏规则不能为空')
        return
      }
      if (form.value.id) {
        apiFieldUpdate(form.value).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          emit('change', true)
        })
      } else {
        apiFieldCreate(form.value).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          emit('change', true)
        })
      }
    }
  })
}
</script>
