import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

/**
 * 统一请求封装：
 * - 注入 JWT
 * - 拆 Result：code=200 返回 data，否则报错（DESIGN §6.3）
 * - 401 跳登录、403 无权限提示
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端统一 Result { code, message, data }
    if (res && typeof res.code !== 'undefined') {
      if (res.code === 200) {
        return res.data
      }
      ElMessage.error(res.message || '操作失败')
      return Promise.reject(new Error(res.message || 'error'))
    }
    return res
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      router.push('/login')
    } else if (status === 403) {
      ElMessage.error('无访问权限')
    } else {
      ElMessage.error(error.response?.data?.message || '网络异常，请稍后再试')
    }
    return Promise.reject(error)
  }
)

export default request
