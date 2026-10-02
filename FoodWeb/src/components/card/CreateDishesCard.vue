<template>
  <div class="bg-base-100 rounded-box relative">
    <div class="flex justify-between items-center mb-6 pb-4 border-b border-base-200">
      <h3 class="text-xl font-bold flex items-center gap-2">
        <span class="w-1.5 h-6 bg-primary rounded-full"></span>
        {{ isEdit ? '编辑菜品' : '添加菜品' }}
      </h3>
      <button @click="$emit('cancel')" type="button"
        class="btn btn-circle btn-ghost btn-sm bg-base-200/50 hover:bg-base-300">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>

    <div class="flex-1 flex flex-col">
      <div class="space-y-8 pb-24">

        <section class="space-y-4">
          <h4 class="text-sm font-bold text-base-content/50 uppercase tracking-widest flex items-center gap-2">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
              stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            基本信息
          </h4>

          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">菜品名称 <span
                  class="text-error">*</span></span></label>
            <input v-model="formData.name" type="text" placeholder="请输入菜品名称"
              class="input input-bordered w-full focus:input-primary transition-colors" required />
          </div>

          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">菜品描述</span></label>
            <textarea v-model="formData.description" placeholder="一句话介绍这道菜..."
              class="textarea textarea-bordered h-24 focus:textarea-primary transition-colors"></textarea>
          </div>
        </section>

        <section class="space-y-4">
          <h4 class="text-sm font-bold text-base-content/50 uppercase tracking-widest flex items-center gap-2">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
              stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            图片设置
          </h4>

          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">首选显示图片</span></label>
            <ImageUpload
              aspect-ratio="1:1"
              :allow-multiple="false"
              @upload-success="handleTopImageSuccess"
              @upload-error="handleUploadError"
              @remove-file="handleTopImageRemove"
            />
            <label class="label" v-if="formData.topImage">
              <span class="label-text-alt text-success">已上传: {{ formData.topImage }}</span>
            </label>
          </div>

          <div class="form-control w-full">
            <label class="label">
              <span class="label-text font-medium">详情页图片</span>
              <span class="label-text-alt text-base-content/50">支持多张上传</span>
            </label>
            <ImageUpload
              aspect-ratio="1:1"
              :allow-multiple="true"
              @upload-success="handleDetailImageSuccess"
              @upload-error="handleUploadError"
              @remove-file="handleDetailImageRemove"
            />
            <label class="label" v-if="formData.imageList.length > 0">
              <span class="label-text-alt text-success">已上传 {{ formData.imageList.length }} 张图片</span>
            </label>
          </div>
        </section>

        <section class="space-y-4">
          <h4 class="text-sm font-bold text-base-content/50 uppercase tracking-widest flex items-center gap-2">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
              stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            售卖规则
          </h4>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div class="form-control w-full">
              <label class="label"><span class="label-text font-medium">计价方式 <span
                    class="text-error">*</span></span></label>
              <select v-model="formData.priceType" class="select select-bordered w-full focus:select-primary" required>
                <option :value="0">按份</option>
                <option :value="1">按斤</option>
                <option :value="2">按时</option>
              </select>
            </div>

            <div class="form-control w-full">
              <label class="label"><span class="label-text font-medium">默认价格 <span
                    class="text-error">*</span></span></label>
              <label class="input input-bordered flex items-center gap-2 focus-within:input-primary">
                <span class="text-base-content/50 font-bold">¥</span>
                <input v-model.number="formData.unitPrice" type="number" placeholder="0.00" class="grow" min="0"
                  step="0.01" required />
              </label>
            </div>

            <div class="form-control w-full">
              <label class="label"><span class="label-text font-medium">菜品状态 <span
                    class="text-error">*</span></span></label>
              <select v-model="formData.status" class="select select-bordered w-full focus:select-primary" required>
                <option :value="1">上架销售中</option>
                <option :value="0">下架停售</option>
              </select>
            </div>
          </div>

          <div class="form-control w-full mt-2">
            <label class="label"><span class="label-text font-medium">分类标签</span></label>
            <div
              class="p-4 rounded-xl border border-base-200 bg-base-200/20 flex flex-wrap gap-2.5 items-center min-h-[4rem]">
              <span v-if="isLoadingTags" class="loading loading-spinner text-primary loading-sm"></span>

              <template v-else>
                <button v-for="category in categories" :key="category.id" type="button"
                  @click="toggleCategory(category)" :class="[
                    'badge badge-lg gap-1 py-3 px-4 font-medium transition-all cursor-pointer',
                    selectedCategories.includes(category.id) ? 'badge-primary shadow-sm' : 'badge-ghost bg-base-100 border-base-300 hover:border-primary/50'
                  ]">
                  {{ category.name }}
                  <svg v-if="selectedCategories.includes(category.id)" xmlns="http://www.w3.org/2000/svg"
                    class="h-3.5 w-3.5" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd"
                      d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                      clip-rule="evenodd" />
                  </svg>
                </button>
                <span v-if="categories.length === 0" class="text-sm text-base-content/40">暂无标签</span>
              </template>

              <button type="button" @click="openAddCategoryModal"
                class="btn btn-sm btn-ghost rounded-full text-primary hover:bg-primary/10 ml-auto">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" fill="none" viewBox="0 0 24 24"
                  stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                </svg>
                新增分类
              </button>
            </div>
          </div>
        </section>

        <section class="space-y-4">
          <div class="flex justify-between items-center">
            <h4 class="text-sm font-bold text-base-content/50 uppercase tracking-widest flex items-center gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
              </svg>
              规格配置
            </h4>
            <button type="button" @click="addSpecGroup" class="btn btn-sm btn-primary btn-outline rounded-full">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
              添加规格组
            </button>
          </div>

          <div v-if="formData.specGroups.length === 0"
            class="text-center py-6 bg-base-200/20 rounded-xl border border-dashed border-base-300 text-base-content/50 text-sm">
            暂无规格，点击上方按钮添加（如：大小、口味、温度）
          </div>

          <div class="space-y-4">
            <div v-for="(group, groupIndex) in formData.specGroups" :key="groupIndex"
              class="card bg-base-200/30 border border-base-200 shadow-sm transition-all hover:border-base-300">
              <div class="card-body p-4 sm:p-5">

                <div class="flex flex-wrap sm:flex-nowrap gap-3 items-center mb-4 pb-3 border-b border-base-200/50">
                  <span class="badge badge-neutral text-xs font-bold w-12 shrink-0">组 {{ groupIndex + 1 }}</span>
                  <input v-model="group.name" type="text" placeholder="规格名 (例: 辣度, 杯型)"
                    class="input input-sm input-bordered flex-1 focus:input-primary min-w-[120px]" />
                  <button v-if="formData.specGroups.length > 1" type="button" @click="removeSpecGroup(groupIndex)"
                    class="btn btn-sm btn-circle btn-ghost text-error ml-auto tooltip" data-tip="删除该组">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                      stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                </div>

                <div class="space-y-3">
                  <div v-for="(spec, specIndex) in group.values" :key="specIndex"
                    class="flex items-center gap-2 sm:gap-3">
                    <input v-model="spec.name" type="text" placeholder="选项名称 (例: 微辣)"
                      class="input input-sm input-bordered flex-1 w-0 focus:input-primary" />

                    <button v-if="group.values.length > 1" type="button" @click="removeSpec(groupIndex, specIndex)"
                      class="btn btn-sm btn-square btn-ghost text-base-content/40 hover:text-error hover:bg-error/10 shrink-0">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                        stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </div>

                  <button type="button" @click="addSpec(groupIndex)"
                    class="btn btn-sm btn-ghost btn-block text-base-content/60 hover:text-primary mt-2 border border-dashed border-base-300">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                      stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                    </svg>
                    添加选项
                  </button>
                </div>

              </div>
            </div>
          </div>
        </section>

      </div>

      <div class="sticky bottom-0 bg-base-100 border-t border-base-200 py-4 z-20 flex gap-3 -mx-4 px-4 sm:mx-0 sm:px-0">
        <button type="button" @click="$emit('cancel')" class="btn btn-outline flex-1 sm:flex-none sm:w-32">
          取消
        </button>
        <button @click="handleSubmit" type="submit" :disabled="isSubmitting" class="btn btn-primary flex-1 shadow-md shadow-primary/20">
          <span v-if="isSubmitting" class="loading loading-spinner"></span>
          {{ isSubmitting ? '保存中...' : '确认添加' }}
        </button>
      </div>
    </div>

    <Teleport to="body">
      <EditFoodTagCard :show="showAddCategoryModal" modal-type="add" @close="closeAddCategoryModal"
        @submit="handleAddCategory" />
    </Teleport>

  </div>
