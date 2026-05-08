<template>
  <!-- 规格配置弹窗 -->
  <dialog class="modal" :class="{ 'modal-open': showModal }">
    <div class="modal-box max-w-2xl w-full">
      <div class="bg-base-100 rounded-box relative">
        <!-- 头部标题 -->
        <div class="flex justify-between items-center mb-6 pb-4 border-b border-base-200">
          <h3 class="text-xl font-bold flex items-center gap-2">
            <span class="w-1.5 h-6 bg-primary rounded-full"></span>
            规格配置修改
          </h3>
          <button @click="handleClose" type="button"
            class="btn btn-circle btn-ghost btn-sm bg-base-200/50 hover:bg-base-300">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div class="alert alert-warning shadow-lg text-sm py-3">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 shrink-0" fill="none" viewBox="0 0 24 24"
            stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <div class="flex flex-col gap-1">
            <span class="font-semibold">注意</span>
            <span>只推荐新增或修改已存在规格组，<strong>不建议删除规格组</strong>，删除规格组会造成相关 SKU 不可用！</span>
          </div>
        </div>

        <br/>

        <form @submit.prevent="handleSubmit" class="flex-1 flex flex-col">
          <div class="space-y-8 pb-24">
            <!-- 规格配置区域 -->
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

              <!-- 空状态 -->
              <div v-if="specGroups.length === 0"
                class="text-center py-6 bg-base-200/20 rounded-xl border border-dashed border-base-300 text-base-content/50 text-sm">
                暂无规格，点击上方按钮添加（如：大小、口味、温度）
              </div>

              <!-- 规格组列表 -->
              <div class="space-y-4">
                <div v-for="(group, groupIndex) in specGroups" :key="group.id || groupIndex"
                  class="card bg-base-200/30 border border-base-200 shadow-sm transition-all hover:border-base-300">
                  <div class="card-body p-4 sm:p-5">
                    <!-- 规格组头部 -->
                    <div class="flex flex-wrap sm:flex-nowrap gap-3 items-center mb-4 pb-3 border-b border-base-200/50">
                      <span class="badge badge-neutral text-xs font-bold w-12 shrink-0">组 {{ groupIndex + 1 }}</span>
                      <input v-model="group.name" type="text" placeholder="规格名 (例: 辣度, 杯型)"
                        class="input input-sm input-bordered flex-1 focus:input-primary min-w-[120px]" />
                      <!-- 仅当存在多个规格组时显示删除按钮 -->
                      <button v-if="specGroups.length > 1" type="button" @click="removeSpecGroup(groupIndex)"
                        class="btn btn-sm btn-circle btn-ghost text-error ml-auto tooltip" data-tip="删除该组">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                          stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                        </svg>
                      </button>
                    </div>

                    <!-- 规格选项列表 -->
                    <div class="space-y-3">
                      <div v-for="(spec, specIndex) in group.values" :key="spec.id || specIndex"
                        class="flex items-center gap-2 sm:gap-3">
                        <input v-model="spec.name" type="text" placeholder="选项名称 (例: 微辣)"
                          class="input input-sm input-bordered flex-1 w-0 focus:input-primary" />
                        <!-- 仅当存在多个选项时显示删除按钮 -->
                        <button v-if="group.values.length > 1" type="button" @click="removeSpec(groupIndex, specIndex)"
                          class="btn btn-sm btn-square btn-ghost text-base-content/40 hover:text-error hover:bg-error/10 shrink-0">
                          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24"
                            stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                              d="M6 18L18 6M6 6l12 12" />
                          </svg>
                        </button>
                      </div>

                      <!-- 添加规格选项按钮 -->
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

          <!-- 底部操作栏 -->
          <div class="sticky bottom-0 bg-base-100 border-t border-base-200 py-4 z-20 flex gap-3 -mx-4 px-4 sm:mx-0 sm:px-0">
            <button type="button" @click="handleClose" class="btn btn-outline flex-1 sm:flex-none sm:w-32">
              取消
            </button>
            <button type="submit" :disabled="isSubmitting" class="btn btn-primary flex-1 shadow-md shadow-primary/20">
              <span v-if="isSubmitting" class="loading loading-spinner"></span>
              {{ isSubmitting ? '提交中...' : '保存规格配置' }}
            </button>
          </div>
        </form>
      </div>
    </div>
    <div class="modal-backdrop" @click="!isSubmitting && handleClose()"></div>
  </dialog>
