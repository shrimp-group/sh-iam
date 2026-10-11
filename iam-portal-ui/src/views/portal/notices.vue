<template>
  <div class="app-container">
    <h2>公告列表</h2>
    <p>这是一个公告列表的示例页面。</p>
    <el-alert
      title="提示"
      type="info"
      description="这里将显示系统的所有公告，包括标题、发布时间、内容等信息。"
      show-icon
      :closable="false"
    ></el-alert>
  </div>
</template>

<script setup name="PortalNotices">
// 公告列表页面
</script>

<style scoped>
.app-container {
  padding: 20px;
}
</style>