</template>

<script setup>
import { ref, reactive, watch, getCurrentInstance, onMounted } from 'vue';
import { httpGet, httpPost, uploadFiles } from '@/utils/fetch.js';
import toast from '@/utils/toast';
import { useStoreContext } from '@/stores/storeContext.js';
import EditFoodTagCard from './EditFoodTagCard.vue';
import ImageUpload from '@/components/upload/ImageUpload.vue';
const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({})
  },
  isEdit: {
    type: Boolean,
    default: false
  }
});

// 从API获取的标签列表
const categories = ref([]);
const isLoadingTags = ref(false);
const emit = defineEmits(['submit', 'cancel', 'update:modelValue']);
// 表单数据
const formData = reactive({
  name: '',
  description: '',
  topImage: '',
  imageList: [],
  fileDataList: [],
  priceType: 0,
  categoryIds: [],
  stock: -1,
  status: 1,
  unitPrice: 0,
  specGroups: [
    {
      name: '',
      values: [
        { name: '', price: 0 }
      ]
    }
  ]
});

const formDataInit = reactive({
  name: '',
  description: '',
  topImage: '',
  imageList: [],
  fileDataList: [],
  priceType: 0,
  categoryIds: [],
  stock: -1,
  status: 1,
  unitPrice: 0,
  specGroups: [
    {
      name: '',
      values: [
        { name: '', price: 0 }
      ]
    }
  ]
});
const isSubmitting = ref(false);
const selectedCategories = ref([]);
const showAddCategoryModal = ref(false);

