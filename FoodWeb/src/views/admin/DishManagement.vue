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
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor" class="w-4 h-4 opacity-70"><path fill-rule="evenodd" d="M9.965 11.026a5 5 0 1 1 1.06-1.06l2.755 2.754a.75.75 0 1 1-1.06 1.06l-2.755-2.754ZM10.5 7a3.5 3.5 0 1 1-7 0 3.5 3.5 0 0 1 7 0Z" clip-rule="evenodd" /></svg>
              <input type="text" placeholder="搜索菜品..." class="grow" />
            </label>
            <select class="select select-bordered w-full sm:w-48">
              <option value="">所有分类</option>
              <option value="appetizer">凉菜</option>
              <option value="main">热菜</option>
              <option value="soup">汤类</option>
              <option value="drink">饮品</option>
            </select>
          </div>
          <button class="btn btn-primary w-full md:w-auto" @click="showCreateForm = true">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" /></svg>
            添加菜品
          </button>
        </div>

        <div class="grid grid-cols-1 gap-4 md:hidden">
          <div v-for="dish in dishList" :key="dish.id" class="bg-base-200/50 border border-base-200 rounded-2xl p-4 flex gap-4 items-center transition-all hover:shadow-md">
            <div class="avatar">
              <div class="w-16 h-16 rounded-xl bg-base-300">
                </div>
            </div>
            <div class="flex-1 min-w-0">
              <h3 class="font-bold text-base truncate">{{ dish.name }}</h3>
              <div class="text-xs text-base-content/70 mt-1">
                {{ dish.category }} <span class="mx-1">·</span> <span class="text-primary font-medium">¥{{ dish.price }}</span>
              </div>
              <div class="mt-2">
                <div :class="['badge badge-sm', dish.status === 'online' ? 'badge-success' : 'badge-error']">
                  {{ dish.status === 'online' ? '上架' : '下架' }}
                </div>
              </div>
            </div>
            <div class="flex flex-col gap-2">
              <button class="btn btn-circle btn-ghost btn-sm bg-base-100 shadow-sm"><svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" /></svg></button>
              <button :class="['btn btn-circle btn-ghost btn-sm bg-base-100 shadow-sm', dish.status === 'online' ? 'text-error' : 'text-success']">
                <svg v-if="dish.status === 'online'" xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 14l-7 7m0 0l-7-7m7 7V3" /></svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 10l7-7m0 0l7 7m-7-7v18" /></svg>
              </button>
            </div>
          </div>
        </div>

        <div class="hidden md:block overflow-x-auto rounded-box border border-base-200">
          <table class="table table-zebra w-full">
            <thead class="bg-base-200 text-base-content">
              <tr>
                <th>菜品信息</th>
                <th>分类</th>
                <th>价格</th>
                <th>状态</th>
                <th class="text-right pr-6">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="dish in dishList" :key="dish.id" class="hover">
                <td>
                  <div class="flex items-center gap-4">
                    <div class="avatar">
                      <div class="w-12 h-12 mask mask-squircle bg-base-300">
                        </div>
                    </div>
                    <div>
                      <div class="font-bold">{{ dish.name }}</div>
                      <div class="text-sm opacity-50">ID: {{ dish.id }}</div>
                    </div>
                  </div>
                </td>
                <td>{{ dish.category }}</td>
                <td class="font-medium text-primary">¥{{ dish.price }}</td>
                <td>
                  <div :class="['badge badge-sm', dish.status === 'online' ? 'badge-success badge-outline' : 'badge-error badge-outline']">
                    {{ dish.status === 'online' ? '上架' : '下架' }}
                  </div>
                </td>
                <td class="text-right">
                  <div class="join">
                    <button class="btn btn-ghost btn-sm join-item">编辑</button>
                    <button :class="['btn btn-ghost btn-sm join-item', dish.status === 'online' ? 'text-error' : 'text-success']">
                      {{ dish.status === 'online' ? '下架' : '上架' }}
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="flex justify-center md:justify-end mt-6">
          <div class="join shadow-sm">
            <button class="join-item btn btn-sm">«</button>
            <button class="join-item btn btn-sm">1</button>
            <button class="join-item btn btn-sm btn-active">2</button>
            <button class="join-item btn btn-sm">3</button>
            <button class="join-item btn btn-sm">»</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import CreateDishesCard from '@/components/card/CreateDishesCard.vue';
import toast from '@/utils/toast';

const showCreateForm = ref(false);

// 模拟后端返回的数据列表
const dishList = ref([
  { id: '1001', name: '宫保鸡丁', category: '热菜', price: '38.00', status: 'online' },
  { id: '1002', name: '麻婆豆腐', category: '热菜', price: '28.00', status: 'online' },
  { id: '1003', name: '拍黄瓜', category: '凉菜', price: '18.00', status: 'offline' },
]);

const handleCreateSuccess = (data) => {
  showCreateForm.value = false;
  toast.success('菜品添加成功！');
  // 这里通常会重新调用拉取列表的 API
};
</script>