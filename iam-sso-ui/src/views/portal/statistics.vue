<template>
  <div class="app-container">
    <h2>统计数据</h2>
    <p>这是一个统计数据的示例页面。</p>
    <el-alert
      title="提示"
      type="info"
      description="这里将显示系统的统计数据，包括用户数量、登录次数、操作次数等信息。"
      show-icon
      :closable="false"
    ></el-alert>
  </div>
</template>

<script setup name="PortalStatistics">
// 统计数据页面
</script>

<style scoped>
.app-container {
  padding: 20px;
}
</style>