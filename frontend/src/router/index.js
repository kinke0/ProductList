import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/LoginView.vue')
  },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'DataWorkbench',
        component: () => import('../views/dashboard/DataWorkbench.vue'),
        meta: { title: '产品清单' }
      },
      {
        path: 'users',
        name: 'UserManage',
        component: () => import('../views/system/UserManage.vue'),
        meta: { title: '用户管理', roles: ['ADMIN'] }
      },
      {
        path: 'roles',
        name: 'RoleManage',
        component: () => import('../views/system/RoleManage.vue'),
        meta: { title: '权限套餐管理', roles: ['ADMIN'] }
      },
      {
        path: 'versions',
        name: 'VersionManage',
        component: () => import('../views/system/VersionManage.vue'),
        meta: { title: '版本管理', roles: ['ADMIN'] }
      },
      {
        path: 'base-data/category',
        name: 'BaseDataManage',
        component: () => import('../views/system/BaseDataManage.vue'),
        meta: { title: '业务分类维护', roles: ['ADMIN'] }
      },
      {
        path: 'base-data/solution',
        name: 'SolutionManage',
        component: () => import('../views/system/OptionManage.vue'),
        meta: { title: '解决方案维护', type: 'solution', roles: ['ADMIN'] }
      },
      {
        path: 'base-data/app-role',
        name: 'AppRoleManage',
        component: () => import('../views/system/OptionManage.vue'),
        meta: { title: '应用角色维护', type: 'app_role', roles: ['ADMIN'] }
      },
      {
        path: 'base-data/status',
        name: 'StatusManage',
        component: () => import('../views/system/OptionManage.vue'),
        meta: { title: '功能状态维护', type: 'status', roles: ['ADMIN'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const roleCode = localStorage.getItem('roleCode')

  if (to.path !== '/login' && !token) {
    next('/login')
    return
  }

  if (to.meta.roles && !to.meta.roles.includes(roleCode)) {
    next('/dashboard')
    return
  }

  next()
})

export default router
