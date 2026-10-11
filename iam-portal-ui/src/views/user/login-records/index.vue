<template>
  <div class="app-container">
    <h2>登录记录</h2>
    <p>这是一个登录记录的示例页面。</p>
    <el-alert
      title="提示"
      type="info"
      description="这里将显示您的所有登录记录，包括登录时间、登录IP等信息。"
      show-icon
      :closable="false"
    ></el-alert>
  </div>
</template>

<script setup name="LoginRecords">
// 登录记录页面
</script>

<style scoped>
.app-container {
  padding: 20px;
}
</style>