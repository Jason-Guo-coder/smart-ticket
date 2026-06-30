import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/login' },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/app',
    component: () => import('@/layouts/AppShell.vue'),
    children: [
      // 各业务页将在后续 Phase 挂载到此处
      { path: '', redirect: '/app/home' },
      { path: 'home', name: 'home', component: () => import('@/views/Placeholder.vue') },
      {
        path: 'submit',
        name: 'submit',
        component: () => import('@/views/employee/SubmitTicket.vue'),
        meta: { title: '提交工单' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 登录守卫：非 public 页面需要 token
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.public) return next()
  if (!token) return next('/login')
  next()
})

export default router
