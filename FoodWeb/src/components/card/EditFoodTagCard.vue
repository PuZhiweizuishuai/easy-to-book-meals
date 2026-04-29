<template>
  <!-- 新增/编辑分类弹窗 -->
  <dialog class="modal" :class="{ 'modal-open': showModal }">
    <div class="modal-box">
      <h3 class="font-bold text-lg mb-4">{{ modalType === 'add' ? '添加分类' : '编辑分类' }}</h3>
      
      <form @submit.prevent="handleSubmit" class="flex flex-col gap-4">
        <div class="form-control w-full">
          <label class="label"><span class="label-text font-medium">分类名称 <span class="text-error">*</span></span></label>
          <input 
            v-model="formData.name" 
            type="text" 
            placeholder="例如：招牌热菜" 
            class="input input-bordered w-full focus:input-primary" 
            required
          />
        </div>

        <div class="form-control w-full">
          <label class="label">
            <span class="label-text font-medium">排序数值</span>
            <span class="label-text-alt text-base-content/60">数值越小越靠前</span>
          </label>
          <input 
            v-model.number="formData.sort" 
            type="number" 
            placeholder="默认0" 
            class="input input-bordered w-full focus:input-primary" 
          />
        </div>

        <div class="modal-action mt-6">
          <button type="button" class="btn btn-ghost" @click="handleClose" :disabled="submitting">取消</button>
          <button type="submit" class="btn btn-primary" :disabled="submitting">
            <span v-if="submitting" class="loading loading-spinner loading-xs"></span>
            保存
          </button>
        </div>
      </form>
    </div>
    <div class="modal-backdrop" @click="!submitting && handleClose()"></div>
  </dialog>

  <!-- 删除确认弹窗 -->
  <dialog class="modal" :class="{ 'modal-open': showDeleteModal }">
    <div class="modal-box">
      <h3 class="font-bold text-lg mb-4">确认删除</h3>
      <p class="text-base-content/70">确定要删除分类「{{ deleteTarget.name }}」吗？此操作不可恢复。</p>
      <div class="modal-action mt-6">
        <button type="button" class="btn btn-ghost" @click="handleCloseDelete">取消</button>
        <button type="button" class="btn btn-error" @click="handleConfirmDelete" :disabled="submitting">
          <span v-if="submitting" class="loading loading-spinner loading-xs"></span>
          删除
        </button>
      </div>
    </div>
    <div class="modal-backdrop" @click="!submitting && handleCloseDelete()"></div>
  </dialog>
</template>

<script setup>
import { ref, watch } from 'vue';

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  modalType: {
    type: String,
    default: 'add', // 'add' | 'edit'
    validator: (value) => ['add', 'edit'].includes(value)
  },
  tag: {
    type: Object,
    default: null
  },
  showDelete: {
    type: Boolean,
    default: false
  },
  deleteTarget: {
    type: Object,
    default: () => ({ id: null, name: '' })
  }
});

const emit = defineEmits(['close', 'submit', 'delete-close', 'delete-confirm']);

const submitting = ref(false);
const showModal = ref(false);
const showDeleteModal = ref(false);
const formData = ref({
  id: null,
  name: '',
  sort: 0
});

watch(() => props.show, (newVal) => {
  showModal.value = newVal;
  if (newVal) {
    if (props.modalType === 'edit' && props.tag) {
      formData.value = {
        id: props.tag.id,
        name: props.tag.name,
        sort: props.tag.sort || 0
      };
    } else {
      formData.value = { id: null, name: '', sort: 0 };
    }
  }
});

watch(() => props.showDelete, (newVal) => {
  showDeleteModal.value = newVal;
});

const handleClose = () => {
  showModal.value = false;
  emit('close');
};

const handleSubmit = async () => {
  if (!formData.value.name.trim()) {
    return;
  }

  submitting.value = true;
  try {
    const payload = {
      ...formData.value,
      sort: formData.value.sort === '' ? 0 : formData.value.sort
    };
    emit('submit', payload);
  } finally {
    submitting.value = false;
  }
};

const handleCloseDelete = () => {
  showDeleteModal.value = false;
  emit('delete-close');
};

const handleConfirmDelete = async () => {
  const { id } = props.deleteTarget;
  if (!id) return;

  submitting.value = true;
  try {
    emit('delete-confirm', id);
  } finally {
    submitting.value = false;
  }
};
</script>