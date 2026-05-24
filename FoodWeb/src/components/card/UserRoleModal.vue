<template>
  <dialog class="modal" :class="{ 'modal-open': show }">
    <div class="modal-box w-11/12 max-w-2xl">
      <h3 class="font-bold text-lg mb-4 flex items-center gap-2">
        <span class="w-1.5 h-6 rounded-full bg-warning"></span>
        角色管理 - {{ user?.nickname || `用户#${user?.id}` }}
      </h3>

      <!-- 加载状态 -->
      <div v-if="loading" class="flex justify-center py-8">
        <span class="loading loading-spinner loading-lg text-primary"></span>
      </div>

      <div v-else class="space-y-4">
        <!-- 当前角色列表 -->
        <div class="bg-base-200/50 rounded-xl p-4">
          <div class="text-sm font-medium mb-3 flex items-center gap-2">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-primary" fill="none" viewBox="0 0 24 24"
              stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
            当前角色
          </div>
          <div v-if="userRoles.length === 0" class="text-sm text-base-content/40 py-2">暂无角色</div>
          <div v-else class="space-y-2">
            <div v-for="ur in userRoles" :key="ur.id"
              class="bg-base-100/60 rounded-lg p-2.5 flex items-start gap-2">
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 flex-wrap">
                  <span class="badge badge-sm" :class="ur.storeId ? 'badge-secondary' : 'badge-primary'">
                    {{ ur.storeId ? '门店' : '全局' }}
                  </span>
                  <span class="font-medium text-sm">{{ getRoleName(ur.roleId) }}</span>
                  <span v-if="ur.storeId" class="text-xs text-base-content/50">[门店#{{ ur.storeId }}]</span>
                  <!-- 权限数量标签 -->
                  <span v-if="getRolePermissionCount(ur.roleId) > 0"
                    class="badge badge-xs badge-ghost cursor-pointer"
                    @click="toggleRoleExpand(ur.roleId)">
                    {{ getRolePermissionCount(ur.roleId) }}个权限
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 ml-0.5 transition-transform"
                      :class="{ 'rotate-180': expandedRoles.has(ur.roleId) }" fill="none" viewBox="0 0 24 24"
                      stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                    </svg>
                  </span>
                </div>
                <!-- 展开的权限列表 -->
                <div v-if="expandedRoles.has(ur.roleId) && getRolePermissions(ur.roleId).length > 0"
                  class="flex flex-wrap gap-1 mt-1.5">
                  <span v-for="p in getRolePermissions(ur.roleId)" :key="p.id"
                    class="text-[10px] bg-base-300/60 rounded px-1.5 py-0.5">
                    {{ p.permissionName }}
                  </span>
                </div>
              </div>
              <button class="btn btn-ghost btn-xs p-0 min-h-0 h-auto hover:text-error shrink-0"
                @click="handleRemoveRole(ur)" :disabled="operating" title="移除角色">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24"
                  stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
          </div>
        </div>

        <div class="divider my-1"></div>

        <!-- 分配全局角色 -->
        <div class="bg-base-200/30 rounded-xl p-4">
          <div class="text-sm font-medium mb-3 flex items-center gap-2">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-primary" fill="none" viewBox="0 0 24 24"
              stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064" />
            </svg>
            分配全局角色
          </div>
          <div class="flex flex-col sm:flex-row gap-2">
            <select v-model="selectedGlobalRoleId" class="select select-bordered w-full focus:select-primary flex-1">
              <option :value="null" disabled>请选择全局角色</option>
              <option v-for="role in globalRoles" :key="role.id" :value="role.id">
                {{ role.roleName }} ({{ role.roleCode }})
              </option>
            </select>
            <button class="btn btn-primary btn-sm sm:btn-md" @click="handleAssignGlobalRole"
              :disabled="!selectedGlobalRoleId || operating">
              <span v-if="operating" class="loading loading-spinner loading-xs"></span>
              分配
            </button>
          </div>
          <!-- 全局角色权限预览 -->
          <div v-if="selectedGlobalRoleId && getRolePermissions(selectedGlobalRoleId).length > 0"
            class="mt-2 flex flex-wrap gap-1">
            <span class="text-xs text-base-content/50">关联权限：</span>
            <span v-for="p in getRolePermissions(selectedGlobalRoleId)" :key="p.id"
              class="badge badge-xs badge-ghost">{{ p.permissionName }}</span>
          </div>
        </div>

        <!-- 分配门店角色 -->
        <div class="bg-base-200/30 rounded-xl p-4">
          <div class="text-sm font-medium mb-3 flex items-center gap-2">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-secondary" fill="none" viewBox="0 0 24 24"
              stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
            </svg>
            分配门店角色
          </div>
          <div class="flex flex-col gap-2">
            <div class="flex flex-col sm:flex-row gap-2">
              <input v-model.number="storeIdInput" type="number" placeholder="门店ID"
                class="input input-bordered w-full sm:w-28 focus:input-primary" min="1" />
              <select v-model="selectedStoreRoleId" class="select select-bordered w-full focus:select-primary flex-1">
                <option :value="null" disabled>请选择门店角色</option>
                <option v-for="role in storeRoles" :key="role.id" :value="role.id">
                  {{ role.roleName }} ({{ role.roleCode }})
                </option>
              </select>
              <button class="btn btn-secondary btn-sm sm:btn-md" @click="handleAssignStoreRole"
                :disabled="!selectedStoreRoleId || !storeIdInput || operating">
                <span v-if="operating" class="loading loading-spinner loading-xs"></span>
                分配
              </button>
            </div>
            <!-- 门店角色权限预览 -->
            <div v-if="selectedStoreRoleId && getRolePermissions(selectedStoreRoleId).length > 0"
              class="flex flex-wrap gap-1">
              <span class="text-xs text-base-content/50">关联权限：</span>
              <span v-for="p in getRolePermissions(selectedStoreRoleId)" :key="p.id"
                class="badge badge-xs badge-ghost">{{ p.permissionName }}</span>
            </div>
          </div>
        </div>

        <!-- 角色与权限总览 -->
        <div class="collapse collapse-arrow bg-base-200/30 rounded-xl">
          <input type="checkbox" />
          <div class="collapse-title text-sm font-medium flex items-center gap-2">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-info" fill="none" viewBox="0 0 24 24"
              stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            系统角色与权限总览
          </div>
          <div class="collapse-content">
            <div v-if="allRoles.length === 0" class="text-sm text-base-content/40 py-2">暂无角色数据</div>
            <div v-else class="space-y-3 pt-1">
              <div v-for="role in allRoles" :key="role.id" class="bg-base-100/60 rounded-lg p-3">
                <div class="flex items-center gap-2 mb-1">
                  <span class="badge badge-sm" :class="role.domain === 'GLOBAL' ? 'badge-primary' : 'badge-secondary'">
                    {{ role.domain === 'GLOBAL' ? '全局' : '门店' }}
                  </span>
                  <span class="font-medium text-sm">{{ role.roleName }}</span>
                  <span class="text-xs text-base-content/50">{{ role.roleCode }}</span>
                </div>
                <div v-if="role.description" class="text-xs text-base-content/60 mb-1">{{ role.description }}</div>
                <div v-if="role.permissionList && role.permissionList.length > 0" class="flex flex-wrap gap-1">
                  <span v-for="p in role.permissionList" :key="p.id"
                    class="badge badge-xs badge-outline">{{ p.permissionName }}</span>
                </div>
                <div v-else class="text-xs text-base-content/30">无关联权限</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="modal-action mt-4">
        <button class="btn btn-ghost" @click="handleClose">关闭</button>
      </div>
    </div>
    <div class="modal-backdrop" @click="!operating && handleClose()"></div>
  </dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { httpGet, httpPost, isSuccess } from '@/utils/fetch'
import toast from '@/utils/toast'

const props = defineProps({
  show: { type: Boolean, default: false },
  user: { type: Object, default: null }
})

const emit = defineEmits(['close', 'updated'])

const loading = ref(false)
const operating = ref(false)
const userRoles = ref([])
/** 所有角色（附带权限详情列表） */
const allRoles = ref([])
/** 展开权限详情的角色ID集合 */
const expandedRoles = ref(new Set())

const selectedGlobalRoleId = ref(null)
const selectedStoreRoleId = ref(null)
const storeIdInput = ref(null)

/** 全局角色列表（domain=GLOBAL） */
const globalRoles = computed(() => {
  return allRoles.value.filter(r => r.domain === 'GLOBAL' && r.status === 0)
})

/** 门店角色列表（domain=STORE，含模板角色和自定义角色） */
const storeRoles = computed(() => {
  return allRoles.value.filter(r => r.domain === 'STORE' && r.status === 0)
})

/** 根据角色ID获取角色名称 */
function getRoleName(roleId) {
  const role = allRoles.value.find(r => r.id === roleId)
  return role ? role.roleName : `角色#${roleId}`
}

/** 根据角色ID获取该角色关联的权限列表 */
function getRolePermissions(roleId) {
  const role = allRoles.value.find(r => r.id === roleId)
  return role?.permissionList || []
}

/** 获取角色权限数量 */
function getRolePermissionCount(roleId) {
  return getRolePermissions(roleId).length
}

/** 切换角色权限展开/收起 */
function toggleRoleExpand(roleId) {
  const newSet = new Set(expandedRoles.value)
  if (newSet.has(roleId)) {
    newSet.delete(roleId)
  } else {
    newSet.add(roleId)
  }
  expandedRoles.value = newSet
}

/** 弹窗打开时加载数据 */
watch(() => props.show, async (newVal) => {
  if (newVal && props.user) {
    selectedGlobalRoleId.value = null
    selectedStoreRoleId.value = null
    storeIdInput.value = null
    expandedRoles.value = new Set()
    await loadData()
  }
})

/**
 * 加载数据：仅需2个请求即可获取所有角色（含权限）和用户角色
 */
const loadData = async () => {
  loading.value = true
  try {
    const [rolesWithPermsRes, userRolesRes] = await Promise.all([
      httpGet('/sys/role/list-with-permissions'),
      httpGet(`/sys/user/role/${props.user.id}`)
    ])

    // 角色列表（已包含权限详情）
    if (isSuccess(rolesWithPermsRes) && rolesWithPermsRes.data) {
      allRoles.value = rolesWithPermsRes.data
    }

    // 用户角色
    if (isSuccess(userRolesRes) && userRolesRes.data) {
      userRoles.value = userRolesRes.data
    }
  } catch (e) {
    toast.error('加载角色数据失败')
  } finally {
    loading.value = false
  }
}

/**
 * 分配全局角色
 */
const handleAssignGlobalRole = async () => {
  if (!selectedGlobalRoleId.value) return
  operating.value = true
  try {
    const res = await httpPost(`/sys/user/global/role/assign?userId=${props.user.id}&roleId=${selectedGlobalRoleId.value}`)
    if (isSuccess(res)) {
      toast.success('全局角色分配成功')
      selectedGlobalRoleId.value = null
      await loadData()
      emit('updated')
    } else {
      toast.error(res?.message || '分配失败')
    }
  } catch (e) {
    toast.error('全局角色分配失败')
  } finally {
    operating.value = false
  }
}

/**
 * 分配门店角色
 */
const handleAssignStoreRole = async () => {
  if (!selectedStoreRoleId.value || !storeIdInput.value) return
  operating.value = true
  try {
    const res = await httpPost(
      `/sys/user/store/role/assign?storeId=${storeIdInput.value}&userId=${props.user.id}&roleId=${selectedStoreRoleId.value}`
    )
    if (isSuccess(res)) {
      toast.success('门店角色分配成功')
      selectedStoreRoleId.value = null
      storeIdInput.value = null
      await loadData()
      emit('updated')
    } else {
      toast.error(res?.message || '分配失败')
    }
  } catch (e) {
    toast.error('门店角色分配失败')
  } finally {
    operating.value = false
  }
}

/**
 * 移除角色
 */
const handleRemoveRole = async (userRole) => {
  operating.value = true
  try {
    let res
    if (userRole.storeId) {
      res = await httpPost(
        `/sys/user/store/role/remove?storeId=${userRole.storeId}&userId=${userRole.userId}&roleId=${userRole.roleId}`
      )
    } else {
      res = await httpPost(
        `/sys/user/global/role/remove?userId=${userRole.userId}&roleId=${userRole.roleId}`
      )
    }
    if (isSuccess(res)) {
      toast.success('角色移除成功')
      await loadData()
      emit('updated')
    } else {
      toast.error(res?.message || '移除失败')
    }
  } catch (e) {
    toast.error('角色移除失败')
  } finally {
    operating.value = false
  }
}

const handleClose = () => {
  emit('close')
}
</script>
