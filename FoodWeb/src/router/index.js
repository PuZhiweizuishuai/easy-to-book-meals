import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import AdminLayout from '../layout/AdminLayout.vue'
import Dashboard from '../views/admin/Dashboard.vue'
import OrderManagement from '../views/admin/OrderManagement.vue'
import OrderCreate from '../views/admin/OrderCreate.vue'
import DishManagement from '../views/admin/DishManagement.vue'
import TableManagement from '../views/admin/TableManagement.vue'
import CategoryManagement from '../views/admin/CategoryManagement.vue'
import MembershipManagement from '../views/admin/MembershipManagement.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    // 门店点餐页（公开，?table=二维码标识 为扫码点餐入口）
    {
      path: '/store/:id',
      name: 'storeMenu',
      component: () => import('@/views/customer/StoreMenuPage.vue'),
    },
    // 优惠券详情（公开：未登录也可展示券面与二维码，登录后可领取）
    {
      path: '/coupon/:code',
      name: 'couponDetail',
      component: () => import('@/views/customer/CouponDetail.vue'),
    },
    // 我的订单（顾客端，需登录）
    {
      path: '/my/orders',
      name: 'myOrders',
      component: () => import('@/views/customer/MyOrders.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/test',
      name: 'test',
      component: () => import('@/views/test/TestPage.vue'),
      meta: { requiresAuth: true },
    },
    // 通用登录页（所有角色共用）
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/Login.vue'),
      meta: { guest: true },
    },
    // 店面管理路由（店主、店员等门店角色可访问）
    {
      path: '/admin',
      component: AdminLayout,
      redirect: '/admin/dashboard',
      meta: {
        requiresAuth: true,
        // 需要门店域角色：店主、收银员、后厨、服务员、库存管理员
        roles: ['STORE_OWNER', 'STORE_CASHIER', 'STORE_KITCHEN', 'STORE_WAITER', 'STORE_INVENTORY'],
      },
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: Dashboard,
          meta: { requiresAuth: true },
        },
        {
          path: 'orders',
          name: 'orders',
          component: OrderManagement,
          meta: {
            requiresAuth: true,
            permissions: ['order:manage', 'order:view'],
          },
        },
        {
          path: 'orders/create',
          name: 'orderCreate',
          component: OrderCreate,
          meta: {
            requiresAuth: true,
            permissions: ['order:manage'],
          },
        },
        {
          path: 'dishes',
          name: 'DishManagement',
          component: DishManagement,
          meta: {
            requiresAuth: true,
            permissions: ['dish:manage', 'dish:view'],
          },
        },
        {
          path: 'dishes/edit/:id',
          name: 'DishEdit',
          component: () => import('@/views/admin/dish/EditDishView.vue'),
          meta: {
            requiresAuth: true,
            permissions: ['dish:manage'],
          },
        },
        {
          path: 'tables',
          name: 'tables',
          component: TableManagement,
          meta: {
            requiresAuth: true,
            permissions: ['table:manage', 'table:view'],
          },
        },
        {
          path: 'tables/:id',
          name: 'TableInfo',
          component: () => import('@/views/admin/table/TableInfo.vue'),
          meta: {
            requiresAuth: true,
            permissions: ['table:manage', 'table:view'],
          },
        },
        {
          path: 'store',
          name: 'StoreSetting',
          component: () => import('@/views/admin/StoreSetting.vue'),
          meta: {
            requiresAuth: true,
            roles: ['STORE_OWNER'],
          },
        },
        {
          path: 'categories',
          name: 'categories',
          component: CategoryManagement,
          meta: {
            requiresAuth: true,
            permissions: ['dish:manage'],
          },
        },
        {
          path: 'memberships',
          name: 'memberships',
          component: MembershipManagement,
          meta: {
            requiresAuth: true,
            permissions: ['member:manage'],
          },
        },
        {
          path: 'coupons',
          name: 'coupons',
          component: () => import('@/views/admin/CouponManagement.vue'),
          meta: {
            requiresAuth: true,
            permissions: ['coupon:manage', 'coupon:view'],
          },
        },
      ],
    },
    // 系统管理路由（超级管理员、平台运营等全局角色可访问）
    {
      path: '/system',
      name: 'system',
      component: () => import('@/layout/SystemLayout.vue'),
      redirect: '/system/index',
      meta: {
        requiresAuth: true,
        // 需要全局域角色：超级管理员、平台运营、平台审核员
        roles: ['SUPER_ADMIN', 'PLATFORM_OPERATOR', 'PLATFORM_AUDITOR'],
      },
      children: [
        {
          path: 'index',
          name: 'systemIndex',
          component: () => import('@/views/system/Index.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'user',
          name: 'SystemUser',
          component: () => import('@/views/system/User.vue'),
          meta: {
            requiresAuth: true,
            permissions: ['user:manage', 'user:view'],
          },
        },
        {
          path: 'store',
          name: 'SystemStore',
          component: () => import('@/views/system/Store.vue'),
          meta: {
            requiresAuth: true,
            permissions: ['store:audit', 'store:view'],
          },
        },
      ],
    },
    // 404 兜底：未匹配的路径统一回到首页（须放在所有具名路由之后）
    {
      path: '/:pathMatch(.*)*',
      name: 'notFound',
      redirect: '/',
    },
  ],
})

/**
 * 全局导航守卫
 *
 * 校验逻辑：
 * 1. requiresAuth — 需要登录，未登录则跳转登录页并记录来源页面
 * 2. guest — 仅未登录用户可访问（如登录页），已登录则跳转首页
 * 3. roles — 需要指定角色之一（超级管理员自动放行）
 * 4. permissions — 需要指定权限之一（超级管理员自动放行）
 */
router.beforeEach((to, from) => {
  const token = localStorage.getItem('token')

  // 需要认证但未登录
  if (to.meta.requiresAuth && !token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  // 已登录用户访问登录页，跳转首页
  if (to.meta.guest && token) {
    return { name: 'home' }
  }

  // 角色和权限校验（仅在已登录时检查）
  if (to.meta.requiresAuth && token) {
    const authStore = useAuthStore()

    // 角色校验：用户需拥有 meta.roles 中的任一角色
    if (to.meta.roles && !authStore.hasAnyRole(to.meta.roles)) {
      return { name: 'home' }
    }

    // 权限校验：用户需拥有 meta.permissions 中的任一权限
    if (to.meta.permissions && !authStore.hasAnyPermission(to.meta.permissions)) {
      return { name: 'home' }
    }
  }

  return true
})

export default router