const handleTopImageSuccess = (response) => {
  if (response?.data && Array.isArray(response.data) && response.data.length > 0) {
    formData.topImage = response.data[0].fileUrl;
    // 将完整对象保存到 fileDataList
    formData.fileDataList.push(...response.data);
  }
};

const handleTopImageRemove = (data) => {
  // 清空封面图
  formData.topImage = '';
  // 从 fileDataList 中移除对应文件
  if (data.fileData && Array.isArray(data.fileData)) {
    data.fileData.forEach(removedFile => {
      const index = formData.fileDataList.findIndex(item => item.fileUrl === removedFile.fileUrl);
      if (index > -1) {
        formData.fileDataList.splice(index, 1);
      }
    });
  }
};

const handleDetailImageSuccess = (response) => {
  if (response?.data && Array.isArray(response.data)) {
    const urls = response.data.map(file => file.fileUrl).filter(Boolean);
    formData.imageList = [...formData.imageList, ...urls];
    // 将完整对象保存到 fileDataList
    formData.fileDataList.push(...response.data);
  }
};

const handleDetailImageRemove = (data) => {
  if (data.fileData && Array.isArray(data.fileData)) {
    data.fileData.forEach(removedFile => {
      // 1. 从总文件数据池中移除
      const index = formData.fileDataList.findIndex(item => item.fileUrl === removedFile.fileUrl);
      if (index > -1) {
        formData.fileDataList.splice(index, 1);
      }
      
      // 2. 直接从 imageList 数组中精确定位并移除
      const imgIndex = formData.imageList.findIndex(url => url === removedFile.fileUrl);
      if (imgIndex > -1) {
        formData.imageList.splice(imgIndex, 1);
      }
    });
  }
};



