<template>
  <el-row :gutter="20">
    <el-col :span="6">
      <div class="stat-card" @click="handleClick('user')">
        <el-icon class="stat-icon"><User /></el-icon>
        <div class="stat-content">
          <div class="stat-title">用户总数</div>
          <div class="stat-value">{{ userCount }}</div>
          <div class="stat-change" :class="{ 'text-success': userChange > 0, 'text-danger': userChange < 0 }">
            <el-icon v-if="userChange > 0"><CaretTop /></el-icon>
            <el-icon v-else-if="userChange < 0"><CaretBottom /></el-icon>
            {{ Math.abs(userChange) }}%
          </div>
        </div>
      </div>
    </el-col>
    <el-col :span="6">
      <div class="stat-card" @click="handleClick('role')">
        <el-icon class="stat-icon"><Avatar /></el-icon>
        <div class="stat-content">
          <div class="stat-title">角色总数</div>
          <div class="stat-value">{{ roleCount }}</div>
          <div class="stat-change" :class="{ 'text-success': roleChange > 0, 'text-danger': roleChange < 0 }">
            <el-icon v-if="roleChange > 0"><CaretTop /></el-icon>
            <el-icon v-else-if="roleChange < 0"><CaretBottom /></el-icon>
            {{ Math.abs(roleChange) }}%
          </div>
        </div>
      </div>
    </el-col>
    <el-col :span="6">
      <div class="stat-card" @click="handleClick('permission')">
        <el-icon class="stat-icon"><Lock /></el-icon>
        <div class="stat-content">
          <div class="stat-title">权限总数</div>
          <div class="stat-value">{{ permissionCount }}</div>
          <div class="stat-change" :class="{ 'text-success': permissionChange > 0, 'text-danger': permissionChange < 0 }">
            <el-icon v-if="permissionChange > 0"><CaretTop /></el-icon>
            <el-icon v-else-if="permissionChange < 0"><CaretBottom /></el-icon>
            {{ Math.abs(permissionChange) }}%
          </div>
        </div>
      </div>
    </el-col>
    <el-col :span="6">
      <div class="stat-card" @click="handleClick('resource')">
        <el-icon class="stat-icon"><DocumentCopy /></el-icon>
        <div class="stat-content">
          <div class="stat-title">资源总数</div>
          <div class="stat-value">{{ resourceCount }}</div>
          <div class="stat-change" :class="{ 'text-success': resourceChange > 0, 'text-danger': resourceChange < 0 }">
            <el-icon v-if="resourceChange > 0"><CaretTop /></el-icon>
            <el-icon v-else-if="resourceChange < 0"><CaretBottom /></el-icon>
            {{ Math.abs(resourceChange) }}%
          </div>
        </div>
      </div>
    </el-col>
  </el-row>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Avatar, Lock, DocumentCopy, CaretTop, CaretBottom } from '@element-plus/icons-vue'

const router = useRouter()

// 模拟数据
const userCount = ref(128)
const userChange = ref(12)
const roleCount = ref(32)
const roleChange = ref(5)
const permissionCount = ref(256)
const permissionChange = ref(8)
const resourceCount = ref(512)
const resourceChange = ref(-3)

const handleClick = (type) => {
  switch (type) {
    case 'user':
      router.push('/user/list')
      break
    case 'role':
      router.push('/user/role')
      break
    case 'permission':
      router.push('/user/role')
      break
    case 'resource':
      router.push('/system/app')
      break
  }
}
</script>

<style scoped>
.stat-card {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 20px;
  display: flex;
  align-items: center;
  transition: all 0.3s ease;
  cursor: pointer;
}

.stat-card:hover {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.stat-icon {
  font-size: 32px;
  color: #409eff;
  margin-right: 20px;
}

.stat-content {
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 5px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 3px;
}

.stat-change {
  font-size: 12px;
  font-weight: 500;
}

.text-success {
  color: #67c23a;
}

.text-danger {
  color: #f56c6c;
}
</style>