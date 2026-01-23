<template>
  <div class="app-container home">
    <!-- 欢迎信息和个人信息 -->
    <el-card class="welcome-card" shadow="hover">
      <div class="welcome-content">
        <h2>欢迎回来，管理员</h2>
        <p class="welcome-desc">今天是 {{ today }}，祝您工作愉快！</p>
        <!-- 个人信息 -->
        <div class="personal-info">
          <el-row :gutter="10">
            <el-col :span="8">
              <span class="info-label">用户角色：</span>
              <span class="info-value">{{ userInfo.role }}</span>
            </el-col>
            <el-col :span="8">
              <span class="info-label">部门：</span>
              <span class="info-value">{{ userInfo.department }}</span>
            </el-col>
            <el-col :span="8">
              <span class="info-label">最后登录：</span>
              <span class="info-value">{{ userInfo.lastLogin }}</span>
            </el-col>
          </el-row>
        </div>
      </div>
      <div class="user-info">
        <el-avatar :size="60" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png"></el-avatar>
      </div>
    </el-card>

    <!-- 统计数据卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :sm="24" :md="12" :lg="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <el-statistic
              title="总用户数"
              :value="stats.totalUsers"
              :precision="0"
              value-style="{ color: '#3f8600' }"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-statistic>
            <div class="stat-desc">较昨日增长 {{ stats.userGrowth }}%</div>
          </div>
        </el-card>
      </el-col>
      <el-col :sm="24" :md="12" :lg="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <el-statistic
              title="今日登录"
              :value="stats.todayLogin"
              :precision="0"
              value-style="{ color: '#1890ff' }"
            >
              <template #prefix>
                <el-icon><Monitor /></el-icon>
              </template>
            </el-statistic>
            <div class="stat-desc">较昨日增长 {{ stats.loginGrowth }}%</div>
          </div>
        </el-card>
      </el-col>
      <el-col :sm="24" :md="12" :lg="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <el-statistic
              title="系统公告"
              :value="stats.notices"
              :precision="0"
              value-style="{ color: '#722ed1' }"
            >
              <template #prefix>
                <el-icon><Bell /></el-icon>
              </template>
            </el-statistic>
            <div class="stat-desc">未读 {{ stats.unreadNotices }} 条</div>
          </div>
        </el-card>
      </el-col>
      <el-col :sm="24" :md="12" :lg="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <el-statistic
              title="待办事项"
              :value="stats.todoItems"
              :precision="0"
              value-style="{ color: '#eb2f96' }"
            >
              <template #prefix>
                <el-icon><Document /></el-icon>
              </template>
            </el-statistic>
            <div class="stat-desc">紧急 {{ stats.urgentTodo }} 项</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <el-card class="shortcut-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>快捷操作</span>
          <el-button type="text" size="small">更多</el-button>
        </div>
      </template>
      <el-row :gutter="20">
        <el-col :sm="12" :md="8" :lg="6" v-for="item in shortcuts" :key="item.id">
          <div class="shortcut-item" @click="handleShortcut(item)">
            <el-icon :size="32">
              <component :is="item.icon" />
            </el-icon>
            <span>{{ item.name }}</span>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 最新公告和待办事项 -->
    <el-row :gutter="20" class="content-row">
      <!-- 最新公告 -->
      <el-col :sm="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>最新公告</span>
              <el-button type="text" size="small">查看全部</el-button>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="notice in notices"
              :key="notice.id"
              :timestamp="notice.time"
              placement="top"
            >
              <el-card :body-style="{ padding: '10px' }">
                <h4>{{ notice.title }}</h4>
                <p class="notice-content">{{ notice.content }}</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>

      <!-- 待办事项 -->
      <el-col :sm="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>待办事项</span>
              <el-button type="text" size="small">查看全部</el-button>
            </div>
          </template>
          <div class="todo-list">
            <div
              v-for="todo in todos"
              :key="todo.id"
              class="todo-item"
              :class="{ 'todo-urgent': todo.urgent }"
            >
              <div class="todo-header">
                <span class="todo-title">{{ todo.title }}</span>
                <el-tag :type="todo.urgent ? 'danger' : 'warning'" size="small">
                  {{ todo.urgent ? '紧急' : '普通' }}
                </el-tag>
              </div>
              <div class="todo-content">{{ todo.content }}</div>
              <div class="todo-actions">
                <el-button type="primary" size="small" @click="handleTodo(todo)">
                  处理
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 系统信息 -->
    <el-card class="system-info-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>系统信息</span>
        </div>
      </template>
      <el-row :gutter="20">
        <el-col :sm="24" :md="12" :lg="6">
          <div class="system-info-item">
            <span class="info-label">系统版本：</span>
            <span class="info-value">{{ systemInfo.version }}</span>
          </div>
        </el-col>
        <el-col :sm="24" :md="12" :lg="6">
          <div class="system-info-item">
            <span class="info-label">运行环境：</span>
            <span class="info-value">{{ systemInfo.env }}</span>
          </div>
        </el-col>
        <el-col :sm="24" :md="12" :lg="6">
          <div class="system-info-item">
            <span class="info-label">最后更新：</span>
            <span class="info-value">{{ systemInfo.lastUpdate }}</span>
          </div>
        </el-col>
        <el-col :sm="24" :md="12" :lg="6">
          <div class="system-info-item">
            <span class="info-label">技术支持：</span>
            <span class="info-value">{{ systemInfo.support }}</span>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup name="Index">
import { ref, onMounted, markRaw } from 'vue'
import { User, Monitor, Bell, Document, Setting, Plus, Search, Upload, Menu } from '@element-plus/icons-vue'

// 今天的日期
const today = ref('')

