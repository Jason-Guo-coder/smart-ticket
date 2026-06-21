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

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const roles = [
  { code: 'EMPLOYEE', label: '报修人' },
  { code: 'ENGINEER', label: '工程师' },
  { code: 'ADMIN', label: '管理员' }
]

// Phase 0：占位登录，打通到 AppShell。Phase 1 接 /api/auth/login 真实鉴权。
function onLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  setTimeout(() => {
    localStorage.setItem('token', 'dev-placeholder-token')
    localStorage.setItem('role', 'EMPLOYEE')
    localStorage.setItem('realName', form.username)
    loading.value = false
    router.push('/app/home')
  }, 400)
}

function quickLogin(roleCode) {
  localStorage.setItem('token', 'dev-placeholder-token')
  localStorage.setItem('role', roleCode)
  localStorage.setItem('realName', roles.find((r) => r.code === roleCode).label)
  router.push('/app/home')
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
