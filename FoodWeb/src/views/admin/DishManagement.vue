<template>
  <div>
    <h2 class="text-2xl font-bold mb-6">菜品管理</h2>

    <div class="modal" :class="{ 'modal-open': showCreateForm }">
      <div class="modal-box max-w-6xl max-h-[90vh] overflow-y-auto">
        <CreateDishesCard @submit="handleCreateSuccess" @cancel="showCreateForm = false" />
      </div>
    </div>

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
              <input type="text" v-model="searchKey" placeholder="搜索菜品..." class="grow" @keyup.enter="handleSearch" />
            </label>
            <!-- <select class="select select-bordered w-full sm:w-48">
              <option value="">所有分类</option>
              <option value="appetizer">凉菜</option>
              <option value="main">热菜</option>
              <option value="soup">汤类</option>
              <option value="drink">饮品</option>
            </select> -->
          </div>
          <button class="btn btn-primary w-full md:w-auto" @click="showCreateForm = true">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24"
              stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            添加菜品
          </button>
        </div>

        <div class="grid grid-cols-1 gap-4 md:hidden">
          <div v-for="dish in dishList" :key="dish.id"
            class="bg-base-200/50 border border-base-200 rounded-2xl p-4 flex gap-4 items-center transition-all hover:shadow-md">
            <div class="avatar">
              <div class="w-16 h-16 rounded-xl bg-base-300">
                <img :src="dish.topImage" :alt="dish.name" class="w-full h-full object-cover rounded-xl" />
              </div>
            </div>
            <div class="flex-1 min-w-0">
              <h3 class="font-bold text-base truncate">{{ dish.name }}</h3>
              <div class="text-xs text-base-content/70 mt-1">
                {{ getCategoryNames(dish.categoryIds) }} <span class="mx-1">·</span> <span
                  class="text-primary font-medium">¥{{ dish.basePrice }}</span>
              </div>
              <div class="mt-2">
                <div :class="['badge badge-sm', dish.status === 1 ? 'badge-success' : 'badge-error']">
                  {{ dish.status === 1 ? '上架' : '下架' }}
                </div>
              </div>
            </div>
            <div class="flex flex-col gap-2">
              <button class="btn btn-circle btn-ghost btn-sm bg-base-100 shadow-sm"><svg
                  xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                  stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                </svg></button>
              <button
                :class="['btn btn-circle btn-ghost btn-sm bg-base-100 shadow-sm', dish.status === 1 ? 'text-error' : 'text-success']">
                <svg v-if="dish.status === 1" xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none"
                  viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M19 14l-7 7m0 0l-7-7m7 7V3" />
                </svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                  stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 10l7-7m0 0l7 7m-7-7v18" />
                </svg>
              </button>
            </div>
          </div>
        </div>

        <div class="hidden md:block overflow-x-auto rounded-box border border-base-200">
          <table class="table table-zebra w-full">
            <thead class="bg-base-200 text-base-content">
              <tr>
                <th>封面图</th>
                <th>菜品信息</th>
                <th>分类</th>
                <th>状态</th>
                <th>总销量</th>
                <th>月销量</th>
                <th>创建时间</th>
                <th class="text-right pr-6">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="dish in dishList" :key="dish.id" class="hover">
                <td>
                  <div class="avatar">
                    <div class="w-12 h-12 mask mask-squircle bg-base-300">
                      <img :src="dish.topImage" :alt="dish.name" class="w-full h-full object-cover" />
                    </div>
                  </div>
                </td>
                <td>
                  <div>
                    <div class="font-bold">{{ dish.name }}</div>
                    <div class="text-sm opacity-50">ID: {{ dish.id }}</div>
                  </div>
                </td>
                <td>{{ getCategoryNames(dish.categoryIds) }}</td>
                <td>
                  <div
                    :class="['badge badge-sm', dish.status === 1 ? 'badge-success badge-outline' : 'badge-error badge-outline']">
                    {{ dish.status === 1 ? '上架' : '下架' }}
                  </div>
                </td>
                <td>{{ dish.allSales }}</td>
                <td>{{ dish.monthlySales }}</td>
                <td>{{ formatDate(dish.createTime) }}</td>
                <td class="text-right">
                  <div class="join">
                    <button class="btn btn-ghost btn-sm join-item" @click="editDish(dish)">编辑</button>
                    <button
                      :class="['btn btn-ghost btn-sm join-item', dish.status === 1 ? 'text-error' : 'text-success']">
                      {{ dish.status === 1 ? '下架' : '上架' }}
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 分页组件 -->
        <Pagination v-model="currentPage" :total-pages="totalPages"
                    mode="numbered" align="end" container-class="mt-6"
                    @change="handlePageChange" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import CreateDishesCard from '@/components/card/CreateDishesCard.vue';
import toast from '@/utils/toast';
import { httpGet } from '@/utils/fetch.js';
import { formatDate } from '@/utils/format.js';
import { useRouter } from 'vue-router'
import { useStoreContext } from '@/stores/storeContext.js'
import Pagination from '@/components/common/Pagination.vue'
const router = useRouter() // 拿到路由实例
const storeContext = useStoreContext()
/** 当前管理门店 ID（由 AdminLayout 门店切换器提供） */
const STORE_ID = computed(() => storeContext.currentStoreId)

const showCreateForm = ref(false);
const dishList = ref([]);
const currentPage = ref(0);
const totalPages = ref(1);
const pageSize = ref(10);
const searchKey = ref('');

const getCategoryNames = (categoryIds) => {
  if (!categoryIds || !Array.isArray(categoryIds)) return '';
  return categoryIds.map(item => item.name).join('、');
};

const handleSearch = () => {
  currentPage.value = 1;
  fetchDishList(1, pageSize.value, searchKey.value);
};

const fetchDishList = async (page = 1, size = 10, key = '') => {
  if (!STORE_ID.value) return
  try {
    let url = `/dishes/list?storefrontId=${STORE_ID.value}&page=${page}&size=${size}`;
    if (key) {
      url += `&key=${encodeURIComponent(key)}`;
    }
    const response = await httpGet(url);
    if (response.status === 200) {
      dishList.value = response.data.content;

      currentPage.value = response.data.page.number + 1;
      totalPages.value = response.data.page.totalPages;
      pageSize.value = response.data.page.size;
    } else {
      toast.error(response.message || '获取菜品列表失败');
    }
  } catch (error) {
    toast.error('获取菜品列表失败');
    console.error(error);
  }
};

const handlePageChange = (page) => {
  currentPage.value = page;
  fetchDishList(page, pageSize.value, searchKey.value);
};

const handleCreateSuccess = (data) => {
  showCreateForm.value = false;
  toast.success('菜品添加成功！');
  fetchDishList(1, pageSize.value, searchKey.value);
  router.push({
    name: 'DishEdit', // 用路由 name 最稳定
    params: { id: data.food.id } // 自动拼接成 /admin/dishes/edit/123
  })

};

const editDish = (dish) => {
  console.log(dish)
  router.push({
    name: 'DishEdit',
    params: { id: dish.id }
  })
}

onMounted(() => {
  fetchDishList(1, pageSize.value, searchKey.value);
});
</script>