// 用户信息
const userInfo = ref({
  role: '超级管理员',
  department: '技术部',
  lastLogin: '2023-06-15 10:30:00'
})

// 统计数据
const stats = ref({
  totalUsers: 1234,
  userGrowth: 5.2,
  todayLogin: 234,
  loginGrowth: 8.7,
  notices: 12,
  unreadNotices: 3,
  todoItems: 8,
  urgentTodo: 2
})

// 快捷操作
const shortcuts = ref([
  { id: 1, name: '新建用户', icon: markRaw(Plus), action: 'createUser' },
  { id: 2, name: '用户管理', icon: markRaw(User), action: 'manageUsers' },
  { id: 3, name: '角色管理', icon: markRaw(Setting), action: 'manageRoles' },
  { id: 4, name: '菜单管理', icon: markRaw(Menu), action: 'manageMenus' },
  { id: 5, name: '日志查询', icon: markRaw(Search), action: 'searchLogs' },
  { id: 6, name: '数据统计', icon: markRaw(Monitor), action: 'statistics' },
  { id: 7, name: '系统设置', icon: markRaw(Setting), action: 'systemSettings' },
  { id: 8, name: '文件上传', icon: markRaw(Upload), action: 'uploadFiles' }
])

// 最新公告
const notices = ref([
  { id: 1, title: '系统升级公告', content: '系统将于今晚23:00-次日01:00进行升级维护，期间系统可能会出现短暂不可用。', time: '2023-06-15 10:30' },
  { id: 2, title: '新功能上线', content: '新增了用户权限管理功能，管理员可以更精细地控制用户权限。', time: '2023-06-14 14:20' },
  { id: 3, title: '安全提醒', content: '请所有用户及时修改初始密码，并定期更换密码，确保账户安全。', time: '2023-06-13 09:15' }
])

// 待办事项
const todos = ref([
  { id: 1, title: '审核用户注册申请', content: '有5位新用户提交了注册申请，需要审核。', urgent: true },
  { id: 2, title: '更新系统文档', content: '更新系统使用手册，添加新功能的说明。', urgent: false },
  { id: 3, title: '处理用户反馈', content: '处理最近收到的用户反馈，解决相关问题。', urgent: true },
  { id: 4, title: '每周工作总结', content: '撰写本周工作总结，提交给上级领导。', urgent: false }
])

// 系统信息
const systemInfo = ref({
  version: '3.9.1',
  env: '生产环境',
  lastUpdate: '2023-06-15',
  support: '技术支持团队'
})

// 处理快捷操作
const handleShortcut = (item) => {
  console.log('快捷操作：', item.action)
  // 这里可以添加具体的操作逻辑，比如跳转到相应的页面
}

// 处理待办事项
const handleTodo = (todo) => {
  console.log('处理待办事项：', todo.id)
  // 这里可以添加处理待办事项的逻辑
}

// 初始化数据
onMounted(() => {
  // 设置今天的日期
  const now = new Date()
  today.value = now.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
})
</script>

<style scoped lang="scss">
.home {
  padding: 10px;
  background-color: #f5f7fa;
  min-height: 100vh;

  .welcome-card {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 10px;
    padding: 15px;

    .welcome-content {
      flex: 1;

      h2 {
        margin: 0 0 5px 0;
        font-size: 20px;
        font-weight: 600;
        color: #303133;
      }

      .welcome-desc {
        margin: 0 0 10px 0;
        font-size: 14px;
        color: #606266;
      }

      .personal-info {
        margin-top: 10px;
        padding-top: 10px;
        border-top: 1px solid #ebeef5;

        .info-label {
          color: #909399;
          font-size: 13px;
          margin-right: 5px;
        }

        .info-value {
          color: #303133;
          font-size: 13px;
          font-weight: 500;
        }
      }
    }

    .user-info {
      display: flex;
      align-items: center;
      margin-left: 15px;
    }
  }

  .stats-row {
    margin-bottom: 10px;

    .stat-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 5px 0;

      .stat-desc {
        margin-top: 5px;
        font-size: 11px;
        color: #909399;
      }
    }
  }

  .shortcut-card {
    margin-bottom: 10px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 12px;
    }

    .shortcut-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 12px 0;
      cursor: pointer;
      transition: all 0.3s;
      border-radius: 6px;

      &:hover {
        background-color: #ecf5ff;
        transform: translateY(-2px);
      }

      span {
        margin-top: 6px;
        font-size: 12px;
        color: #606266;
      }
    }
  }

  .content-row {
    margin-bottom: 10px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 12px;
    }

    .notice-content {
      margin: 5px 0 0 0;
      font-size: 12px;
      color: #606266;
      line-height: 1.4;
    }

    .todo-list {
      .todo-item {
        padding: 10px;
        border-bottom: 1px solid #ebeef5;
        transition: all 0.3s;
        
        &:last-child {
          border-bottom: none;
        }
        
        &:hover {
          background-color: #f5f7fa;
        }
        
        &.todo-urgent {
          border-left: 4px solid #f56c6c;
        }
      }
      
      .todo-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 3px;

        .todo-title {
          font-weight: 500;
          font-size: 13px;
          color: #303133;
        }
      }

      .todo-content {
        font-size: 12px;
        color: #606266;
        line-height: 1.4;
        margin-bottom: 6px;
      }
      
      .todo-actions {
        display: flex;
        justify-content: flex-end;
      }
    }
  }

  .system-info-card {
    margin-bottom: 10px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 12px;
    }

    .system-info-item {
      padding: 6px 0;
      display: flex;
      align-items: center;

      .info-label {
        color: #909399;
        margin-right: 8px;
        font-size: 12px;
      }

      .info-value {
        color: #303133;
        font-weight: 500;
        font-size: 12px;
      }
    }
  }
}
</style>

