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
              <button class="btn btn-primary btn-sm" @click="openInviteModal()">添加员工</button>
            </div>

            <div v-if="employeesLoading" class="flex justify-center py-10">
              <span class="loading loading-spinner text-primary"></span>
            </div>

            <template v-else>
              <div v-if="employees.length === 0" class="text-center py-10">
                <p class="text-base-content/40">暂无员工，点击右上角按手机号邀请店员</p>
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
                    <tr v-for="emp in employees" :key="emp.userId" class="hover align-top">
                      <td>
                        <div class="font-medium flex items-center gap-1.5 flex-wrap">
                          {{ emp.nickname || ('用户#' + emp.userId) }}
                          <span v-if="emp.pending" class="badge badge-warning badge-sm">待确认</span>
                        </div>
                        <div class="text-xs text-base-content/50">{{ emp.phone || '未留手机号' }}</div>
                      </td>
                      <td>
                        <div class="flex flex-wrap gap-1">
                          <!-- 已生效角色 -->
                          <span v-for="r in emp.roles" :key="'r' + r.roleId"
                                class="badge badge-sm gap-1"
                                :class="r.roleCode === 'STORE_OWNER' ? 'badge-primary' : 'badge-ghost'">
                            {{ r.roleName || r.roleCode }}
                            <button v-if="r.roleCode !== 'STORE_OWNER'"
                                    class="cursor-pointer opacity-50 hover:opacity-100 hover:text-error"
                                    title="移除该角色"
                                    @click="handleRemoveRole(emp, r)">✕</button>
                          </span>
                          <!-- 待确认角色 -->
                          <span v-for="r in emp.pendingRoles" :key="'p' + r.roleId"
                                class="badge badge-sm badge-outline border-warning text-warning">
                            {{ r.roleName || r.roleCode }}
                          </span>
                          <span v-if="emp.roles.length === 0 && emp.pendingRoles.length === 0"
                                class="text-xs text-base-content/40">无角色</span>
                        </div>
                        <div v-if="emp.pending" class="text-[11px] text-base-content/40 mt-1">
                          已发送邀请，等待员工在「消息通知」中确认
                        </div>
                      </td>
                      <td class="text-right whitespace-nowrap">
                        <button v-if="emp.pending"
                                class="btn btn-ghost btn-xs text-error"
                                @click="handleCancelInvite(emp)">撤销邀请</button>
                        <button v-else-if="!isOwnerRow(emp)"
                                class="btn btn-ghost btn-xs"
                                @click="openInviteModal(emp)">追加角色</button>
                        <span v-else class="text-xs text-base-content/40">店主</span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <p class="text-xs text-base-content/40 mt-2">
                员工需先用手机号注册账号；邀请发出后需员工本人确认才会生效，未确认前不具备任何门店权限。
              </p>
            </template>
          </div>
        </div>
      </div>
    </template>

    <!-- 邀请员工弹窗（按手机号 + 多角色） -->
    <dialog class="modal" :class="{ 'modal-open': showInvite }">
      <div class="modal-box max-w-md">
        <h3 class="font-bold text-lg mb-1">{{ editingEmployee ? '追加员工角色' : '添加员工' }}</h3>
        <p class="text-xs text-base-content/50 mb-4">
          邀请发送后，员工会在「消息通知」中收到确认请求，同意后角色才生效。
        </p>
        <div class="space-y-4">
          <div class="form-control">
            <label class="label"><span class="label-text">员工手机号 *</span></label>
            <input v-model.trim="inviteForm.phone" type="tel" maxlength="11"
                   class="input input-bordered w-full" placeholder="请输入员工注册时使用的手机号"
                   :disabled="!!editingEmployee" />
            <label v-if="editingEmployee" class="label">
              <span class="label-text-alt text-base-content/40">追加角色时不可修改手机号</span>
            </label>
          </div>

          <div class="form-control">
            <label class="label">
              <span class="label-text">分配角色 *（可多选）</span>
              <span class="label-text-alt text-base-content/40">已选 {{ inviteForm.roleIds.length }} 个</span>
            </label>
            <div class="border border-base-300 rounded-box p-2 max-h-64 overflow-y-auto space-y-1">
              <label v-for="role in assignableRoles" :key="role.id"
                     class="flex items-start gap-2 p-2 rounded-lg cursor-pointer hover:bg-base-200"
                     :class="{ 'bg-primary/5': inviteForm.roleIds.includes(role.id) }">
                <input type="checkbox" class="checkbox checkbox-sm checkbox-primary mt-0.5"
                       :value="role.id" v-model="inviteForm.roleIds" />
                <span class="min-w-0">
                  <span class="block text-sm font-medium">{{ role.roleName }}</span>
                  <span v-if="role.description" class="block text-xs text-base-content/50">{{ role.description }}</span>
                </span>
              </label>
              <div v-if="assignableRoles.length === 0" class="text-sm text-base-content/40 p-2">
                暂无可分配角色
              </div>
            </div>
          </div>
        </div>
        <div class="modal-action">
          <button class="btn btn-ghost" @click="closeInviteModal">取消</button>
          <button class="btn btn-primary" :disabled="inviting" @click="handleInvite">
            <span v-if="inviting" class="loading loading-spinner loading-sm"></span>
            {{ inviting ? '发送中...' : '发送邀请' }}
          </button>
        </div>
      </div>
      <div class="modal-backdrop" @click="closeInviteModal"></div>
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
const showInvite = ref(false)
const inviting = ref(false)
const storeRoles = ref([])
const inviteForm = ref({ phone: '', roleIds: [] })
/** 追加角色时指向的员工行，为 null 表示新增员工 */
const editingEmployee = ref(null)

