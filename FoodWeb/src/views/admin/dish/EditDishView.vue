<template>
  <div>
    <!-- 顶部标题 -->
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-base-content flex items-center gap-2">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-primary" fill="none" viewBox="0 0 24 24"
            stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
          </svg>
          编辑菜品信息
        </h1>
        <p class="text-sm text-base-content/60 mt-1">修改基本信息、规格及 SKU 售卖属性</p>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="isLoading" class="flex justify-center items-center h-64">
      <span class="loading loading-spinner loading-lg text-primary"></span>
    </div>

    <form v-else @submit.prevent="handleSave" class="space-y-6">

      <!-- 模块 1：基本信息 -->
      <div class="card bg-base-100 shadow-sm border border-base-200">
        <div class="card-body">
          <h2 class="card-title text-lg border-b border-base-200 pb-2 mb-4">🍽️ 基本信息</h2>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div class="form-control">
              <label class="label"><span class="label-text font-medium">菜品名称</span></label>
              <input v-model="formData.food.name" type="text" class="input input-bordered focus:input-primary"
                required />
            </div>

            <div class="form-control">
              <label class="label"><span class="label-text font-medium">菜品基础价格 (¥)</span></label>
              <input v-model.number="formData.food.basePrice" type="number" step="0.01"
                class="input input-bordered focus:input-primary" required />
            </div>

            <div class="form-control md:col-span-2">
              <label class="label"><span class="label-text font-medium">菜品描述</span></label>
              <textarea v-model="formData.food.description"
                class="textarea textarea-bordered focus:textarea-primary h-24" placeholder="诱人的描述能增加销量..."></textarea>
            </div>

            <div class="form-control">
              <label class="label"><span class="label-text font-medium">封面首图 URL</span></label>
              <input v-model="formData.food.topImage" type="url" class="input input-bordered focus:input-primary" />
            </div>

            <div class="form-control">
              <label class="label">
                <span class="label-text font-medium">轮播图列表 URLs</span>
                <span class="label-text-alt text-base-content/50">多个请用逗号分隔</span>
              </label>
              <input v-model="uiState.imageListStr" type="text" class="input input-bordered focus:input-primary" />
            </div>

            <div class="form-control">
              <label class="label"><span class="label-text font-medium">菜品状态</span></label>
              <label class="cursor-pointer label justify-start gap-4">
                <input v-model="formData.food.status" type="checkbox" :true-value="1" :false-value="0"
                  class="toggle toggle-success" />
                <span class="label-text"
                  :class="formData.food.status === 1 ? 'text-success font-bold' : 'text-base-content/50'">
                  {{ formData.food.status === 1 ? '上架中' : '已下架' }}
                </span>
              </label>
            </div>
          </div>
        </div>
      </div>

      <!-- 模块 2：分类信息修改 -->
      <div class="card bg-base-100 shadow-sm border border-base-200">
        <div class="card-body">
          <div class="flex justify-between items-center border-b border-base-200 pb-2 mb-4">
            <h2 class="card-title text-lg">🏷️ 分类设置</h2>
            <button type="button" @click="showAddCategoryModal = true"
              class="btn btn-xs btn-outline btn-primary rounded-full">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24"
                stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
              新增分类
            </button>
          </div>

          <div class="flex flex-wrap gap-3 items-center min-h-[3rem]">
            <!-- 加载状态 -->
            <span v-if="isLoadingTags" class="loading loading-dots loading-sm text-primary"></span>

            <!-- 分类标签选择器 -->
            <template v-else>
              <button v-for="cat in categories" :key="cat.id" type="button" @click="toggleCategory(cat)" :class="[
                'badge badge-lg py-4 px-4 cursor-pointer transition-all gap-2 border',
                isCategorySelected(cat.id)
                  ? 'badge-primary text-primary-content border-none shadow-md'
                  : 'badge-ghost bg-base-200/50 border-base-300 hover:border-primary/50'
              ]">
                <svg v-if="isCategorySelected(cat.id)" xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5"
                  viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd"
                    d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                    clip-rule="evenodd" />
                </svg>
                {{ cat.name }}
              </button>

              <!-- 无分类占位 -->
              <p v-if="categories.length === 0" class="text-sm text-base-content/40 italic">暂无可选分类，请先创建</p>
            </template>
          </div>
        </div>
      </div>

      <!-- 分类编辑/新增弹窗 -->
      <Teleport to="body">
        <EditFoodTagCard :show="showAddCategoryModal" modal-type="add" @close="showAddCategoryModal = false"
          @submit="handleAddCategory" />
      </Teleport>

      <!-- 模块 3：SKU 详细配置 (核心要求) -->
      <div class="card bg-base-100 shadow-sm border border-base-200">
        <div class="card-body p-0 sm:p-8">
          <h2 class="card-title text-lg border-b border-base-200 pb-2 mb-4 px-4 sm:px-0 pt-4 sm:pt-0">📦 SKU
            价格与库存管理</h2>

          <!-- 🟢 新增：交互式筛选器区域 -->
          <div v-if="formData.specGroups && formData.specGroups.length > 0"
            class="mx-4 sm:mx-0 mb-4 p-4 bg-base-200/40 rounded-xl border border-base-200">
            <div class="flex items-center justify-between mb-3">
              <h3 class="text-sm font-bold text-base-content/70 flex items-center gap-2">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                  stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
                </svg>
                快速筛选 SKU
              </h3>
              <button v-if="selectedFilterSpecs.length > 0" @click="clearFilters" type="button"
                class="btn btn-xs btn-ghost text-error/80 hover:text-error hover:bg-error/10">
                清空筛选
              </button>
            </div>

            <div class="space-y-3">
              <!-- 遍历每个规格组生成筛选按钮 -->
              <div v-for="(group, gIdx) in formData.specGroups" :key="gIdx" class="flex flex-wrap items-center gap-2">
                <span class="text-xs font-medium text-base-content/50 w-10 shrink-0">{{ group.name }}:</span>
                <button v-for="spec in group.values" :key="spec.id" type="button" @click="toggleFilterSpec(spec.id)"
                  class="badge badge-md transition-all cursor-pointer select-none gap-1 py-3 px-3" :class="[
                    selectedFilterSpecs.includes(spec.id)
                      ? `${specDict[spec.id].colorClass} text-primary-content border-none shadow-md scale-105 font-bold`
                      : 'badge-outline border-base-300 text-base-content/60 hover:border-primary/50'
                  ]">
                  <!-- 增加一个选中的小图标，增强辨识度 -->
                  <svg v-if="selectedFilterSpecs.includes(spec.id)" xmlns="http://www.w3.org/2000/svg" class="h-3 w-3"
                    viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd"
                      d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                      clip-rule="evenodd" />
                  </svg>
                  {{ spec.name }}
                </button>
              </div>
            </div>
          </div>
          <!-- 筛选器结束 -->

          <div class="overflow-x-auto">
            <table class="table table-zebra table-sm sm:table-md w-full">
              <thead class="bg-base-200/50 text-base-content">
                <tr>
                  <th>规格组合</th>
                  <th class="min-w-[120px]">单价 (¥)</th>
                  <th class="min-w-[120px]">库存<br /><span class="text-xs font-normal opacity-70">(-1为无限)</span></th>
                  <th class="min-w-[100px]">折扣<br /><span class="text-xs font-normal opacity-70">(0~1)</span></th>
                  <th class="min-w-[200px]">折扣到期时间</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="sku in filteredSkuList" :key="sku.id" :class="{ 'opacity-60': sku.status === 0 }">
                  <!-- 规格解析展示 -->
                  <td class="font-medium">
                    <div class="flex flex-wrap gap-1">
                      <!-- 改为遍历 getSkuSpecDetails，并动态绑定 colorClass -->
                      <span v-for="(spec, idx) in getSkuSpecDetails(sku._specsArr)" :key="idx"
                        class="badge badge-sm border-transparent shadow-sm text-white" :class="spec.colorClass">
                        {{ spec.name }}
                      </span>
                    </div>
                  </td>

                  <!-- 单价 -->
                  <td>
                    <input v-model.number="sku.unitPrice" type="number" step="0.01"
                      class="input input-sm input-bordered w-full max-w-[100px]" />
                  </td>

                  <!-- 库存 -->
                  <td>
                    <input v-model.number="sku.stock" type="number"
                      class="input input-sm input-bordered w-full max-w-[100px]" />
                  </td>

                  <!-- 折扣 -->
                  <td>
                    <input v-model.number="sku.discount" type="number" step="0.01" max="1" min="0"
                      class="input input-sm input-bordered w-full max-w-[80px]" />
                  </td>

                  <!-- 折扣到期时间 -->
                  <td>
                    <div class="flex flex-col gap-1">
                      <label class="cursor-pointer label p-0 justify-start gap-2">
                        <input type="checkbox" v-model="sku._isPermanent"
                          class="checkbox checkbox-xs checkbox-primary" />
                        <span class="label-text text-xs">永久优惠 (无期限)</span>
                      </label>
                      <input v-model="sku._endTimeStr" type="datetime-local" :disabled="sku._isPermanent"
                        class="input input-sm input-bordered w-full text-xs" />
                    </div>
                  </td>

                  <!-- SKU 状态 -->
                  <td>
                    <div class="flex flex-col items-center gap-1">
                      <input v-model="sku.status" type="checkbox" :true-value="1" :false-value="0"
                        class="toggle toggle-sm toggle-success" />
                      <span class="text-[10px]" :class="sku.status === 1 ? 'text-success' : 'text-base-content/50'">
                        {{ sku.status === 1 ? '上架' : '下架' }}
                      </span>
                    </div>
                  </td>
                </tr>
                <!-- 🟢 新增：当筛选结果为空时的友好提示 -->
                <tr v-if="filteredSkuList.length === 0">
                  <td colspan="6" class="text-center py-8 text-base-content/50">
                    没有找到符合该筛选条件的规格组合，请尝试取消部分筛选条件
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 底部悬浮操作栏 -->
      <div class="card bg-base-100 shadow-sm border border-base-200">
        <!-- <button type="button" @click="resetForm" class="btn btn-ghost w-1/3 sm:w-auto">重置修改</button> -->
        <button type="submit" class="btn btn-primary shadow-lg  text-lg">
          保存菜品数据
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import EditFoodTagCard from '@/components/card/EditFoodTagCard.vue';
import { httpGet } from '@/utils/fetch.js'; // 你的 fetch 工具类
import toast from '@/utils/toast.js';       // 你的 toast 工具类
import { formatDate } from '@/utils/format.js';
// 如果使用路由传参，请按需引入 useRoute
// const route = useRoute();
// const dishId = route.params.id;
const dishId = 1; // 演示写死 ID 为 1

