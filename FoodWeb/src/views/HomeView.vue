<template>
  <div class="min-h-screen bg-base-200">
    <!-- 顶部导航（通用组件） -->
    <AppHeader>
      <!-- 搜索框 -->
      <template #middle>
        <div class="flex-none hidden sm:block mr-4">
          <label class="input input-bordered input-sm flex items-center gap-2 w-64">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor" class="w-4 h-4 opacity-70">
              <path fill-rule="evenodd" d="M9.965 11.026a5 5 0 1 1 1.06-1.06l2.755 2.754a.75.75 0 1 1-1.06 1.06l-2.755-2.754ZM10.5 7a3.5 3.5 0 1 1-7 0 3.5 3.5 0 0 1 7 0Z" clip-rule="evenodd" />
            </svg>
            <input v-model="searchKey" type="text" placeholder="搜索门店..." class="grow" @keyup.enter="handleSearch" />
          </label>
        </div>
      </template>
    </AppHeader>

    <main class="container mx-auto px-4 py-6 max-w-6xl">
      <!-- 移动端搜索 -->
      <div class="mb-4 sm:hidden">
        <label class="input input-bordered flex items-center gap-2 w-full">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor" class="w-4 h-4 opacity-70">
            <path fill-rule="evenodd" d="M9.965 11.026a5 5 0 1 1 1.06-1.06l2.755 2.754a.75.75 0 1 1-1.06 1.06l-2.755-2.754ZM10.5 7a3.5 3.5 0 1 1-7 0 3.5 3.5 0 0 1 7 0Z" clip-rule="evenodd" />
          </svg>
          <input v-model="searchKey" type="text" placeholder="搜索门店..." class="grow" @keyup.enter="handleSearch" />
        </label>
      </div>

      <h1 class="text-2xl font-bold mb-1">选择门店</h1>
      <p class="text-sm text-base-content/50 mb-6">挑选心仪的门店，扫码或选购心仪的美食</p>

      <div v-if="loading" class="flex justify-center py-24">
        <span class="loading loading-spinner loading-lg text-primary"></span>
      </div>

      <div v-else-if="stores.length === 0" class="card bg-base-100 shadow-sm">
        <div class="card-body items-center py-20 text-center">
          <span class="text-5xl mb-3">🏪</span>
          <p class="text-base-content/50">暂无营业中的门店</p>
        </div>
      </div>

      <template v-else>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <router-link
            v-for="store in stores"
            :key="store.id"
            :to="`/store/${store.id}`"
            class="card bg-base-100 shadow-sm border border-base-200 hover:shadow-md transition-shadow"
          >
            <figure class="h-36 bg-base-200 overflow-hidden">
              <img
                v-if="store.logoUrl"
                :src="store.logoUrl"
                :alt="store.storeName"
                class="w-full h-full object-cover"
              />
              <div v-else class="w-full h-full flex items-center justify-center text-5xl">🍽️</div>
            </figure>
            <div class="card-body p-4">
              <div class="flex items-center justify-between">
                <h2 class="card-title text-base">{{ store.storeName }}</h2>
                <span :class="['badge badge-sm', store.status === 0 ? 'badge-success' : 'badge-info']">
                  {{ store.status === 0 ? '营业中' : '休息中' }}
                </span>
              </div>
              <p class="text-sm text-base-content/60 line-clamp-2 min-h-10">{{ store.description || '暂无简介' }}</p>
              <div class="flex items-center justify-between text-xs text-base-content/50">
                <span class="flex items-center gap-1">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  {{ shortAddress(store.address) }}
                </span>
                <span v-if="store.businessHours">{{ store.businessHours }}</span>
              </div>
              <div v-if="store.tags && store.tags.length" class="flex flex-wrap gap-1 mt-1">
                <span v-for="tag in store.tags.slice(0, 3)" :key="tag.id" class="badge badge-ghost badge-xs">{{ tag.tagName }}</span>
              </div>
            </div>
          </router-link>
        </div>

        <!-- 分页 -->
        <Pagination v-model="page" :total-pages="totalPages" container-class="mt-8"
                    @change="changePage" />
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { httpGet, isSuccess, parsePage } from '@/utils/fetch'
import toast from '@/utils/toast'
import Pagination from '@/components/common/Pagination.vue'
import AppHeader from '@/components/common/AppHeader.vue'

const loading = ref(false)
const stores = ref([])
const page = ref(1)
const size = 12
const totalPages = ref(1)
const searchKey = ref('')

onMounted(() => {
  fetchStores()
})

/** 加载营业中的门店列表 */
const fetchStores = async () => {
  loading.value = true
  try {
    let url = `/store/list?page=${page.value}&size=${size}`
    if (searchKey.value) {
      url += `&key=${encodeURIComponent(searchKey.value)}`
    }
    const res = await httpGet(url)
    if (isSuccess(res) && res.data) {
      const p = parsePage(res.data)
      stores.value = p.content
      totalPages.value = p.totalPages
    }
  } catch (e) {
    console.error('加载门店列表失败:', e)
    toast.error('加载门店列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchStores()
}

const changePage = (p) => {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  fetchStores()
}

const shortAddress = (addr) => {
  if (!addr) return ''
  return addr.length > 24 ? addr.slice(0, 24) + '...' : addr
}
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
