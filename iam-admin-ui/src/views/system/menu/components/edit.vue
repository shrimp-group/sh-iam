<template>
  <el-dialog :title="title" v-model="open" width="1200px">
    <el-form ref="editRef" :model="form" :rules="rules" label-width="80px">
      <el-row :gutter="20">
        <el-col :span="10">
          <el-form-item label="父菜单" prop="parentCode">
            <div class="parent-breadcrumb">
              <el-breadcrumb separator-class="el-icon-arrow-right" class="menu-breadcrumb">
                <el-breadcrumb-item v-for="(item, index) in parents" :key="index" class="menu-breadcrumb-item">
                  <svg-icon v-if="item.icon" :icon-class="item.icon" class="menu-breadcrumb-icon" />
                  <span>{{ item.menuName }}</span>
                </el-breadcrumb-item>
              </el-breadcrumb>
            </div>
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="应用编码" prop="appCode">
                <el-input v-model="form.appCode" placeholder="请输入应用编码" disabled/>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="菜单类型" prop="menuType">
                <el-radio-group v-model="form.menuType">
                  <el-radio-button v-for="item in MENU_TYPE" :key="item.value" :label="item.label" :value="item.value" />
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="form.sort" placeholder="请输入排序" />
            <form-tip text="数值越小越靠前"/>
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="form.remark" placeholder="请输入备注" type="textarea" :rows="10"/>
          </el-form-item>
        </el-col>
        <el-col :span="14">
          <div v-if="form.menuType === 'MENU'">
            <el-form-item label="菜单名称" prop="menuName">
              <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
              <form-tip text="菜单的显示名称，用于前端展示"/>
            </el-form-item>
            <el-form-item label="路由地址" prop="routePath">
              <el-input v-model="form.routePath" placeholder="请输入路由地址" />
              <form-tip html="前端路由路径，例如：/system/user<br>路由需满足 Vue-Router 组件要求"/>
            </el-form-item>
            <el-form-item label="组件" prop="component">
              <div style="display: flex; gap: 8px; align-items: center;">
                <el-input v-model="form.component" placeholder="请输入组件" style="flex: 1; width: 450px;" />
                <el-select v-model="quickComponent" placeholder="快捷填充" @change="selectComponent" clearable style="width: 120px;">
                  <el-option label="Layout" value="Layout" />
                  <el-option label="ParentView" value="ParentView" />
                </el-select>
              </div>
              <form-tip html="
              1. 组件路径，例如：system/user/index<br>
              2. 【Layout】组件表示管理页面框架结构。使用在一级目录 <br>
              3. 【ParentView】组件表示管理页面框架结构。使用在非一级，非叶子级目录 <br>
              4. 【自定义组件】路径为代码中，view(之后) 的路径，可省略 .vue 后缀
              "/>
            </el-form-item>
            <el-form-item label="图标" prop="icon">
              <div style="display: flex; gap: 8px; align-items: center;">
                <el-input v-model="form.icon" placeholder="请输入图标" style="flex: 1;">
                  <template #prefix>
                    <svg-icon v-if="form.icon" :icon-class="form.icon" class="el-input__icon" style="height: 32px;width: 16px;" />
                    <el-icon v-else style="height: 32px;width: 16px;"><search /></el-icon>
                  </template>
                </el-input>
                <el-popover placement="bottom" :width="400" trigger="click">
                  <template #reference>
                    <el-button type="primary" plain size="small">选择图标</el-button>
                  </template>
                  <IconSelect :active-icon="form.icon" @selected="(icon) => form.icon = icon"/>
                </el-popover>
              </div>
              <form-tip text="菜单图标，使用Element Plus图标名称"/>
            </el-form-item>
            <el-form-item label="隐藏" prop="hidden">
              <el-radio-group v-model="form.hidden">
                <el-radio-button v-for="item in BOOLEAN" :key="item.value" :label="item.label" :value="item.value" />
              </el-radio-group>
              <form-tip html="隐藏后，菜单功能只能被页面跳转打开，无法从菜单中点击打开!"/>
            </el-form-item>
          </div>

          <div v-if="form.menuType === 'BUTTON'">
            <el-form-item label="按钮名称" prop="menuName">
              <el-input v-model="form.menuName" placeholder="请输入按钮名称" />
              <form-tip text="按钮名称，用于标识按钮"/>
            </el-form-item>
            <el-form-item label="按钮编码" prop="buttonCode">
              <el-input v-model="form.buttonCode" placeholder="请输入按钮编码" />
              <form-tip text="同一菜单下，按钮编码需要唯一"/>
            </el-form-item>

            <el-form-item>
              <span>快捷填充:</span>
              <div style="margin-top: 8px; display: flex; gap: 8px; flex-wrap: wrap;">
                <el-button type="primary" plain size="small" @click="fillButton('分页', 'page')">分页</el-button>
                <el-button type="primary" plain size="small" @click="fillButton('列表', 'list')">列表</el-button>
                <el-button type="primary" plain size="small" @click="fillButton('新增', 'create')">新增</el-button>
                <el-button type="primary" plain size="small" @click="fillButton('修改', 'update')">修改</el-button>
                <el-button type="primary" plain size="small" @click="fillButton('保存', 'save')">保存</el-button>
                <el-button type="primary" plain size="small" @click="fillButton('删除', 'remove')">删除</el-button>
                <el-button type="primary" plain size="small" @click="fillButton('导入', 'import')">导入</el-button>
                <el-button type="primary" plain size="small" @click="fillButton('导出', 'export')">导出</el-button>
              </div>
            </el-form-item>

          </div>
        </el-col>
      </el-row>

    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="IamMenuEdit">
