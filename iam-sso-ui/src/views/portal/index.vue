<template>
  <div class="app-container">
    <h2>门户网站首页</h2>
    <p>这是一个门户网站首页的示例页面。</p>
    <el-alert
      title="提示"
      type="info"
      description="这里将显示门户网站的首页数据，包括系统状态、最新公告等信息。"
      show-icon
      :closable="false"
    ></el-alert>
  </div>
</template>

<script setup name="PortalIndex">
// 门户网站首页页面
</script>

<style scoped>
.app-container {
  padding: 20px;
}
</style>