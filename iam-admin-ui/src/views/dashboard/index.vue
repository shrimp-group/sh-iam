<template>
  <div class="dashboard-container">
    <!-- 页面头部 -->
    <div class="dashboard-header">
      <div class="header-left">
        <h1>系统仪表盘</h1>
        <p>欢迎使用 IAM 授权中心系统</p>
      </div>
      <div class="header-right">
        <el-date-picker
          v-model="currentDate"
          type="datetime"
          format="YYYY-MM-DD HH:mm"
          value-format="YYYY-MM-DD HH:mm"
          placeholder="选择日期时间"
          style="margin-right: 10px"
        />
        <el-button type="primary" icon="RefreshRight" circle @click="handleRefresh" />
      </div>
    </div>

    <!-- 快速操作 -->
    <div class="dashboard-section">
      <QuickActions />
    </div>

    <!-- 统计卡片 -->
    <div class="dashboard-section">
      <StatsCard />
    </div>

    <!-- 主要内容区域 -->
    <div class="dashboard-content">
      <el-row :gutter="20">
        <!-- 最近活动 -->
        <el-col :span="12">
          <div class="content-section">
            <RecentActivity />
          </div>
        </el-col>
        <!-- 系统状态 -->
        <el-col :span="12">
          <div class="content-section">
            <SystemStatus />
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { RefreshRight } from '@element-plus/icons-vue'
import QuickActions from './components/QuickActions.vue'
import StatsCard from './components/StatsCard.vue'
import RecentActivity from './components/RecentActivity.vue'
import SystemStatus from './components/SystemStatus.vue'

const currentDate = ref(new Date())

const handleRefresh = () => {
  // 模拟刷新数据
  currentDate.value = new Date()
  console.log('刷新数据')
}
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header-left h1 {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 600;
  color: #333;
}

.header-left p {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.dashboard-section {
  margin-bottom: 20px;
}

.dashboard-content {
  margin-top: 20px;
}

.content-section {
  height: 400px;
}
</style>