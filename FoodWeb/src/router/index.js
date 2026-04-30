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
            requiresAuth: true, // 添加meta字段，标记需要认证才能访问
          },
        },
        {
          path: 'dishes',
          name: 'dishes',
          component: DishManagement,
          meta: {
            requiresAuth: true, // 添加meta字段，标记需要认证才能访问
          },
        },
        {
          path: 'dishes/edit/:id',
          name: 'editDish',
          component: () => import('@/views/admin/dish/EditDishView.vue'),
          meta: {
            requiresAuth: true, // 添加meta字段，标记需要认证才能访问
          },
        },
        {
          path: 'tables',
          name: 'tables',
          component: TableManagement,
          meta: {
            requiresAuth: true, // 添加meta字段，标记需要认证才能访问
          },
        },
        {
          path: 'categories',
          name: 'categories',
          component: CategoryManagement,
          meta: {
            requiresAuth: true, // 添加meta字段，标记需要认证才能访问
          },
        },
        {
          path: 'memberships',
          name: 'memberships',
          component: MembershipManagement,
          meta: {
            requiresAuth: true, // 添加meta字段，标记需要认证才能访问
          },
        },
      ],
    },
  ],
})

export default router
