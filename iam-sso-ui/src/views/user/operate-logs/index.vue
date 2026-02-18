<template>
  <div class="app-container">
    <h2>操作日志</h2>
    <p>这是一个操作日志的示例页面。</p>
    <el-alert
      title="提示"
      type="info"
      description="这里将显示您的所有操作日志，包括操作时间、操作内容等信息。"
      show-icon
      :closable="false"
    ></el-alert>
  </div>
</template>

<script setup name="OperateLogs">
// 操作日志页面
</script>

<style scoped>
.app-container {
  padding: 20px;
}
</style>