import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import AdminLayout from '../layout/AdminLayout.vue'
import Dashboard from '../views/admin/Dashboard.vue'
import OrderManagement from '../views/admin/OrderManagement.vue'
import DishManagement from '../views/admin/DishManagement.vue'
import TableManagement from '../views/admin/TableManagement.vue'
import CategoryManagement from '../views/admin/CategoryManagement.vue'
import MembershipManagement from '../views/admin/MembershipManagement.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/test',
      name: 'test',
      component: () => import('@/views/test/TestPage.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    // 超级管理员登录
    {
      path: '/sys-login',
      name: 'sysLogin',
      component: () => import('@/views/login/SysLogin.vue'),
      meta: {
        guest: true, // 仅未登录用户可访问
      },
    },
    // 店面管理路由
    {
      path: '/admin',
      component: AdminLayout,
      redirect: '/admin/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: Dashboard,
          meta: {
            requiresAuth: true,
          },
        },
        {
          path: 'orders',
          name: 'orders',
          component: OrderManagement,
          meta: {
            requiresAuth: true,
          },
        },
        {
          path: 'dishes',
          name: 'DishManagement',
          component: DishManagement,
          meta: {
            requiresAuth: true,
          },
        },
        {
          path: 'dishes/edit/:id',
          name: 'DishEdit',
          component: () => import('@/views/admin/dish/EditDishView.vue'),
          meta: {
            requiresAuth: true,
          },
        },
        {
          path: 'tables',
          name: 'tables',
          component: TableManagement,
          meta: {
            requiresAuth: true,
          },
        },
        {
          path: 'tables/:id',
          name: 'TableInfo',
          component: () => import('@/views/admin/table/TableInfo.vue'),
          meta: {
            requiresAuth: true,
          },
        },
        {
          path: 'store',
          name: 'StoreSetting',
          component: () => import('@/views/admin/StoreSetting.vue'),
          meta: {
            requiresAuth: true,
          },
        },
        {
          path: 'categories',
          name: 'categories',
          component: CategoryManagement,
          meta: {
            requiresAuth: true,
          },
        },
        {
          path: 'memberships',
          name: 'memberships',
          component: MembershipManagement,
          meta: {
            requiresAuth: true,
          },
        },
      ],
    },
    // 系统管理路由
    {
      path: '/system',
      name: 'system',
      component: () => import('@/layout/SystemLayout.vue'),
      redirect: '/system/index',
      meta: {
        requiresAuth: true,
      },
      children: [
        {
          path: 'index',
          name: 'systemIndex',
          component: () => import('@/views/system/Index.vue'),
          meta: {
            requiresAuth: true,
          },
        },
        {
          path: 'user',
          name: 'SystemUser',
          component: () => import('@/views/system/User.vue'),
          meta: {
            requiresAuth: true,
          },
        },
        {
          path: 'store',
          name: 'SystemStore',
          component: () => import('@/views/system/Store.vue'),
          meta: {
            requiresAuth: true,
          },
        },
      ]
    }
  ],
})

// 全局导航守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth && !token) {
    // 需要认证但未登录，跳转到登录页
    next({ name: 'sysLogin', query: { redirect: to.fullPath } })
  } else if (to.meta.guest && token) {
    // 已登录用户访问登录页，跳转到系统首页
    next({ name: 'systemIndex' })
  } else {
    next()
  }
})

export default router
