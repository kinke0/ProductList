<template>
  <div class="si-layout">
    <div class="si-sidebar" :class="{ 'is-collapsed': isCollapsed }">
      <div class="sidebar-logo">
        <span class="logo-text">添翼</span>
        <span class="logo-sub">PRO</span>
        <span class="collapse-btn" @click="isCollapsed = !isCollapsed">
          <el-icon><Fold v-if="!isCollapsed" /><Expand v-else /></el-icon>
        </span>
      </div>
      <el-menu
        :default-active="route.path"
        router
        class="si-menu"
        :collapse="isCollapsed"
        background="transparent"
        text-color="#94A3B8"
        active-text-color="#FFFFFF"
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
        <el-menu-item v-if="authStore.isAdmin()" index="/image-gallery">
          <el-icon><Picture /></el-icon>
          <span>图床管理</span>
        </el-menu-item>
      </el-menu>
    </div>

    <div class="si-main">
      <div class="si-header">
        <span class="header-title">{{ route.meta?.title || '工作台' }}</span>
        <el-dropdown @command="handleCommand">
          <span class="header-user">
            {{ nickname || '用户' }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="changePassword">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <div class="si-content">
        <router-view />
      </div>
    </div>

    <el-dialog v-model="pwdVisible" title="修改密码" width="400px" :close-on-click-modal="false">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px" size="large">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password @keyup.enter="handleChangePassword" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="handleChangePassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { ref, reactive } from 'vue'
import { Monitor, Setting, User, Ticket, Document, Grid, List, Coin, UserFilled, Flag, ArrowDown, Fold, Expand, Picture } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { changePassword } from '../api/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const nickname = ref(authStore.user?.nickname || localStorage.getItem('nickname') || '用户')
const isCollapsed = ref(false)

function handleCommand(command) {
  if (command === 'logout') {
    authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } else if (command === 'changePassword') {
    Object.assign(pwdForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
    pwdVisible.value = true
  }
}

const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref(null)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function handleChangePassword() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  pwdLoading.value = true
  try {
    await changePassword(pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('密码修改成功，请重新登录')
    pwdVisible.value = false
    authStore.logout()
    router.push('/login')
  } catch (e) {
  } finally {
    pwdLoading.value = false
  }
}
</script>

<style scoped>
.si-layout {
  display: flex;
  height: 100vh;
  background: var(--si-bg);
}

.si-sidebar {
  width: var(--si-sidebar-width);
  flex-shrink: 0;
  background: var(--si-bg-sidebar);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: width 0.3s ease;
}
.si-sidebar.is-collapsed {
  width: 64px;
}

.sidebar-logo {
  height: 48px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  flex-shrink: 0;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  position: relative;
}
.sidebar-logo .logo-text,
.sidebar-logo .logo-sub {
  transition: opacity 0.2s;
}
.si-sidebar.is-collapsed .logo-text,
.si-sidebar.is-collapsed .logo-sub {
  opacity: 0;
  width: 0;
  overflow: hidden;
}
.collapse-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  color: #94A3B8;
  font-size: 16px;
  padding: 4px;
  border-radius: 4px;
  transition: color 0.2s, background 0.2s;
}
.collapse-btn:hover {
  color: #fff;
  background: rgba(255,255,255,0.08);
}
.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
}
.logo-sub {
  font-size: 9px;
  color: var(--si-text-on-dark-muted);
  margin-left: 4px;
  font-weight: 600;
  letter-spacing: 1.5px;
}

.si-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  background: transparent;
  padding: 4px 8px;
}
.si-menu .el-menu-item {
  height: 36px;
  line-height: 36px;
  margin: 1px 0;
  border-radius: var(--si-radius-md);
  font-size: 13px;
}
.si-menu .el-sub-menu .el-sub-menu__title {
  height: 36px;
  line-height: 36px;
  margin: 1px 0;
  border-radius: var(--si-radius-md);
  font-size: 13px;
}

.si-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.si-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: var(--si-header-height);
  flex-shrink: 0;
  background: var(--si-bg-header);
  border-bottom: 1px solid var(--si-border);
}

.header-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--si-text-primary);
}

.header-user {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--si-text-secondary);
  font-size: 13px;
  padding: 4px 8px;
  border-radius: var(--si-radius-sm);
  transition: background var(--si-transition), color var(--si-transition);
}
.header-user:hover {
  background: var(--si-bg-hover);
  color: var(--si-text-primary);
}

.si-content {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}
</style>
