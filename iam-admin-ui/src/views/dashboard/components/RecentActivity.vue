<template>
  <div class="activity-card">
    <div class="card-header">
      <h3>最近活动</h3>
      <el-button type="text" size="small">查看全部</el-button>
    </div>
    <div class="card-body">
      <div v-if="activities.length === 0" class="empty-state">
        <el-empty description="暂无活动记录" />
      </div>
      <div v-else class="activity-list">
        <div v-for="activity in activities" :key="activity.id" class="activity-item">
          <div class="activity-avatar">
            <el-avatar :size="40" :src="activity.avatar">{{ activity.user.charAt(0) }}</el-avatar>
          </div>
          <div class="activity-content">
            <div class="activity-info">
              <span class="activity-user">{{ activity.user }}</span>
              <span class="activity-action">{{ activity.action }}</span>
              <span class="activity-target">{{ activity.target }}</span>
            </div>
            <div class="activity-time">
              <el-icon><Clock /></el-icon>
              <span>{{ activity.time }}</span>
            </div>
          </div>
          <div class="activity-status" :class="`status-${activity.status}`">
            {{ activity.statusText }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Clock } from '@element-plus/icons-vue'

// 模拟数据
const activities = ref([
  {
    id: 1,
    user: '张三',
    avatar: '',
    action: '创建了',
    target: '管理员角色',
    time: '2分钟前',
    status: 'success',
    statusText: '成功'
  },
  {
    id: 2,
    user: '李四',
    avatar: '',
    action: '更新了',
    target: '用户管理权限',
    time: '15分钟前',
    status: 'success',
    statusText: '成功'
  },
  {
    id: 3,
    user: '王五',
    avatar: '',
    action: '删除了',
    target: '测试资源',
    time: '1小时前',
    status: 'warning',
    statusText: '警告'
  },
  {
    id: 4,
    user: '赵六',
    avatar: '',
    action: '登录了系统',
    target: '',
    time: '2小时前',
    status: 'info',
    statusText: '信息'
  },
  {
    id: 5,
    user: '孙七',
    avatar: '',
    action: '授权了',
    target: '开发人员权限',
    time: '3小时前',
    status: 'success',
    statusText: '成功'
  }
])
</script>

<style scoped>
.activity-card {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 20px;
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.card-body {
  height: calc(100% - 50px);
  overflow-y: auto;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.activity-item:hover {
  background-color: #f5f7fa;
}

.activity-avatar {
  flex-shrink: 0;
}

.activity-content {
  flex: 1;
  min-width: 0;
}

.activity-info {
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 4px;
}

.activity-user {
  font-weight: 600;
  color: #333;
}

.activity-action {
  color: #666;
  margin: 0 4px;
}

.activity-target {
  color: #409eff;
  font-weight: 500;
}

.activity-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.activity-status {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.status-success {
  background-color: #f0f9eb;
  color: #67c23a;
}

.status-warning {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.status-info {
  background-color: #ecf5ff;
  color: #409eff;
}
</style>