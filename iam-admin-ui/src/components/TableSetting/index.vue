<template>
  <div>
    <span>操作</span>
    <el-tooltip class="item" effect="dark" content="显隐列" placement="top" v-if="columns">
      <el-dropdown trigger="click" :hide-on-click="false" style="padding-left: 12px">
        <el-button circle icon="Tools" size="small"/>
        <template #dropdown>
          <el-dropdown-menu>
            <template v-for="(item, key) in columns" :key="item.key">
              <el-dropdown-item>
                <el-checkbox :checked="item.visible" @change="checkboxChange($event, key)" :label="item.label" />
              </el-dropdown-item>
            </template>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </el-tooltip>
  </div>
</template>

<script setup>
const props = defineProps({
  columns: {
    type: Object,
  }
})
function checkboxChange(event, key) {
  props.columns[key].visible = event;
}
</script>

<style lang='scss' scoped>
:deep(.el-dropdown-menu__item) {
  line-height: 30px;
  padding: 0 17px;
}
</style>