const isLoading = ref(true);

// 核心表单数据对象
const formData = reactive({
  food: {},
  skuList: [],
  specGroups: []
});

// UI专属的状态绑定（非直接提交字段，需做转换）
const uiState = reactive({
  imageListStr: ''
});



// 初始化获取数据
const fetchDishDetail = async () => {
  isLoading.value = true;
  try {
    // 调用实际 API: 获取菜品数据
    const res = await httpGet(`/dishes/${dishId}`);

    if (res && res.status === 200 && res.data) {
      initData(res.data);
    } else {
      toast.error('获取菜品数据失败');
    }
  } catch (error) {
    console.error('Fetch error:', error);
    toast.error('网络请求错误，请检查网络');
  } finally {
    isLoading.value = false;
  }
};

// 数据装载与 UI 状态预处理
const initData = (data) => {
  // 1. 装载基础与分类
  formData.food = data.food;
  formData.specGroups = data.specGroups;

  // 2. 转换图片数组 -> 字符串，方便在 input 展示修改
  try {
    const imagesArr = JSON.parse(formData.food.imageList || '[]');
    uiState.imageListStr = imagesArr.join(', ');
  } catch (e) {
    uiState.imageListStr = formData.food.imageList;
  }

  // 3. 处理 skuList，注入 Vue 绑定的辅助属性
  formData.skuList = data.skuList.map(sku => {
    // 提取出 spec 数组用于名称映射渲染
    const specsArr = JSON.parse(sku.specs || '[]');

    return {
      ...sku,
      _specsArr: specsArr, // 渲染名称用
      _isPermanent: sku.discountEndTime === 0, // 是否为永久（0）
      _endTimeStr: sku.discountEndTime === 0 ? '' : formatDate(sku.discountEndTime) // 时间字符串绑定给 input
    };
  });
};

