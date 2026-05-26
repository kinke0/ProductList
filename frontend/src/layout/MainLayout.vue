<template>
  <el-container style="height: 100vh">
    <el-aside width="220px" style="background: #1a2a3a">
      <div class="logo-area">
        <span class="logo-text">添翼管理平台</span>
      </div>
      <el-menu
        :default-active="route.path"
        background-color="#1a2a3a"
        text-color="rgba(255,255,255,0.7)"
        active-text-color="#409eff"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><Monitor /></el-icon>
          <span>产品清单</span>
        </el-menu-item>
        <el-sub-menu v-if="authStore.isAdmin()" index="admin">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/roles">
            <el-icon><Ticket /></el-icon>
            <span>权限套餐管理</span>
          </el-menu-item>
          <el-menu-item index="/versions">
            <el-icon><Document /></el-icon>
            <span>版本管理</span>
          </el-menu-item>
          <el-sub-menu index="base-data">
            <template #title>
              <el-icon><Grid /></el-icon>
              <span>基础数据维护</span>
            </template>
            <el-menu-item index="/base-data/category">
              <el-icon><List /></el-icon>
              <span>业务分类维护</span>
            </el-menu-item>
            <el-menu-item index="/base-data/solution">
              <el-icon><Coin /></el-icon>
              <span>解决方案维护</span>
            </el-menu-item>
            <el-menu-item index="/base-data/app-role">
              <el-icon><UserFilled /></el-icon>
              <span>应用角色维护</span>
            </el-menu-item>
            <el-menu-item index="/base-data/status">
              <el-icon><Flag /></el-icon>
              <span>功能状态维护</span>
            </el-menu-item>
          </el-sub-menu>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background: #fff; border-bottom: 1px solid #e0e0e0; display: flex; align-items: center; justify-content: flex-end; padding: 0 20px; height: 50px;">
        <el-dropdown @command="handleCommand">
          <span style="cursor: pointer; display: flex; align-items: center; gap: 6px; color: #333;">
            {{ nickname || '用户' }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main style="background: #f5f7fa; padding: 16px;">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { ref } from 'vue'
import { Monitor, Setting, User, Ticket, Document, Grid, List, Coin, UserFilled, Flag, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const nickname = ref(authStore.user?.nickname || localStorage.getItem('nickname') || '用户')

function handleCommand(command) {
  if (command === 'logout') {
    authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.logo-area {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.logo-text {
  color: #fff;
  font-size: 16px;
  letter-spacing: 2px;
  font-weight: 600;
}
.el-menu {
  border-right: none;
}
</style>
