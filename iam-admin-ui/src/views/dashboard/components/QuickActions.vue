<template>
  <div class="quick-actions-card">
    <div class="card-header">
      <h3>快速操作</h3>
    </div>
    <div class="card-body">
      <div class="actions-grid">
        <div v-for="action in actions" :key="action.key" class="action-item" @click="handleAction(action)">
          <div class="action-icon" :class="action.iconClass">
            <component :is="action.icon" />
          </div>
          <div class="action-label">{{ action.label }}</div>
          <div class="action-desc">{{ action.desc }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import {
  Plus, User, Lock, DocumentCopy,
  Setting, Lock as Shield, Bell, Document
} from '@element-plus/icons-vue'

const router = useRouter()

const actions = [
  {
    key: 'create-user',
    icon: Plus,
    iconClass: 'bg-blue',
    label: '创建用户',
    desc: '添加新用户账号',
    route: '/user/list'
  },
  {
    key: 'create-role',
    icon: User,
    iconClass: 'bg-green',
    label: '创建角色',
    desc: '新建角色并分配权限',
    route: '/user/role'
  },
  {
    key: 'create-app',
    icon: DocumentCopy,
    iconClass: 'bg-orange',
    label: '创建应用',
    desc: '添加新应用',
    route: '/system/app'
  },
  {
    key: 'create-api',
    icon: Lock,
    iconClass: 'bg-purple',
    label: '创建接口',
    desc: '添加新接口',
    route: '/system/api'
  },
  {
    key: 'menu-management',
    icon: Setting,
    iconClass: 'bg-cyan',
    label: '菜单管理',
    desc: '配置系统菜单',
    route: '/system/menu'
  },
  {
    key: 'data-dimension',
    icon: Shield,
    iconClass: 'bg-red',
    label: '数据维度',
    desc: '管理数据维度',
    route: '/system/data-dimension'
  },
  {
    key: 'ak-management',
    icon: Bell,
    iconClass: 'bg-yellow',
    label: '密钥管理',
    desc: '管理访问密钥',
    route: '/system/ak'
  },
  {
    key: 'audit-logs',
    icon: Document,
    iconClass: 'bg-gray',
    label: '审计日志',
    desc: '查看操作记录',
    route: '/log/login'
  }
]

const handleAction = (action) => {
  if (action.route) {
    router.push(action.route)
  }
}
</script>

<style scoped>
.quick-actions-card {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-bottom: 20px;
}

.card-header {
  margin-bottom: 15px;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 16px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
  background-color: #f9f9f9;
  transition: all 0.3s ease;
  cursor: pointer;
  text-align: center;
}

.action-item:hover {
  background-color: #f0f0f0;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.action-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  font-size: 24px;
  color: #fff;
}

.action-icon.bg-blue {
  background-color: #409eff;
}

.action-icon.bg-green {
  background-color: #67c23a;
}

.action-icon.bg-purple {
  background-color: #909399;
}

.action-icon.bg-orange {
  background-color: #e6a23c;
}

.action-icon.bg-cyan {
  background-color: #13c2c2;
}

.action-icon.bg-red {
  background-color: #f56c6c;
}

.action-icon.bg-yellow {
  background-color: #fadb14;
}

.action-icon.bg-gray {
  background-color: #606266;
}

.action-label {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.action-desc {
  font-size: 12px;
  color: #909399;
}
</style>