<template>
  <div>
    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
      <div>
        <h2 class="text-2xl font-bold text-base-content">店铺设置</h2>
        <p class="text-sm text-base-content/60 mt-1">管理店面信息与店内员工角色</p>
      </div>
      <!-- 店面状态提示 -->
      <div v-if="store" class="flex items-center gap-2">
        <span :class="['badge', statusBadge(store.status)]">{{ statusText(store.status) }}</span>
        <span v-if="store.status === -1" class="text-xs text-base-content/50">等待平台审核后对外展示</span>
      </div>
    </div>

    <div v-if="!store" class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body text-center py-16">
        <p class="text-base-content/50 text-lg">请先在顶部选择要管理的门店</p>
      </div>
    </div>

    <template v-else>
      <div class="grid grid-cols-1 xl:grid-cols-2 gap-6">
        <!-- 左侧：店面信息编辑 -->
        <div class="card bg-base-100 shadow-sm border border-base-200">
          <div class="card-body">
            <h3 class="font-bold text-lg mb-2">店面信息</h3>
            <form @submit.prevent="handleSaveStore" class="space-y-3">
              <!-- 店铺 Logo 上传 -->
              <div class="form-control">
                <label class="label"><span class="label-text">店铺 Logo</span></label>
                <div class="flex items-center gap-4">
                  <figure class="w-24 h-24 rounded-xl bg-base-200 overflow-hidden flex-shrink-0 border border-base-300">
                    <img v-if="storeForm.logoUrl" :src="storeForm.logoUrl" alt="店铺Logo" class="w-full h-full object-cover" />
                    <div v-else class="w-full h-full flex items-center justify-center text-4xl text-base-content/30">🏪</div>
                  </figure>
                  <div class="flex flex-col gap-2">
                    <button type="button" class="btn btn-outline btn-sm" @click="showLogoModal = true">
                      {{ storeForm.logoUrl ? '更换图片' : '上传图片' }}
                    </button>
                    <button v-if="storeForm.logoUrl" type="button" class="btn btn-ghost btn-xs text-error" @click="storeForm.logoUrl = ''">移除</button>
                  </div>
                </div>
              </div>
              <div class="form-control">
                <label class="label"><span class="label-text">门店名称 *</span></label>
                <input v-model.trim="storeForm.storeName" type="text" class="input input-bordered w-full" required maxlength="50" />
              </div>
              <div class="form-control">
                <label class="label"><span class="label-text">门店地址 *</span></label>
                <input v-model.trim="storeForm.address" type="text" class="input input-bordered w-full" required maxlength="200" />
              </div>
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div class="form-control">
                  <label class="label"><span class="label-text">联系电话 *</span></label>
                  <input v-model.trim="storeForm.phone" type="text" class="input input-bordered w-full" required maxlength="20" />
                </div>
                <div class="form-control">
                  <label class="label"><span class="label-text">营业时间</span></label>
                  <input v-model.trim="storeForm.businessHours" type="text" class="input input-bordered w-full" placeholder="09:00-21:00" maxlength="50" />
                </div>
              </div>
              <div class="form-control">
                <label class="label"><span class="label-text">门店简介</span></label>
                <textarea v-model.trim="storeForm.description" class="textarea textarea-bordered w-full" rows="3" maxlength="500"></textarea>
              </div>
              <!-- 营业状态切换（仅已通过审核的门店可自行切换） -->
              <div class="form-control" v-if="store.status === 0 || store.status === 1">
                <label class="label"><span class="label-text">营业状态</span></label>
                <div class="flex gap-2">
                  <label class="label cursor-pointer justify-start gap-2 bg-base-200 rounded-lg px-4 py-2 flex-1">
                    <input v-model.number="storeForm.status" type="radio" :value="0" class="radio radio-primary radio-sm" />
                    <span class="label-text">营业中</span>
                  </label>
                  <label class="label cursor-pointer justify-start gap-2 bg-base-200 rounded-lg px-4 py-2 flex-1">
                    <input v-model.number="storeForm.status" type="radio" :value="1" class="radio radio-sm" />
                    <span class="label-text">休息中</span>
                  </label>
                </div>
              </div>
              <div class="card-actions justify-end pt-2">
                <button type="submit" class="btn btn-primary" :disabled="saving">
                  <span v-if="saving" class="loading loading-spinner loading-sm"></span>
                  {{ saving ? '保存中...' : '保存修改' }}
                </button>
              </div>
            </form>
          </div>
        </div>

        <!-- 右侧：员工管理（仅店主可见） -->
        <div v-if="isOwner" class="card bg-base-100 shadow-sm border border-base-200">
          <div class="card-body">
            <div class="flex items-center justify-between mb-2">
              <h3 class="font-bold text-lg">员工管理</h3>
              <button class="btn btn-primary btn-sm" @click="openAssignModal">添加员工角色</button>
            </div>

            <div v-if="employeesLoading" class="flex justify-center py-10">
              <span class="loading loading-spinner text-primary"></span>
            </div>

            <template v-else>
              <div v-if="employees.length === 0" class="text-center py-10">
                <p class="text-base-content/40">暂无员工，点击右上角为店员分配角色</p>
              </div>
              <div v-else class="overflow-x-auto rounded-box border border-base-200">
                <table class="table table-sm">
                  <thead>
                    <tr>
                      <th>员工</th>
                      <th>角色</th>
                      <th class="text-right">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="emp in employees" :key="emp.id" class="hover">
                      <td>
                        <div class="font-medium">{{ emp.nickname || ('用户#' + emp.userId) }}</div>
                        <div class="text-xs text-base-content/50">{{ emp.phone || '未留手机号' }}</div>
                      </td>
                      <td>
                        <span class="badge badge-sm" :class="emp.roleCode === 'STORE_OWNER' ? 'badge-primary' : 'badge-ghost'">
                          {{ emp.roleName || emp.roleCode }}
                        </span>
                      </td>
                      <td class="text-right">
                        <button
                          v-if="emp.roleCode !== 'STORE_OWNER'"
                          class="btn btn-ghost btn-xs text-error"
                          @click="handleRemoveRole(emp)"
                        >移除</button>
                        <span v-else class="text-xs text-base-content/40">店主</span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <p class="text-xs text-base-content/40 mt-2">
                添加员工：先让店员使用手机号注册账号，然后通过用户ID为其分配本店角色。
              </p>
            </template>
          </div>
        </div>
      </div>
    </template>

    <!-- 分配员工角色弹窗 -->
    <dialog class="modal" :class="{ 'modal-open': showAssign }">
      <div class="modal-box max-w-md">
        <h3 class="font-bold text-lg mb-4">为员工分配角色</h3>
        <div class="space-y-3">
          <div class="form-control">
            <label class="label"><span class="label-text">用户ID（员工注册后可在系统用户管理中查询） *</span></label>
            <input v-model.number="assignForm.userId" type="number" min="1" class="input input-bordered w-full" placeholder="请输入用户ID" />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">分配角色 *</span></label>
            <select v-model="assignForm.roleId" class="select select-bordered w-full">
              <option value="" disabled>请选择角色</option>
              <option v-for="role in storeRoles" :key="role.id" :value="role.id">
                {{ role.roleName }}{{ role.description ? ' - ' + role.description : '' }}
              </option>
            </select>
          </div>
        </div>
        <div class="modal-action">
          <button class="btn btn-ghost" @click="closeAssignModal">取消</button>
          <button class="btn btn-primary" :disabled="assigning" @click="handleAssignRole">
            <span v-if="assigning" class="loading loading-spinner loading-sm"></span>
            {{ assigning ? '分配中...' : '确认分配' }}
          </button>
        </div>
      </div>
      <div class="modal-backdrop" @click="closeAssignModal"></div>
    </dialog>

    <!-- 店铺 Logo 上传弹窗 -->
    <Teleport to="body">
      <dialog :class="['modal modal-middle', { 'modal-open': showLogoModal }]">
        <div class="modal-box w-11/12 max-w-2xl">
          <h3 class="font-bold text-lg mb-4">上传店铺 Logo</h3>
          <ImageUpload :allow-multiple="false" @upload-success="handleLogoUploadSuccess" />
          <div class="modal-action">
            <button class="btn btn-ghost" @click="showLogoModal = false">完成</button>
          </div>
        </div>
        <div class="modal-backdrop" @click="showLogoModal = false"></div>
      </dialog>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { httpGet, httpPost, isSuccess } from '@/utils/fetch'
