<template>
  <dialog class="modal" :class="{ 'modal-open': show }">
    <div class="modal-box w-full max-w-lg">
      <h3 class="font-bold text-lg mb-4 flex items-center gap-2">
        <span class="w-1.5 h-6 rounded-full" :class="isEdit ? 'bg-info' : 'bg-primary'"></span>
        {{ isEdit ? '编辑餐桌' : '添加餐桌' }}
      </h3>

      <form @submit.prevent="handleSubmit" class="flex flex-col gap-4">
        <div class="form-control w-full">
          <label class="label"><span class="label-text font-medium">餐桌号 <span class="text-error">*</span></span></label>
          <input
            v-model="formData.tableNumber"
            type="text"
            placeholder="例如：A1、2号桌、VIP-01"
            class="input input-bordered w-full focus:input-primary"
            required
          />
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">座位数 <span class="text-error">*</span></span></label>
            <input
              v-model.number="formData.capacity"
              type="number"
              min="1"
              max="100"
              placeholder="4"
              class="input input-bordered w-full focus:input-primary"
              required
            />
          </div>

          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">排序权重</span></label>
            <input
              v-model.number="formData.sortOrder"
              type="number"
              placeholder="0"
              class="input input-bordered w-full focus:input-primary"
            />
          </div>
        </div>

        <div class="form-control w-full">
          <label class="label"><span class="label-text font-medium">区域位置</span></label>
          <select v-model="formData.area" class="select select-bordered w-full focus:select-primary">
            <option value="">请选择区域</option>
            <option value="大厅">大厅</option>
            <option value="包间">包间</option>
            <option value="露台">露台</option>
            <option value="卡座">卡座</option>
            <option value="吧台">吧台</option>
          </select>
        </div>

        <div v-if="isEdit" class="form-control w-full">
          <label class="label"><span class="label-text font-medium">餐桌状态</span></label>
          <select v-model.number="formData.status" class="select select-bordered w-full focus:select-primary">
            <option :value="0">空闲</option>
            <option :value="1">已占用</option>
            <option :value="2">已预订</option>
            <option :value="3">清理中</option>
            <option :value="4">停用</option>
          </select>
        </div>

        <div class="form-control w-full">
          <label class="label"><span class="label-text font-medium">备注</span></label>
          <textarea
            v-model="formData.remark"
            class="textarea textarea-bordered h-16 focus:textarea-primary"
            placeholder="例如：靠窗、腿有点晃"
          ></textarea>
        </div>

        <div class="modal-action mt-2">
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
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  show: { type: Boolean, default: false },
  modalType: { type: String, default: 'add' },
  table: { type: Object, default: null }
})

const emit = defineEmits(['close', 'submit'])

const submitting = ref(false)
const formData = ref(getDefaultFormData())

const isEdit = computed(() => props.modalType === 'edit')

import { computed } from 'vue'

function getDefaultFormData() {
  return {
    id: null,
    storefrontId: 0,
    tableNumber: '',
    capacity: 4,
    status: 0,
    area: '',
    sortOrder: 0,
    remark: ''
  }
}

watch(() => props.show, (newVal) => {
  if (newVal) {
    if (props.modalType === 'edit' && props.table) {
      formData.value = {
        id: props.table.id,
        storefrontId: props.table.storefrontId || 0,
        tableNumber: props.table.tableNumber || '',
        capacity: props.table.capacity || 4,
        status: props.table.status ?? 0,
        area: props.table.area || '',
        sortOrder: props.table.sortOrder || 0,
        remark: props.table.remark || ''
      }
    } else {
      formData.value = getDefaultFormData()
    }
  }
})

const handleClose = () => {
  emit('close')
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    emit('submit', { ...formData.value })
  } finally {
    submitting.value = false
  }
}
</script>