import {menuInfo, menuCreate, menuUpdate} from "@/api/system/menu";
import IconSelect from "@/components/IconSelect/index.vue";

defineExpose({init});
const emit = defineEmits(["change"]);
const { proxy } = getCurrentInstance();
const { BOOLEAN, MENU_TYPE } = proxy.useDict("BOOLEAN", "MENU_TYPE");
const open = ref(false);
const title = ref("");
const form = ref({});
const parents = ref([]);
const quickComponent = ref('');
const rules = ref({
  appCode: [
    { required: true, message: "应用编码不能为空", trigger: "blur" },
  ],
  parentCode: [
    { required: true, message: "父菜单不能为空", trigger: "blur" }
  ],
  menuName: [
    { required: true, message: "菜单名称不能为空", trigger: "blur" },
    { min: 1, max: 50, message: "菜单名称长度应在 1-50 个字符之间", trigger: "blur" }
  ],
  icon: [
    { required: true, message: "图标不能为空", trigger: "blur" }
  ],
  menuType: [
    { required: true, message: "菜单类型不能为空", trigger: "blur" },
  ],
  buttonCode: [
    { required: true, message: "按钮编码不能为空", trigger: "blur" },
  ],
  component: [
    { required: true, message: "页面组件不能为空", trigger: "blur" },
  ],
  routePath: [
    { required: true, message: "路由地址不能为空", trigger: "blur" },
  ],
  hidden: [
    { required: true, message: "隐藏不能为空", trigger: "blur" }
  ],
  sort: [
    { required: true, message: "排序不能为空", trigger: "blur" }
  ],
  remark: [
    { max: 200, message: "备注长度不能超过 200 个字符", trigger: "blur" }
  ]
})

/** 表单重置 */
function reset() {
  form.value = {};
  parents.value = [];
  quickComponent.value = '';
  proxy.resetForm("editRef");
}

/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
}

// 新增/修改按钮操作
function init(row) {
  reset();
  parents.value = row.parents;
  if (!row || !row.id) {
    open.value = true;
    title.value = "添加";
    form.value.appCode = row?.appCode;
    form.value.menuType = 'MENU';
    form.value.sort = 99;
    form.value.hidden = 0;
    form.value.parentCode = row.parentCode;
  } else {
    menuInfo({id: row.id}).then(res => {
      form.value = res.data;
      open.value = true;
      title.value = "修改";
    });
  }
}

/** 选择组件 */
function selectComponent() {
  if (quickComponent.value) {
    form.value.component = quickComponent.value;
  }
}

/** 填充按钮信息 */
function fillButton(name, code) {
  form.value.menuName = name;
  form.value.buttonCode = code;
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["editRef"].validate(valid => {
    if (valid) {
      if (form.value.id) {
        menuUpdate(form.value).then(res => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          emit("change", true);
        });
      } else {
        menuCreate(form.value).then(res => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          emit("change", true);
        });
      }
    }
  });
}
</script>

<style scoped lang="scss">
.menu-breadcrumb {
  font-size: 14px;
  padding: 8px 16px;
  background-color: #f9fafc;
  border-radius: 6px;
  border: 1px solid #e9ecef;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  transition: all 0.3s ease;
}

.menu-breadcrumb-item {
  color: #495057;
  font-weight: 500;
  display: flex;
  align-items: center;
}

.menu-breadcrumb-icon {
  margin-right: 6px;
  color: #409eff;
  font-size: 12px;
}

</style>

