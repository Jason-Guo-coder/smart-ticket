<template>
  <div class="login">
    <div class="blob-bg">
      <div class="blob blob-blue"></div>
      <div class="blob blob-purple"></div>
      <div class="blob blob-pink"></div>
      <div class="blob blob-orange"></div>
    </div>

    <div class="login__card glass-card page-enter">
      <div class="login__head">
        <div class="login__logo"><el-icon :size="36"><Tickets /></el-icon></div>
        <h1>欢迎回来</h1>
        <p>登录 SmartTicket 智工单</p>
      </div>

      <el-form class="login__form" @submit.prevent>
        <el-form-item>
          <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <el-button class="login__btn" type="primary" size="large" :loading="loading" @click="onLogin">
          登录
        </el-button>
      </el-form>

      <el-divider>或选择角色快速登录（演示）</el-divider>
      <div class="login__roles">
        <el-button v-for="r in roles" :key="r.code" plain @click="quickLogin(r.code)">
          {{ r.label }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
// 演示账号（密码统一 123456）
const roles = [
  { code: 'EMPLOYEE', label: '报修人', username: 'liming' },
  { code: 'ENGINEER', label: '工程师', username: 'zhangwei' },
  { code: 'ADMIN', label: '管理员', username: 'wangfang' }
]

async function doLogin(username, password) {
  loading.value = true
  try {
    const data = await login({ username, password })
    localStorage.setItem('token', data.token)
    localStorage.setItem('role', data.role)
    localStorage.setItem('realName', data.realName || data.username)
    ElMessage.success('登录成功')
    router.push('/app/home')
  } finally {
    loading.value = false
  }
}

function onLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  doLogin(form.username, form.password)
}

function quickLogin(roleCode) {
  const r = roles.find((x) => x.code === roleCode)
  doLogin(r.username, '123456')
}
</script>

<style scoped>
.login {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.login__card {
  position: relative;
  z-index: 1;
  width: 420px;
  padding: 44px 40px 36px;
  border-radius: 28px;
}
.login__head {
  text-align: center;
  margin-bottom: 28px;
}
.login__logo {
  width: 80px;
  height: 80px;
  margin: 0 auto 16px;
  border-radius: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #4285ff, #8c4ddc);
}
.login__head h1 {
  font-size: 30px;
  font-weight: 700;
  margin-bottom: 6px;
}
.login__head p {
  color: var(--apple-darkgray);
  font-size: 15px;
}
.login__btn {
  width: 100%;
  border-radius: var(--radius-control);
}
.login__roles {
  display: flex;
  gap: 12px;
}
.login__roles .el-button {
  flex: 1;
  margin: 0;
}
</style>
