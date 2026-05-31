<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-title-row">
        <h2 class="login-title">添翼解决方案管理平台</h2>
        <span class="login-version">Version 1.0.1</span>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large">
        <el-form-item prop="username">
          <el-input ref="usernameRef" v-model="form.username" placeholder="用户名" :prefix-icon="User" @keyup.enter="focusPassword" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input ref="passwordRef" v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
        <div class="login-footer">
          <span class="login-link" @click="showRegister">注册账号</span>
        </div>
      </el-form>
    </div>

    <el-dialog v-model="registerVisible" title="注册账号" width="400px" :close-on-click-modal="false">
      <el-form ref="regFormRef" :model="regForm" :rules="regRules" label-width="80px" size="large">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="regForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="regForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="regForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password @keyup.enter="handleRegister" />
        </el-form-item>
        <el-form-item label="姓名" prop="nickname">
          <el-input v-model="regForm.nickname" placeholder="请输入姓名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerVisible = false">取消</el-button>
        <el-button type="primary" :loading="regLoading" @click="handleRegister">注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../store/auth'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { register } from '../../api/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const passwordRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function focusPassword() {
  if (form.username) passwordRef.value?.focus()
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
  } finally {
    loading.value = false
  }
}

const registerVisible = ref(false)
const regLoading = ref(false)
const regFormRef = ref(null)
const regForm = reactive({ username: '', password: '', confirmPassword: '', nickname: '' })

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) { callback(new Error('请再次输入密码')); return }
  if (value !== regForm.password) { callback(new Error('两次输入的密码不一致')); return }
  callback()
}

const regRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度3-50个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

function showRegister() {
  Object.assign(regForm, { username: '', password: '', confirmPassword: '', nickname: '' })
  registerVisible.value = true
}

async function handleRegister() {
  const valid = await regFormRef.value.validate().catch(() => false)
  if (!valid) return
  regLoading.value = true
  try {
    await register(regForm.username, regForm.password, regForm.nickname)
    ElMessage.success('注册成功，请登录')
    registerVisible.value = false
    form.username = regForm.username
    form.password = ''
  } catch (e) {
  } finally {
    regLoading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--si-bg);
}

.login-card {
  width: 400px;
  padding: 48px 40px;
  background: var(--si-bg-card);
  border: 1px solid var(--si-border);
  border-radius: var(--si-radius-xl);
  box-shadow: var(--si-shadow-lg);
}

.login-title-row {
  display: flex;
  align-items: baseline;
  margin: 0 0 6px;
}

.login-title {
  margin: 0;
  color: var(--si-text-primary);
  font-size: 22px;
  font-weight: 600;
  font-family: var(--si-font);
}

.login-version {
  margin-left: auto;
  font-size: 12px;
  color: var(--si-text-tertiary, #aaa);
  white-space: nowrap;
  padding-right: 4px;
}

.login-footer {
  text-align: center;
  margin-top: -8px;
}

.login-link {
  color: var(--si-primary);
  cursor: pointer;
  font-size: 13px;
}

.login-link:hover {
  text-decoration: underline;
}

:deep(.el-form-item__label) {
  white-space: nowrap;
}
</style>