// 1. 升级版字典：不仅记录规格名称，还记录它属于第几个规格组
const specDict = computed(() => {
  const dict = {};
  formData.specGroups.forEach((group, index) => {
    if (group.values) {
      group.values.forEach(val => {
        dict[val.id] = {
          name: val.name,
          groupIndex: index // 记录所属分组索引，用于计算颜色
        };
      });
    }
  });
  return dict;
});

// 2. 解析 SKU 规格，并自动分配 DaisyUI 颜色类名
const getSkuSpecDetails = (specsArr) => {
  if (!specsArr || !Array.isArray(specsArr)) {
    return [{ name: '默认规格', colorClass: 'badge-ghost' }];
  }

  // 定义 DaisyUI 的颜色池 (排除了太刺眼的 error)
  const colorClasses = [
    'badge-primary',
    'badge-secondary',
    'badge-accent',
    'badge-info',
    'badge-success',
    'badge-warning'
  ];

  return specsArr.map(id => {
    const spec = specDict.value[id];
    if (spec) {
      // 根据规格组索引取模，确保即使规格组数量超过颜色池长度也不会报错，且同组颜色始终一致
      const colorClass = colorClasses[spec.groupIndex % colorClasses.length];
      return { name: spec.name, colorClass: colorClass };
    }
    return { name: '未知规格', colorClass: 'badge-ghost' };
  });
};

