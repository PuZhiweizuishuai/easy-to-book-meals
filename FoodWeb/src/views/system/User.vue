<template>
  <div>
    <h2 class="text-2xl font-bold mb-6">用户管理</h2>

    <div class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body p-4 sm:p-6">
        <div class="flex flex-col md:flex-row justify-between gap-4 mb-6">
          <div class="flex flex-col sm:flex-row gap-3 w-full md:w-auto">
            <label class="input input-bordered flex items-center gap-2 w-full sm:w-64">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor"
                class="w-5 h-4 opacity-70">
                <path fill-rule="evenodd"
                  d="M9.965 11.026a5 5 0 1 1 1.06-1.06l2.755 2.754a.75.75 0 1 1-1.06 1.06l-2.755-2.754ZM10.5 7a3.5 3.5 0 1 1-7 0 3.5 3.5 0 0 1 7 0Z"
                  clip-rule="evenodd" />
              </svg>
              <input type="text" v-model="searchKey" placeholder="搜索用户昵称..." class="grow"
                @keyup.enter="handleSearch" />
            </label>
          </div>
          <button class="btn btn-primary w-full md:w-auto" @click="showAddModal = true">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24"
              stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            新增用户
          </button>
        </div>

        <div v-if="loading" class="flex justify-center py-16">
          <span class="loading loading-spinner loading-lg text-primary"></span>
        </div>

        <div v-else-if="users.length === 0" class="text-center py-16 text-base-content/50">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 mx-auto mb-4 opacity-30" fill="none"
            viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1"
              d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z" />
          </svg>
          <p class="text-lg">暂无用户数据</p>
        </div>

        <template v-else>
          <div class="grid grid-cols-1 gap-4 md:hidden">
            <div v-for="user in users" :key="user.id"
              class="bg-base-200/50 border border-base-200 rounded-2xl p-4 flex gap-4 items-center transition-all hover:shadow-md">
              <div class="avatar placeholder">
                <div class="bg-neutral text-neutral-content w-14 h-14 rounded-xl">
                  <img v-if="user.avatarUrl" :src="user.avatarUrl" :alt="user.nickname" />
                  <span v-else class="text-lg">{{ (user.nickname || '?')[0] }}</span>
                </div>
              </div>
              <div class="flex-1 min-w-0">
                <h3 class="font-bold text-base truncate">{{ user.nickname || '未设置' }}</h3>
                <div class="text-xs text-base-content/70 mt-1">
                  {{ user.phone || '未绑定手机' }} <span v-if="user.email" class="mx-1">·</span>
                  {{ user.email || '' }}
                </div>
                <div class="flex items-center gap-2 mt-2">
                  <span :class="['badge badge-sm', user.status === 0 ? 'badge-success' : 'badge-error']">
                    {{ user.status === 0 ? '正常' : '禁用' }}
                  </span>
                  <span class="text-xs text-base-content/40">{{ formatDate(user.createTime) }}</span>
                </div>
              </div>
              <div class="flex flex-col gap-2">
                <button class="btn btn-circle btn-ghost btn-sm bg-base-100 shadow-sm" @click="openEditModal(user)"
                  title="编辑信息">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                    stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                  </svg>
                </button>
                <button class="btn btn-circle btn-ghost btn-sm bg-base-100 shadow-sm" @click="openRoleModal(user)"
                  title="角色管理">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                    stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                  </svg>
                </button>
                <button
                  :class="['btn btn-circle btn-ghost btn-sm bg-base-100 shadow-sm', user.status === 0 ? 'text-error' : 'text-success']"
                  @click="toggleStatus(user)" :title="user.status === 0 ? '禁用' : '启用'">
                  <svg v-if="user.status === 0" xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none"
                    viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                  </svg>
                  <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                    stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </button>
                <button class="btn btn-circle btn-ghost btn-sm bg-base-100 shadow-sm text-error"
                  @click="openResetModal(user)" title="重置密码">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                    stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
                  </svg>
                </button>
              </div>
            </div>
          </div>

          <div class="hidden md:block overflow-x-auto rounded-box border border-base-200">
            <table class="table table-zebra w-full">
              <thead class="bg-base-200 text-base-content">
                <tr>
                  <th>用户信息</th>
                  <th>手机号</th>
                  <th>邮箱</th>
                  <th>状态</th>
                  <th>注册时间</th>
                  <th class="text-right pr-6">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="user in users" :key="user.id" class="hover">
                  <td>
                    <div class="flex items-center gap-3">
                      <div class="avatar placeholder">
                        <div class="bg-neutral text-neutral-content w-10 h-10 mask mask-squircle">
                          <img v-if="user.avatarUrl" :src="user.avatarUrl" :alt="user.nickname" />
                          <span v-else class="text-sm">{{ (user.nickname || '?')[0] }}</span>
                        </div>
                      </div>
                      <div>
                        <div class="font-bold">{{ user.nickname || '未设置' }}</div>
                        <div class="text-sm opacity-50">ID: {{ user.id }}</div>
                      </div>
                    </div>
                  </td>
                  <td class="text-sm">{{ user.phone || '-' }}</td>
                  <td class="text-sm">{{ user.email || '-' }}</td>
                  <td>
                    <span
                      :class="['badge badge-sm', user.status === 0 ? 'badge-success badge-outline' : 'badge-error badge-outline']">
                      {{ user.status === 0 ? '正常' : '禁用' }}
                    </span>
                  </td>
                  <td class="text-sm text-base-content/60">{{ formatDate(user.createTime) }}</td>
                  <td class="text-right">
                    <div class="join">
                      <button class="btn btn-ghost btn-sm join-item" @click="openEditModal(user)">编辑</button>
                      <button class="btn btn-ghost btn-sm join-item" @click="openRoleModal(user)">角色</button>
                      <button
                        :class="['btn btn-ghost btn-sm join-item', user.status === 0 ? 'text-error' : 'text-success']"
                        @click="toggleStatus(user)">
                        {{ user.status === 0 ? '禁用' : '启用' }}
                      </button>
                      <button class="btn btn-ghost btn-sm join-item text-error" @click="openResetModal(user)">重置密码</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="flex justify-center md:justify-end mt-6">
            <div class="join shadow-sm">
              <button class="join-item btn btn-sm" :disabled="currentPage <= 1" @click="goPage(currentPage - 1)">«
              </button>
              <button v-for="p in visiblePages" :key="p" class="join-item btn btn-sm"
                :class="{ 'btn-active': p === currentPage, 'btn-disabled': p === '...' }"
                @click="p !== '...' && goPage(p)">
                {{ p }}
              </button>
              <button class="join-item btn btn-sm" :disabled="currentPage >= totalPages" @click="goPage(currentPage + 1)">
                »
              </button>
            </div>
          </div>

          <div class="text-center md:text-right text-sm text-base-content/50 mt-2">
            共 {{ totalElements }} 条记录，第 {{ currentPage }} / {{ totalPages || 1 }} 页
          </div>
        </template>
      </div>
    </div>

    <UserEditModal :show="showEditModal" :user="editingUser" @close="showEditModal = false"
      @submit="handleEditSubmit" />
    <UserAddModal :show="showAddModal" @close="showAddModal = false" @submit="handleAddSubmit" />
    <UserRoleModal :show="showRoleModal" :user="editingUser" @close="showRoleModal = false" @updated="loadUsers" />
    <ResetPasswordModal :show="showResetModal" :user="editingUser" @close="showResetModal = false"
      @submit="loadUsers" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { httpGet, httpPost } from '@/utils/fetch'