const handleUploadError = (error) => {
  console.error('图片上传失败:', error);
  toast.error('图片上传失败，请稍后重试');
};

// 打开新增分类弹窗
const openAddCategoryModal = () => {
  showAddCategoryModal.value = true;
};

// 关闭新增分类弹窗
const closeAddCategoryModal = () => {
  showAddCategoryModal.value = false;
};

// 处理新增分类成功
const handleAddCategory = async (payload) => {
  try {
    const url = payload.id ? '/foodtag/update' : '/foodtag/add';
    const res = await httpPost(url, payload);
    toast.success(payload.id ? '分类更新成功' : '分类添加成功');
    closeAddCategoryModal();
    // 重新获取分类列表
    await fetchTags();
    // 如果是新增的分类，自动选中
    if (!payload.id && res.data && res.data.id) {
      toggleCategory({ id: res.data.id, name: payload.name });
    }
  } catch (error) {
    console.error('保存分类失败:', error);
    toast.error('保存分类失败，请稍后重试');
  }
};

// 监听 modelValue 变化（用于编辑场景）
watch(() => props.modelValue, (newVal) => {
  if (newVal && Object.keys(newVal).length > 0) {
    Object.assign(formData, newVal);
    if (newVal.categoryIds && Array.isArray(newVal.categoryIds)) {
      selectedCategories.value = newVal.categoryIds.map(c => c.id);
    }
  }
}, { immediate: true, deep: true });
// 切换分类选择
const toggleCategory = (category) => {
  const index = selectedCategories.value.indexOf(category.id);
  if (index > -1) {
    selectedCategories.value.splice(index, 1);
  }
  else {
    selectedCategories.value.push(category.id);
  }
  // 更新 formData
  formData.categoryIds = selectedCategories.value.map(id => ({ id }));
};
// 添加规格组
const addSpecGroup = () => {
  formData.specGroups.push({
    name: '',
    values: [
      { name: '', price: 0 }
    ]
  });
};
// 删除规格组
const removeSpecGroup = (index) => {
  formData.specGroups.splice(index, 1);
};
// 添加规格选项
const addSpec = (groupIndex) => {
  formData.specGroups[groupIndex].values.push({ name: '', price: 0 });
};
// 删除规格选项
const removeSpec = (groupIndex, specIndex) => {
  formData.specGroups[groupIndex].values.splice(specIndex, 1);
};
// 获取标签列表
const fetchTags = async () => {
  isLoadingTags.value = true;
  const json = await httpGet('/foodtag/list');
  if (json && json.status === 200 && json.data) {
    // 将对象转换为数组
    categories.value = json.data;
    isLoadingTags.value = false;
  }
  else {
    console.error('获取标签失败:', json?.message || '未知错误');
  }

};

// 提交表单
const handleSubmit = async () => {
  //isSubmitting.value = true;
  // 附加当前管理门店 ID（菜品归属门店，后端据此做数据隔离与权限校验）
  const storeContext = useStoreContext();
  if (!storeContext.currentStoreId) {
    toast.error('请先选择门店');
    return;
  }
  formData.storefrontId = storeContext.currentStoreId;
  // 提交表单数据
  const json = await httpPost('/dishes/add', formData);
  if (json && json.status === 200) {
    isSubmitting.value = false;
    // 重置表单数据
    Object.assign(formData, formDataInit);
    emit('submit', json.data);
  }
  else {
    toast.error(json?.message || '提交失败');
  }
};

// 组件挂载时获取标签列表
onMounted(() => {
  fetchTags();
});
</script>