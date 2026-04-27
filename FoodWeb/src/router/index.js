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
        },
        {
          path: 'orders',
          name: 'orders',
          component: OrderManagement,
        },
        {
          path: 'dishes',
          name: 'dishes',
          component: DishManagement,
        },
        {
          path: 'tables',
          name: 'tables',
          component: TableManagement,
        },
        {
          path: 'categories',
          name: 'categories',
          component: CategoryManagement,
        },
        {
          path: 'memberships',
          name: 'memberships',
          component: MembershipManagement,
        },
      ],
    },
  ],
})

export default router