import { formatDate } from '@/utils/format'
import toast from '@/utils/toast'
import UserEditModal from '@/components/card/UserEditModal.vue'
import UserAddModal from '@/components/card/UserAddModal.vue'
import UserRoleModal from '@/components/card/UserRoleModal.vue'
import ResetPasswordModal from '@/components/card/ResetPasswordModal.vue'

const users = ref([])
const loading = ref(false)
const searchKey = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = ref(0)
const totalElements = ref(0)

const showEditModal = ref(false)
const showAddModal = ref(false)
const showRoleModal = ref(false)
const showResetModal = ref(false)
const editingUser = ref(null)

const visiblePages = computed(() => {
  const total = totalPages.value
  const current = currentPage.value
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }
  const pages = []
  pages.push(1)
  if (current > 3) pages.push('...')
  for (let i = Math.max(2, current - 1); i <= Math.min(total - 1, current + 1); i++) {
    pages.push(i)
  }
  if (current < total - 2) pages.push('...')
  pages.push(total)
  return pages
})

const loadUsers = async () => {
  loading.value = true
  try {
    const keyParam = searchKey.value ? `&key=${encodeURIComponent(searchKey.value)}` : ''
    const res = await httpGet(`/sys/user/list?page=${currentPage.value}&size=${pageSize.value}${keyParam}`)
    if (res?.data) {
      const page = res.data
      users.value = page.content || []
      totalPages.value = page.page.totalPages || 0
      totalElements.value = page.page.totalElements || 0
    }
  } catch (e) {
    toast.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadUsers()
}

const goPage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  loadUsers()
}

const openEditModal = (user) => {
  editingUser.value = { ...user }
  showEditModal.value = true
}

const openRoleModal = (user) => {
  editingUser.value = { ...user }
  showRoleModal.value = true
}

const openResetModal = (user) => {
  editingUser.value = { ...user }
  showResetModal.value = true
}

const toggleStatus = async (user) => {
  const newStatus = user.status === 0 ? 1 : 0
  const action = newStatus === 1 ? '禁用' : '启用'
  try {
    const res = await httpPost(`/sys/user/status?userId=${user.id}&status=${newStatus}`)
    if (res?.data) {
      toast.success(`用户已${action}`)
      loadUsers()
    } else {
      toast.error(res?.message || `${action}失败`)
    }
  } catch (e) {
    toast.error(`${action}操作失败`)
  }
}

const handleEditSubmit = async (formData) => {
  try {
    const res = await httpPost('/user/update', formData)
    if (res?.data) {
      toast.success('用户信息更新成功')
      showEditModal.value = false
      loadUsers()
    } else {
      toast.error(res?.message || '更新失败')
    }
  } catch (e) {
    toast.error('用户信息更新失败')
  }
}

const handleAddSubmit = async (formData) => {
  try {
    const res = await httpPost('/user/register', formData)
    if (res?.data) {
      toast.success('用户创建成功')
      showAddModal.value = false
      loadUsers()
    } else {
      toast.error(res?.message || '创建失败')
    }
  } catch (e) {
    toast.error('用户创建失败')
  }
}

onMounted(() => {
  loadUsers()
})
</script>