import toast from '@/utils/toast'
import { useStoreContext } from '@/stores/storeContext'
import { useAuthStore } from '@/stores/auth'
import ImageUpload from '@/components/upload/ImageUpload.vue'
import { useConfirm } from '@/composables/useConfirm'

const { confirm } = useConfirm()

const storeContext = useStoreContext()
const authStore = useAuthStore()

/** 当前用户是否为店主（控制员工管理面板可见性） */
const isOwner = computed(() => authStore.hasAnyRole(['STORE_OWNER']))

/** 当前管理的门店 */
const store = computed(() => storeContext.currentStore)

const saving = ref(false)
const showLogoModal = ref(false)
const storeForm = ref({
  id: null,
  storeName: '',
  address: '',
  phone: '',
  logoUrl: '',
  businessHours: '',
  description: '',
  status: 0,
  tags: []
})

const employees = ref([])
const employeesLoading = ref(false)
const showAssign = ref(false)
const assigning = ref(false)
const storeRoles = ref([])
const assignForm = ref({ userId: null, roleId: '' })

onMounted(() => {
  initForm()
  if (isOwner.value) {
    fetchEmployees()
    fetchStoreRoles()
  }
})

// 门店切换时重新加载
watch(() => storeContext.currentStoreId, () => {
  initForm()
  if (isOwner.value) {
    fetchEmployees()
    fetchStoreRoles()
  }
})

