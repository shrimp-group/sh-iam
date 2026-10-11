<template>
  <div class="app-container">
    <h2>待办事项</h2>
    <p>这是一个待办事项的示例页面。</p>
    <el-alert
      title="提示"
      type="info"
      description="这里将显示您的所有待办事项，包括事项名称、优先级、截止时间等信息。"
      show-icon
      :closable="false"
    ></el-alert>
  </div>
</template>

<script setup name="PortalTodoList">
// 待办事项页面
</script>

<style scoped>
.app-container {
  padding: 20px;
}
</style>