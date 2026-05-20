<template>
  <dialog class="modal" :class="{ 'modal-open': show }">
    <div class="modal-box w-full max-w-lg">
      <h3 class="font-bold text-lg mb-4 flex items-center gap-2">
        <span class="w-1.5 h-6 rounded-full bg-warning"></span>
        预定餐桌 - {{ table?.tableNumber }}
      </h3>

      <form @submit.prevent="handleSubmit" class="flex flex-col gap-4">
        <div class="grid grid-cols-2 gap-4">
          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">预订人 <span class="text-error">*</span></span></label>
            <input
              v-model="formData.contactName"
              type="text"
              placeholder="姓名"
              class="input input-bordered w-full focus:input-primary"
              required
            />
          </div>

          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">联系电话 <span class="text-error">*</span></span></label>
            <input
              v-model="formData.contactPhone"
              type="tel"
              placeholder="手机号"
              class="input input-bordered w-full focus:input-primary"
              required
            />
          </div>
        </div>

        <div class="form-control w-full">
          <label class="label"><span class="label-text font-medium">就餐人数 <span class="text-error">*</span></span></label>
          <input
            v-model.number="formData.guestCount"
            type="number"
            min="1"
            :max="table?.capacity || 100"
            placeholder="人数"
            class="input input-bordered w-full focus:input-primary"
            required
          />
          <label class="label">
            <span class="label-text-alt text-base-content/50">该餐桌最多容纳 {{ table?.capacity || '-' }} 人</span>
          </label>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">开始时间 <span class="text-error">*</span></span></label>
            <input
              v-model="startTimeStr"
              type="datetime-local"
              class="input input-bordered w-full focus:input-primary"
              required
            />
          </div>

          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">结束时间 <span class="text-error">*</span></span></label>
            <input
              v-model="endTimeStr"
              type="datetime-local"
              class="input input-bordered w-full focus:input-primary"
              required
            />
          </div>
        </div>

        <div class="form-control w-full">
          <label class="label"><span class="label-text font-medium">备注</span></label>
          <textarea
            v-model="formData.remark"
            class="textarea textarea-bordered h-16 focus:textarea-primary"
            placeholder="特殊需求或备注"
          ></textarea>
        </div>

        <div class="modal-action mt-2">
          <button type="button" class="btn btn-ghost" @click="handleClose" :disabled="submitting">取消</button>
          <button type="submit" class="btn btn-warning" :disabled="submitting">
            <span v-if="submitting" class="loading loading-spinner loading-xs"></span>
            确认预定
          </button>
        </div>
      </form>
    </div>
    <div class="modal-backdrop" @click="!submitting && handleClose()"></div>
  </dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue'

const props = defineProps({
  show: { type: Boolean, default: false },
  table: { type: Object, default: null }
})

const emit = defineEmits(['close', 'submit'])

const submitting = ref(false)
const formData = ref({
  contactName: '',
  contactPhone: '',
  guestCount: 1,
  remark: ''
})

const startTimeStr = ref('')
const endTimeStr = ref('')

function getDefaultTimeRange() {
  const now = new Date()
  const start = new Date(now.getTime() + 3600000)
  start.setMinutes(0, 0, 0)
  const end = new Date(start.getTime() + 7200000)
  return {
    start: formatLocalDatetime(start),
    end: formatLocalDatetime(end)
  }
}

function formatLocalDatetime(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d}T${h}:${min}`
}

watch(() => props.show, (newVal) => {
  if (newVal) {
    const defaultTime = getDefaultTimeRange()
    startTimeStr.value = defaultTime.start
    endTimeStr.value = defaultTime.end
    formData.value = {
      contactName: '',
      contactPhone: '',
      guestCount: props.table?.capacity ? Math.min(2, props.table.capacity) : 1,
      remark: ''
    }
  }
})

const handleClose = () => {
  emit('close')
}

const handleSubmit = async () => {
  const startMs = new Date(startTimeStr.value).getTime()
  const endMs = new Date(endTimeStr.value).getTime()

  if (startMs >= endMs) {
    emit('submit', { _error: '开始时间必须早于结束时间' })
    return
  }

  submitting.value = true
  try {
    emit('submit', {
      storeId: 0,
      tableId: props.table?.id,
      contactName: formData.value.contactName,
      contactPhone: formData.value.contactPhone,
      guestCount: formData.value.guestCount,
      reservedStartTime: startMs,
      reservedEndTime: endMs,
      remark: formData.value.remark,
      operator: 0
    })
  } finally {
    submitting.value = false
  }
}
</script>
