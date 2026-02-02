<template>
  <div class="status-card">
    <div class="card-header">
      <h3>系统状态</h3>
      <el-dropdown>
        <span class="dropdown-trigger">
          {{ timeRange }}
          <el-icon class="el-icon--right"><CaretBottom /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="timeRange = '实时'">实时</el-dropdown-item>
            <el-dropdown-item @click="timeRange = '今日'">今日</el-dropdown-item>
            <el-dropdown-item @click="timeRange = '本周'">本周</el-dropdown-item>
            <el-dropdown-item @click="timeRange = '本月'">本月</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
    <div class="card-body">
      <div class="status-grid">
        <div v-for="status in statusList" :key="status.key" class="status-item">
          <div class="status-label">{{ status.label }}</div>
          <div class="status-value">{{ status.value }}</div>
          <el-progress
            :percentage="status.percentage"
            :stroke-width="6"
            :color="status.color"
            :show-text="false"
          />
          <div class="status-desc" :class="status.descClass">
            {{ status.desc }}
          </div>
        </div>
      </div>
      <div class="system-alerts">
        <h4>系统告警</h4>
        <div v-if="alerts.length === 0" class="empty-alerts">
          <el-empty description="暂无告警信息" size="small" />
        </div>
        <div v-else class="alert-list">
          <el-alert
            v-for="alert in alerts"
            :key="alert.id"
            :type="alert.type"
            :title="alert.title"
            :description="alert.description"
            :closable="false"
            :show-icon="true"
            effect="light"
            style="margin-bottom: 10px"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { CaretBottom } from '@element-plus/icons-vue'

const timeRange = ref('实时')

// 模拟数据
const statusList = ref([
  {
    key: 'cpu',
    label: 'CPU 使用率',
    value: '35%',
    percentage: 35,
    color: '#409eff',
    desc: '正常',
    descClass: 'text-success'
  },
  {
    key: 'memory',
    label: '内存使用率',
    value: '62%',
    percentage: 62,
    color: '#e6a23c',
    desc: '警告',
    descClass: 'text-warning'
  },
  {
    key: 'disk',
    label: '磁盘使用率',
    value: '48%',
    percentage: 48,
    color: '#67c23a',
    desc: '正常',
    descClass: 'text-success'
  },
  {
    key: 'network',
    label: '网络流量',
    value: '12.5 MB/s',
    percentage: 75,
    color: '#f56c6c',
    desc: '繁忙',
    descClass: 'text-danger'
  }
])

const alerts = ref([
  {
    id: 1,
    type: 'warning',
    title: '内存使用率警告',
    description: '内存使用率已超过 60%，建议及时清理'
  },
  {
    id: 2,
    type: 'info',
    title: '系统更新提醒',
    description: '发现新版本，建议在非高峰时段更新'
  }
])
</script>

<style scoped>
.status-card {
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

.dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: #409eff;
  cursor: pointer;
}

.card-body {
  height: calc(100% - 50px);
  display: flex;
  flex-direction: column;
}

.status-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.status-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.status-label {
  font-size: 14px;
  color: #666;
}

.status-value {
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.status-desc {
  font-size: 12px;
  font-weight: 500;
  text-align: right;
}

.text-success {
  color: #67c23a;
}

.text-warning {
  color: #e6a23c;
}

.text-danger {
  color: #f56c6c;
}

.system-alerts {
  flex: 1;
  overflow-y: auto;
  margin-top: 10px;
}

.system-alerts h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.empty-alerts {
  padding: 20px 0;
}

.alert-list {
  max-height: 200px;
  overflow-y: auto;
}
</style>