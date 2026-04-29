<template>
  <div>
    <div class="flex justify-between items-center mb-6">
      <h3 class="text-xl font-bold">{{ isEdit ? '编辑菜品' : '添加菜品' }}</h3>
      <button @click="$emit('cancel')" class="btn btn-circle btn-ghost btn-sm">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>

    <form @submit.prevent="handleSubmit" class="space-y-5">
      <!-- 菜品名称 -->
      <div class="form-control">
        <label class="label">
          <span class="label-text">菜品名称</span>
          <span class="label-text-alt text-error">必填</span>
        </label>
        <input v-model="formData.name" type="text" placeholder="请输入菜品名称" class="input input-bordered" required />
      </div>

      <!-- 菜品描述 -->
      <div class="form-control">
        <label class="label">
          <span class="label-text">菜品描述</span>
        </label>
        <textarea v-model="formData.description" placeholder="请输入菜品描述" class="textarea textarea-bordered" rows="3"></textarea>
      </div>

      <!-- 首选显示图片 -->
      <div class="form-control">
        <label class="label">
          <span class="label-text">首选显示图片URL</span>
        </label>
        <input v-model="formData.topImage" type="text" placeholder="请输入首选图片URL" class="input input-bordered" />
      </div>

      <!-- 其它图片列表 -->
      <div class="form-control">
        <label class="label">
          <span class="label-text">其它图片URL</span>
          <span class="label-text-alt">多个用英文逗号分隔</span>
        </label>
        <input v-model="imageListText" type="text" placeholder="图片URL1,图片URL2,图片URL3" class="input input-bordered" />
      </div>

      <!-- 计价方式和默认价格 -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <!-- 计价方式 -->
        <div class="form-control">
          <label class="label">
            <span class="label-text">计价方式</span>
            <span class="label-text-alt text-error">必填</span>
          </label>
          <select v-model="formData.priceType" class="select select-bordered" required>
            <option :value="0">按份</option>
            <option :value="1">按斤</option>
            <option :value="2">按时</option>
          </select>
        </div>

        <!-- 默认价格 -->
        <div class="form-control">
          <label class="label">
            <span class="label-text">默认价格</span>
          </label>
          <input v-model.number="formData.unitPrice" type="number" placeholder="请输入默认价格" class="input input-bordered" min="0" step="0.01" />
        </div>
      </div>

      <!-- 分类选择 -->
      <div class="form-control">
        <label class="label">
          <span class="label-text">分类标签</span>
        </label>
        <div class="flex flex-wrap gap-2">
          <span v-if="isLoadingTags" class="loading loading-spinner"></span>
          <button v-for="category in categories" :key="category.id" type="button" @click="toggleCategory(category)"
            :class="[
              'btn',
              selectedCategories.includes(category.id)
                ? 'btn-primary'
                : 'btn-outline btn-primary'
            ]">
            {{ category.name }}
          </button>
          <span v-if="!isLoadingTags && categories.length === 0" class="text-gray-500">暂无标签</span>
        </div>
      </div>

      <!-- 菜品状态 -->
      <div class="form-control">
        <label class="label">
          <span class="label-text">菜品状态</span>
          <span class="label-text-alt text-error">必填</span>
        </label>
        <select v-model="formData.status" class="select select-bordered" required>
          <option :value="0">下架</option>
          <option :value="1">上架</option>
        </select>
      </div>

      <!-- 规格分组 -->
      <div class="form-control">
        <label class="label">
          <span class="label-text">规格配置</span>
        </label>

        <!-- 添加规格组按钮 -->
        <button type="button" @click="addSpecGroup" class="btn btn-outline btn-secondary w-full mb-4">
          + 添加规格组
        </button>

        <!-- 规格组列表 -->
        <div v-for="(group, groupIndex) in formData.specGroups" :key="groupIndex" class="space-y-4 mb-4">
          <div class="card bg-base-50 border border-base-200">
            <div class="card-body">
              <div class="flex justify-between items-center mb-4">
                <h4 class="font-semibold text-base">规格组 {{ groupIndex + 1 }}</h4>
                <button v-if="formData.specGroups.length > 1" type="button" @click="removeSpecGroup(groupIndex)"
                  class="btn btn-ghost btn-xs text-error">
                  删除规格组
                </button>
              </div>

              <!-- 规格组名称 -->
              <input v-model="group.groupName" type="text" placeholder="请输入规格组名称（如：规格、口味）"
                class="input input-bordered w-full mb-4" />

              <!-- 规格选项 -->
              <div class="space-y-2">
                <div v-for="(spec, specIndex) in group.specs" :key="specIndex" class="flex gap-2">
                  <input v-model="spec.name" type="text" placeholder="规格名称" class="input input-bordered flex-1" />
                  <!-- <div class="relative w-36">
                    <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500">+¥</span>
                    <input v-model.number="spec.price" type="number" placeholder="加价金额" class="input input-bordered w-full pl-8"
                      min="0" step="0.01" />
                  </div> -->
                  <button v-if="group.specs.length > 1" type="button" @click="removeSpec(groupIndex, specIndex)"
                    class="btn btn-ghost btn-xs text-error h-10">
                    删除
                  </button>
                </div>
                <button type="button" @click="addSpec(groupIndex)" class="btn btn-outline btn-xs w-full">
                  + 添加规格选项
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 提交按钮 -->
      <div class="flex gap-3 pt-4">
        <button type="submit" :disabled="isSubmitting" class="btn btn-primary flex-1">
          <span v-if="isSubmitting" class="loading loading-spinner"></span>
          {{ isSubmitting ? '提交中...' : '提交' }}
        </button>
        <button type="button" @click="$emit('cancel')" class="btn btn-outline">
          取消
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, watch, getCurrentInstance, onMounted } from 'vue';
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
  priceType: 0,
  categoryIds: [],
  stock: -1,
  status: 1,
  unitPrice: 0,
  specGroups: [
    {
      groupName: '',
      specs: [
        { name: '', price: 0 }
      ]
    }
  ]
});
// 图片列表文本（用于输入）
const imageListText = ref('');
// 是否正在提交
const isSubmitting = ref(false);
// 选中的分类ID
const selectedCategories = ref([]);
// 监听 modelValue 变化（用于编辑场景）
watch(() => props.modelValue, (newVal) => {
  if (newVal && Object.keys(newVal).length > 0) {
    Object.assign(formData, newVal);
    if (newVal.imageList && Array.isArray(newVal.imageList)) {
      imageListText.value = newVal.imageList.join(',');
    }
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
    groupName: '',
    specs: [
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
  formData.specGroups[groupIndex].specs.push({ name: '', price: 0 });
};
// 删除规格选项
const removeSpec = (groupIndex, specIndex) => {
  formData.specGroups[groupIndex].specs.splice(specIndex, 1);
};
// 获取标签列表
const fetchTags = () => {
  isLoadingTags.value = true;
  const vm = getCurrentInstance().proxy;
  vm.httpGet('/foodtag/map', (response) => {
    isLoadingTags.value = false;
    if (response && response.status === 200 && response.data) {
      // 将对象转换为数组
      categories.value = Object.values(response.data);
    }
    else {
      console.error('获取标签失败:', response?.message || '未知错误');
    }
  });
};

// 提交表单
const handleSubmit = () => {
  // 处理图片列表
  if (imageListText.value.trim()) {
    formData.imageList = imageListText.value.split(',').map(url => url.trim()).filter(url => url);
  }
  else {
    formData.imageList = [];
  }
  isSubmitting.value = true;
  // 使用全局 httpPost 方法
  const vm = getCurrentInstance().proxy;
  vm.httpPost('/dishes/add', formData, (response) => {
    isSubmitting.value = false;
    if (response && response.status === 1) {
      emit('submit', response.data);
    }
    else {
      alert(response?.message || '提交失败');
    }
  });
};

// 组件挂载时获取标签列表
onMounted(() => {
  fetchTags();
});
</script>