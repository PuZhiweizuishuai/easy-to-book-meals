<template>
  <dialog class="modal" :class="{ 'modal-open': show }">
    <div class="modal-box w-11/12 max-w-2xl">
      <h3 class="font-bold text-lg mb-4 flex items-center gap-2">
        <span class="w-1.5 h-6 rounded-full bg-warning"></span>
        角色管理 - {{ user?.nickname || `用户#${user?.id}` }}
      </h3>

      <div v-if="loading" class="flex justify-center py-8">
        <span class="loading loading-spinner loading-lg text-primary"></span>
      </div>

      <div v-else class="space-y-4">
        <div class="flex items-center gap-2 mb-2">
          <span class="text-sm text-base-content/60">当前角色：</span>
          <div class="flex flex-wrap gap-2">
            <span v-for="ur in userRoles" :key="ur.id" class="badge badge-outline badge-primary gap-1">
              {{ getRoleName(ur.roleId) }}
              <span v-if="ur.storeId" class="text-xs opacity-70">[店面#{{ ur.storeId }}]</span>
              <button class="btn btn-ghost btn-xs p-0 min-h-0 h-auto" @click="handleRemoveRole(ur)" title="移除角色">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24"
                  stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </span>
            <span v-if="userRoles.length === 0" class="text-sm text-base-content/40">暂无角色</span>
          </div>
        </div>

        <div class="divider my-2"></div>

        <div class="form-control w-full">
          <label class="label"><span class="label-text font-medium">分配新角色</span></label>
          <div class="flex flex-col sm:flex-row gap-2">
            <select v-model="selectedRoleId" class="select select-bordered w-full focus:select-primary flex-1">
              <option :value="null" disabled>请选择角色</option>
              <option v-for="role in availableRoles" :key="role.id" :value="role.id">
                {{ role.roleName }} ({{ role.roleCode }})
              </option>
            </select>
            <button class="btn btn-primary btn-sm sm:btn-md" @click="handleAssignRole"
              :disabled="!selectedRoleId || assigning">
              <span v-if="assigning" class="loading loading-spinner loading-xs"></span>
              分配系统角色
            </button>
          </div>
        </div>

        <div class="form-control w-full">
          <label class="label"><span class="label-text font-medium">分配店面角色</span></label>
          <div class="flex flex-col sm:flex-row gap-2">
            <input v-model.number="storeIdInput" type="number" placeholder="店面ID"
              class="input input-bordered w-full sm:w-32 focus:input-primary" min="1" />
            <select v-model="selectedStoreRoleId" class="select select-bordered w-full focus:select-primary flex-1">
              <option :value="null" disabled>请选择角色</option>
              <option v-for="role in availableRoles" :key="role.id" :value="role.id">
                {{ role.roleName }} ({{ role.roleCode }})
              </option>
            </select>
            <button class="btn btn-warning btn-sm sm:btn-md" @click="handleAssignStoreRole"
              :disabled="!selectedStoreRoleId || !storeIdInput || assigning">
              <span v-if="assigning" class="loading loading-spinner loading-xs"></span>
              分配店面角色
            </button>
          </div>
        </div>
      </div>

      <div class="modal-action mt-4">
        <button class="btn btn-ghost" @click="handleClose">关闭</button>
      </div>
    </div>
    <div class="modal-backdrop" @click="handleClose"></div>
  </dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { httpGet, httpPost } from '@/utils/fetch'
import toast from '@/utils/toast'

const props = defineProps({
  show: { type: Boolean, default: false },
  user: { type: Object, default: null }
})

const emit = defineEmits(['close', 'updated'])

const loading = ref(false)
const assigning = ref(false)
const userRoles = ref([])
const allRoles = ref([])
const selectedRoleId = ref(null)
const selectedStoreRoleId = ref(null)
const storeIdInput = ref(null)

const availableRoles = computed(() => {
  return allRoles.value.filter(r => r.status === 0)
})

function getRoleName(roleId) {
  const role = allRoles.value.find(r => r.id === roleId)
  return role ? role.roleName : `角色#${roleId}`
}

watch(() => props.show, async (newVal) => {
  if (newVal && props.user) {
    selectedRoleId.value = null
    selectedStoreRoleId.value = null
    storeIdInput.value = null
    await loadData()
  }
})

const loadData = async () => {
  loading.value = true
  try {
    const [rolesRes, userRolesRes] = await Promise.all([
      httpGet('/sys/role/list'),
      httpGet(`/sys/user/role/${props.user.id}`)
    ])
    if (rolesRes?.data) allRoles.value = rolesRes.data
    if (userRolesRes?.data) userRoles.value = userRolesRes.data
  } catch (e) {
    toast.error('加载角色数据失败')
  } finally {
    loading.value = false
  }
}

const handleAssignRole = async () => {
  if (!selectedRoleId.value) return
  assigning.value = true
  try {
    const res = await httpPost(`/sys/user/role/assign?userId=${props.user.id}&roleId=${selectedRoleId.value}`)
    if (res?.status === 0 || res?.data) {
      toast.success('系统角色分配成功')
      await loadData()
      emit('updated')
    } else {
      toast.error(res?.message || '分配失败')
    }
  } catch (e) {
    toast.error('系统角色分配失败')
  } finally {
    assigning.value = false
  }
}

const handleAssignStoreRole = async () => {
  if (!selectedStoreRoleId.value || !storeIdInput.value) return
  assigning.value = true
  try {
    const res = await httpPost(
      `/sys/user/store/role/assign?storeId=${storeIdInput.value}&userId=${props.user.id}&roleId=${selectedStoreRoleId.value}&operatorId=1`
    )
    if (res?.status === 0 || res?.data) {
      toast.success('店面角色分配成功')
      await loadData()
      emit('updated')
    } else {
      toast.error(res?.message || '分配失败')
    }
  } catch (e) {
    toast.error('店面角色分配失败')
  } finally {
    assigning.value = false
  }
}

const handleRemoveRole = async (userRole) => {
  assigning.value = true
  try {
    const storeParam = userRole.storeId ? `&storeId=${userRole.storeId}` : ''
    const res = await httpPost(
      `/sys/user/role/remove?userId=${userRole.userId}&roleId=${userRole.roleId}${storeParam}`
    )
    if (res?.status === 0 || res?.message) {
      toast.success('角色移除成功')
      await loadData()
      emit('updated')
    } else {
      toast.error(res?.message || '移除失败')
    }
  } catch (e) {
    toast.error('角色移除失败')
  } finally {
    assigning.value = false
  }
}

const handleClose = () => {
  emit('close')
}
</script>