/** 用当前门店数据初始化表单 */
const initForm = () => {
  if (!store.value) return
  storeForm.value = {
    id: store.value.id,
    storeName: store.value.storeName,
    address: store.value.address,
    phone: store.value.phone,
    logoUrl: store.value.logoUrl || '',
    businessHours: store.value.businessHours || '',
    description: store.value.description || '',
    status: store.value.status,
    tags: store.value.tags || []
  }
}

/**
 * 保存店面信息
 */
const handleSaveStore = async () => {
  if (!storeForm.value.storeName || !storeForm.value.address || !storeForm.value.phone) {
    toast.error('请填写门店名称、地址和联系电话')
    return
  }
  saving.value = true
  try {
    const res = await httpPost('/store/manage/update', storeForm.value)
    if (isSuccess(res)) {
      toast.success('店面信息已保存')
      // 刷新门店上下文（名称等可能变化）
      await storeContext.loadMyStores()
    } else {
      toast.error(res?.message || '保存失败')
    }
  } catch (e) {
    toast.error('保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

/** Logo 上传成功回调 */
const handleLogoUploadSuccess = (response) => {
  if (!response?.data?.[0]) return
  storeForm.value.logoUrl = response.data[0].fileUrl
  nextTick(() => {
    showLogoModal.value = false
  })
}

/**
 * 加载门店员工列表
 */
const fetchEmployees = async () => {
  if (!storeContext.currentStoreId) return
  employeesLoading.value = true
  try {
    const res = await httpGet(`/sys/user/store/role/list?storeId=${storeContext.currentStoreId}`)
    if (isSuccess(res)) {
      employees.value = res.data || []
    }
  } catch (e) {
    console.error('加载员工列表失败:', e)
  } finally {
    employeesLoading.value = false
  }
}

/**
 * 加载可分配的门店角色（模板角色）
 */
const fetchStoreRoles = async () => {
  try {
    const res = await httpGet('/sys/role/store/template/list-with-permissions')
    if (isSuccess(res)) {
      // 过滤掉店主角色（店主通过创建门店自动产生）
      storeRoles.value = (res.data || []).filter(r => r.roleCode !== 'STORE_OWNER')
    }
  } catch (e) {
    console.error('加载门店角色失败:', e)
  }
}

/** 打开分配弹窗 */
const openAssignModal = () => {
  assignForm.value = { userId: null, roleId: '' }
  showAssign.value = true
}

const closeAssignModal = () => {
  showAssign.value = false
}

/**
 * 为员工分配门店角色
 */
const handleAssignRole = async () => {
  const { userId, roleId } = assignForm.value
  if (!userId || !roleId) {
    toast.error('请填写用户ID并选择角色')
    return
  }
  assigning.value = true
  try {
    const res = await httpPost(`/sys/user/store/role/assign?storeId=${storeContext.currentStoreId}&userId=${userId}&roleId=${roleId}`)
    if (isSuccess(res)) {
      toast.success('角色分配成功')
      closeAssignModal()
      fetchEmployees()
    } else {
      toast.error(res?.message || '分配失败')
    }
  } catch (e) {
    toast.error('分配失败，请稍后重试')
  } finally {
    assigning.value = false
  }
}

/**
 * 移除员工门店角色
 */
const handleRemoveRole = async (emp) => {
  const ok = await confirm({
    title: '移除角色',
    message: `确定移除「${emp.nickname || '用户#' + emp.userId}」的「${emp.roleName}」角色吗？`,
    confirmText: '确认移除',
    tone: 'error'
  })
  if (!ok) return
  try {
    const res = await httpPost(`/sys/user/store/role/remove?storeId=${emp.storeId}&userId=${emp.userId}&roleId=${emp.roleId}`)
    if (isSuccess(res)) {
      toast.success('角色已移除')
      fetchEmployees()
    } else {
      toast.error(res?.message || '移除失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('移除失败，请稍后重试')
  }
}

/** 店面状态展示 */
const statusText = (status) => {
  const map = { '-1': '待审核', 0: '营业中', 1: '休息中', 2: '已停业' }
  return map[status] ?? '未知'
}

const statusBadge = (status) => {
  const map = { '-1': 'badge-warning', 0: 'badge-success', 1: 'badge-info', 2: 'badge-error' }
  return map[status] ?? 'badge-ghost'
}
</script>