/**
 * 弹窗中可选的角色：排除店主角色（店主由创建门店自动产生），
 * 追加角色时再排除该员工已拥有或已在待确认中的角色，避免后端重复校验报错。
 */
const assignableRoles = computed(() => {
  const owned = new Set()
  const emp = editingEmployee.value
  if (emp) {
    for (const r of emp.roles || []) owned.add(r.roleId)
    for (const r of emp.pendingRoles || []) owned.add(r.roleId)
  }
  return storeRoles.value.filter(r => !owned.has(r.id))
})

/** 该员工行是否为店主（店主角色不可移除，也不能被当作普通员工追加角色） */
const isOwnerRow = (emp) => (emp.roles || []).some(r => r.roleCode === 'STORE_OWNER')

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

/**
 * 打开邀请弹窗
 * @param {object} [emp] 传入员工行表示「追加角色」，不传表示新增员工
 */
const openInviteModal = (emp = null) => {
  editingEmployee.value = emp || null
  inviteForm.value = {
    phone: emp?.phone || '',
    roleIds: []
  }
  showInvite.value = true
}

const closeInviteModal = () => {
  showInvite.value = false
  editingEmployee.value = null
}

/**
 * 发送员工邀请（按手机号，可多角色）
 * 邀请不会立即赋权，需员工在消息通知中确认
 */
const handleInvite = async () => {
  const { phone, roleIds } = inviteForm.value
  if (!/^1[3-9]\d{9}$/.test(phone)) {
    toast.error('请输入正确的 11 位手机号')
    return
  }
  if (roleIds.length === 0) {
    toast.error('请至少选择一个角色')
    return
  }
  inviting.value = true
  try {
    const res = await httpPost('/sys/user/store/staff/invite', {
      storeId: storeContext.currentStoreId,
      phone,
      roleIds
    })
    if (isSuccess(res)) {
      toast.success(res?.message || '邀请已发送，等待员工确认')
      closeInviteModal()
      fetchEmployees()
    } else {
      toast.error(res?.message || '邀请失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('邀请失败，请稍后重试')
  } finally {
    inviting.value = false
  }
}

/**
 * 撤销待确认的邀请
 */
const handleCancelInvite = async (emp) => {
  const ok = await confirm({
    title: '撤销邀请',
    message: `确定撤销对「${emp.nickname || emp.phone || '用户#' + emp.userId}」的邀请吗？`,
    confirmText: '确认撤销',
    tone: 'error'
  })
  if (!ok) return
  try {
    const res = await httpPost(
      `/sys/user/store/staff/invite/cancel?storeId=${storeContext.currentStoreId}&notificationId=${emp.notificationId}`
    )
    if (isSuccess(res)) {
      toast.success('邀请已撤销')
      fetchEmployees()
    } else {
      toast.error(res?.message || '撤销失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('撤销失败，请稍后重试')
  }
}

/**
 * 移除员工的单个门店角色（一人多角色时逐个移除）
 */
const handleRemoveRole = async (emp, role) => {
  const ok = await confirm({
    title: '移除角色',
    message: `确定移除「${emp.nickname || '用户#' + emp.userId}」的「${role.roleName}」角色吗？`,
    confirmText: '确认移除',
    tone: 'error'
  })
  if (!ok) return
  try {
    const res = await httpPost(
      `/sys/user/store/role/remove?storeId=${storeContext.currentStoreId}&userId=${emp.userId}&roleId=${role.roleId}`
    )
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
