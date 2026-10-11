<template>
  <div class="app-container">
    <h2>修改密码</h2>
    <p>这是一个修改密码的示例页面。</p>
    <el-alert
      title="提示"
      type="info"
      description="请输入旧密码和新密码来修改您的密码。"
      show-icon
      :closable="false"
    ></el-alert>
  </div>
</template>

<script setup name="ChangePassword">
// 修改密码页面
</script>

<style scoped>
.app-container {
  padding: 20px;
}
</style>