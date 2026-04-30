<template>
  <div>
    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
      <div>
        <h2 class="text-2xl font-bold text-base-content">分类管理</h2>
        <p class="text-sm text-base-content/60 mt-1">管理菜单中的菜品分类及排序</p>
      </div>
      <button class="btn btn-primary w-full sm:w-auto" @click="openModal('add')">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" /></svg>
        添加分类
      </button>
    </div>

    <div v-if="loading" class="flex justify-center items-center py-20">
      <span class="loading loading-spinner loading-lg text-primary"></span>
    </div>

    <div v-else class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body p-0 sm:p-4">
        
        <div class="grid grid-cols-1 gap-3 p-4 md:hidden">
          <div v-if="tagList.length === 0" class="text-center py-10 text-base-content/50">暂无分类数据</div>
          <div v-for="tag in tagList" :key="tag.id" class="bg-base-200/50 border border-base-200 rounded-2xl p-4 flex flex-col gap-3">
            <div class="flex justify-between items-center border-b border-base-300 pb-2">
              <h3 class="font-bold text-lg text-primary">{{ tag.name }}</h3>
              <span class="badge badge-neutral badge-sm">排序: {{ tag.sort }}</span>
            </div>
            <div class="flex justify-between items-center">
              <div class="text-sm text-base-content/70">
                菜品数量: <span class="font-semibold text-base-content">{{ tag.count || 0 }}</span>
              </div>
              <div class="flex gap-2">
                <button class="btn btn-circle btn-ghost btn-sm bg-base-100 shadow-sm text-info" @click="openModal('edit', tag)">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" /></svg>
                </button>
                <button class="btn btn-circle btn-ghost btn-sm bg-base-100 shadow-sm text-error" @click="handleDelete(tag.id, tag.name)">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="hidden md:block overflow-x-auto">
          <table class="table table-zebra w-full">
            <thead class="bg-base-200 text-base-content">
              <tr>
                <th class="w-20">ID</th>
                <th>分类名称</th>
                <th>当前分类下菜品数量</th>
                <th>显示排序</th>
                <th>创建时间</th>
                <th class="text-right pr-6">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="tagList.length === 0">
                <td colspan="6" class="text-center py-10 text-base-content/50">暂无分类数据</td>
              </tr>
              <tr v-for="tag in tagList" :key="tag.id" class="hover">
                <td class="font-mono text-xs opacity-60">{{ tag.id }}</td>
                <td class="font-bold text-primary">{{ tag.name }}</td>
                <td>
                  <div class="badge badge-ghost">{{ tag.count || 0 }} 道</div>
                </td>
                <td>
                  <div class="badge badge-outline">{{ tag.sort }}</div>
                </td>
                <td class="text-sm">{{ formatDate(tag.createTime) }}</td>
                <td class="text-right">
                  <div class="join">
                    <button class="btn btn-ghost btn-sm join-item text-info" @click="openModal('edit', tag)">编辑</button>
                    <button class="btn btn-ghost btn-sm join-item text-error" @click="handleDelete(tag.id, tag.name)">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

      </div>
    </div>

    <!-- 使用抽取的组件 -->
    <EditFoodTagCard
      :show="showModal"
      :modal-type="modalType"
      :tag="currentTag"
      :show-delete="showDeleteModal"
      :delete-target="deleteTarget"
      @close="closeModal"
      @submit="submitForm"
      @delete-close="closeDeleteModal"
      @delete-confirm="confirmDelete"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { httpGet, httpPost } from '@/utils/fetch.js';
import { formatDate } from '@/utils/format.js';
import toast from '@/utils/toast.js';
import EditFoodTagCard from '@/components/card/EditFoodTagCard.vue';

const loading = ref(false);
const tagList = ref([]);
const showModal = ref(false);
const modalType = ref('add');
const currentTag = ref(null);
const showDeleteModal = ref(false);
const deleteTarget = ref({ id: null, name: '' });

onMounted(() => {
  fetchTagList();
});

const fetchTagList = async () => {
  loading.value = true;
  try {
    const res = await httpGet('/foodtag/list');
    if (res && res.data) {
      tagList.value = res.data;
    }
  } catch (error) {
    console.error('获取列表失败:', error);
    toast.error('获取分类列表失败，请检查网络');
  } finally {
    loading.value = false;
  }
};

const openModal = (type, tag = null) => {
  modalType.value = type;
  currentTag.value = tag;
  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
  currentTag.value = null;
};

const submitForm = async (payload) => {
  try {
    const url = payload.id ? '/foodtag/update' : '/foodtag/add';
    await httpPost(url, payload);
    toast.success(payload.id ? '分类更新成功' : '分类添加成功');
    closeModal();
    fetchTagList();
  } catch (error) {
    console.error('保存失败:', error);
    toast.error('保存失败，请稍后重试');
  }
};

const handleDelete = (id, name) => {
  deleteTarget.value = { id, name };
  showDeleteModal.value = true;
};

const closeDeleteModal = () => {
  showDeleteModal.value = false;
  deleteTarget.value = { id: null, name: '' };
};

const confirmDelete = async (id) => {
  try {
    await httpPost(`/foodtag/delete/${id}`, {});
    toast.success('删除成功');
    closeDeleteModal();
    fetchTagList();
  } catch (error) {
    console.error('删除失败:', error);
    toast.error('删除失败，请检查该分类下是否还有菜品');
  }
};
</script>