</template>

<script setup>
import { ref, reactive, watch, toRefs } from 'vue';
import { httpPost } from '@/utils/fetch.js';
import toast from '@/utils/toast';

// 定义Props：接收菜品ID、规格配置数据、弹窗显示状态
const props = defineProps({
  dishesId: {
    type: [Number, String],
    required: true,
    validator: (val) => val > 0 // 确保菜品ID有效
  },
  specData: {
    type: Object,
    default: () => ({ specGroups: [] }),
    validator: (val) => Array.isArray(val.specGroups) // 确保specGroups是数组
  },
  show: { // 新增：控制弹窗显示
    type: Boolean,
    default: false
  }
});

// 定义Emits：提交成功、关闭弹窗
const emit = defineEmits(['submit', 'close']);

// 响应式数据：规格组（深拷贝避免修改原数据）
const specGroups = reactive(JSON.parse(JSON.stringify(props.specData.specGroups)));
// 提交状态
const isSubmitting = ref(false);
// 弹窗显示状态
const showModal = ref(false);

// 监听父组件show变化，同步弹窗显示状态
watch(
  () => props.show,
  (newVal) => {
    showModal.value = newVal;
  },
  { immediate: true }
);

// 监听父组件specData变化，同步更新（编辑场景）
watch(
  () => props.specData,
  (newVal) => {
    if (newVal?.specGroups) {
      Object.assign(specGroups, JSON.parse(JSON.stringify(newVal.specGroups)));
    }
  },
  { immediate: true, deep: true }
);

// 关闭弹窗
const handleClose = () => {
  showModal.value = false;
  emit('close');
};

// 添加规格组
const addSpecGroup = () => {
  specGroups.push({
    dishId: props.dishesId,
    id: null,
    name: '',
    sort: 0,
    values: [
      {
        groupId: null,
        id: null,
        name: '',
        sort: 0
      }
    ]
  });
};

// 删除规格组
const removeSpecGroup = (index) => {
  specGroups.splice(index, 1);
};

// 添加规格选项
const addSpec = (groupIndex) => {
  const group = specGroups[groupIndex];
  group.values.push({
    groupId: group.id || null,
    id: null,
    name: '',
    sort: 0
  });
};

// 删除规格选项
const removeSpec = (groupIndex, specIndex) => {
  specGroups[groupIndex].values.splice(specIndex, 1);
};

// 提交规格配置
const handleSubmit = async () => {
  if (specGroups.length === 0) {
    toast.warning('请至少配置一个规格组');
    return;
  }

  // 校验必填项
  const hasEmptyGroupName = specGroups.some(group => !group.name.trim());
  const hasEmptySpecName = specGroups.some(group => 
    group.values.some(spec => !spec.name.trim())
  );
  if (hasEmptyGroupName || hasEmptySpecName) {
    toast.error('规格组名称和选项名称不能为空');
    return;
  }

  isSubmitting.value = true;
  try {
    // 构造提交数据
    const submitData = {
      dishesId: Number(props.dishesId),
      specGroups: specGroups.map(group => ({
        ...group,
        // 确保ID为数字类型，临时ID可由后端处理
        id: group.id ? Number(group.id) : undefined,
        values: group.values.map(spec => ({
          ...spec,
          id: spec.id ? Number(spec.id) : undefined,
          groupId: spec.groupId ? Number(spec.groupId) : undefined
        }))
      }))
    };
    console.log(submitData);
    // 调用后端接口
    const res = await httpPost('/sku/adjust', submitData);
    if (res?.status === 200) {
      toast.success('规格配置修改成功');
      emit('submit', submitData); // 向父组件传递提交成功的数据
      handleClose(); // 提交成功后关闭弹窗
    } else {
      toast.error(res?.message || '规格配置修改失败');
    }
  } catch (error) {
    console.error('提交规格配置失败:', error);
    toast.error('提交失败，请稍后重试');
  } finally {
    isSubmitting.value = false;
  }
};
</script>