// 提交表单保存
const handleSave = () => {
  // 组装要提交给后端的数据 (深拷贝防止直接污染视图)
  const postData = {
    food: { ...formData.food },
    specGroups: formData.specGroups,
    skuList: formData.skuList.map(sku => {
      // 组装最终需要发送的单个 sku 对象
      const processedSku = { ...sku };

      // 处理时间：如果是永久，设为0；否则将字符串解析回时间戳
      if (sku._isPermanent || !sku._endTimeStr) {
        processedSku.discountEndTime = 0;
      } else {
        processedSku.discountEndTime = new Date(sku._endTimeStr).getTime();
      }

      // 剔除前端注入的辅助属性，保持对象整洁
      delete processedSku._specsArr;
      delete processedSku._isPermanent;
      delete processedSku._endTimeStr;

      return processedSku;
    })
  };

  // 图片字符串转换回 JSON String
  const imagesArray = uiState.imageListStr.split(',').map(s => s.trim()).filter(Boolean);
  postData.food.imageList = JSON.stringify(imagesArray);

  // 【核心要求】：由于后端暂未完成，只打印并弹出提示
  console.log('======= 准备保存的数据 =======');
  console.log(JSON.stringify(postData, null, 2));
  console.log('==============================');

  toast.success('数据已在控制台打印，请按 F12 查看！', 4000);
};

// ====== 新增：筛选器相关逻辑 ======

// 1. 记录当前选中的筛选规格 ID 列表
const selectedFilterSpecs = ref([]);

// 2. 切换选中状态
const toggleFilterSpec = (specId) => {
  const index = selectedFilterSpecs.value.indexOf(specId);
  if (index > -1) {
    // 已选中则取消
    selectedFilterSpecs.value.splice(index, 1);
  } else {
    // 未选中则添加
    selectedFilterSpecs.value.push(specId);
  }
};

// 3. 清空所有筛选
const clearFilters = () => {
  selectedFilterSpecs.value = [];
};

// 4. 计算过滤后的 SKU 列表 (替换模板中直接使用的 formData.skuList)
const filteredSkuList = computed(() => {
  // 如果没有选择任何筛选条件，返回全量数据
  if (selectedFilterSpecs.value.length === 0) {
    return formData.skuList;
  }

  // 开始过滤
  return formData.skuList.filter(sku => {
    // 逻辑：必须包含【所有】被选中的规格 (AND 逻辑)
    // 比如同时选中 "微辣" 和 "大份"，只展示同时具备这两个属性的 SKU
    return selectedFilterSpecs.value.every(filterId =>
      sku._specsArr.includes(filterId)
    );
  });
});
// ==================================

// --- 分类相关状态 ---
const categories = ref([]); // 系统所有分类
const isLoadingTags = ref(false);
const showAddCategoryModal = ref(false); // 控制新增分类弹窗

// 获取所有分类列表
const fetchTags = async () => {
  isLoadingTags.value = true;
  try {
    const res = await httpGet('/foodtag/list');
    if (res && res.status === 200) {
      categories.value = res.data;
    }
  } catch (error) {
    console.error('获取标签失败:', error);
  } finally {
    isLoadingTags.value = false;
  }
};

// 切换分类选择状态
const toggleCategory = (category) => {
  // 确保 categoryIds 存在
  if (!formData.food.categoryIds) formData.food.categoryIds = [];

  const index = formData.food.categoryIds.findIndex(c => c.id === category.id);
  if (index > -1) {
    // 已存在则移除
    formData.food.categoryIds.splice(index, 1);
  } else {
    // 不存在则添加整个对象（保持与 API 结构一致）
    formData.food.categoryIds.push({
      id: category.id,
      name: category.name
    });
  }
};

// 判断是否被选中（辅助 UI 渲染）
const isCategorySelected = (id) => {
  return formData.food.categoryIds?.some(c => c.id === id);
};

// 处理新增分类成功的回调
const handleAddCategory = async (payload) => {
  try {
    const url = payload.id ? '/foodtag/update' : '/foodtag/add';
    const res = await httpPost(url, payload);
    if (res.status === 200) {
      toast.success(payload.id ? '分类更新成功' : '分类添加成功');
      showAddCategoryModal.value = false;
      // 重新刷新列表
      await fetchTags();
      // 如果是新创建的，自动为当前菜品勾选上
      if (!payload.id && res.data) {
        toggleCategory(res.data);
      }
    }
  } catch (error) {
    toast.error('操作分类失败');
  }
};

// 页面加载触发
onMounted(() => {
  fetchDishDetail();
  fetchTags();
});
</script>

<style scoped>
/* 隐藏数字输入框自带的上下箭头，保证 UI 整洁 */
input[type="number"]::-webkit-inner-spin-button,
input[type="number"]::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

input[type="number"] {
  -moz-appearance: textfield;
}
</style>