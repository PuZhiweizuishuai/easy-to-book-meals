import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { httpPost, isSuccess } from '@/utils/fetch'

/** localStorage 键名常量 */
const STORAGE_KEY_TOKEN = 'token'
const STORAGE_KEY_USER = 'user_info'

/**
 * 从 localStorage 读取并解析 JSON，失败返回 null
 * @param {string} key
 * @returns {object|null}
 */
function loadFromStorage(key) {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

/**
 * 将数据序列化后写入 localStorage
 * @param {string} key
 * @param {*} value
 */
function saveToStorage(key, value) {
  localStorage.setItem(key, JSON.stringify(value))
}

/**
 * 认证状态管理 Store
 *
 * 设计要点：
 * - 登录成功后，将 token 和用户信息（含角色、权限）全部写入 localStorage
 * - 后续直接从 localStorage 读取，不再请求 /auth/info 接口，减少后端压力
 * - 路由守卫通过此 store 的 computed 属性判断用户是否有权访问页面
 */
export const useAuthStore = defineStore('auth', () => {
  /** 登录令牌 */
  const token = ref(localStorage.getItem(STORAGE_KEY_TOKEN) || '')

  /** 用户信息（含角色 roleDataList，每个角色含 permissionList） */
  const userInfo = ref(loadFromStorage(STORAGE_KEY_USER))

  /** 是否已登录 */
  const isLoggedIn = computed(() => !!token.value)

  /** 用户昵称 */
  const nickname = computed(() => userInfo.value?.nickname || '')

  /** 用户角色列表 */
  const roles = computed(() => userInfo.value?.roleDataList || [])

  /** 是否为超级管理员 */
  const isSuperAdmin = computed(() => {
    return roles.value.some(r => r.roleCode === 'SUPER_ADMIN')
  })

  /**
   * 获取用户所有权限编码（去重）
   * 遍历所有角色的 permissionList，提取 permissionCode
   * @returns {string[]}
   */
  const permissionCodes = computed(() => {
    const codes = new Set()
    for (const role of roles.value) {
      if (role.permissionList) {
        for (const perm of role.permissionList) {
          if (perm.permissionCode) {
            codes.add(perm.permissionCode)
          }
        }
      }
    }
    return [...codes]
  })

  /**
   * 获取用户所有角色编码
   * @returns {string[]}
   */
  const roleCodes = computed(() => {
    return roles.value.map(r => r.roleCode).filter(Boolean)
  })

  /**
   * 判断用户是否拥有指定角色（任一匹配即可）
   * @param {string[]} requiredRoles - 需要的角色编码列表
   * @returns {boolean}
   */
  function hasAnyRole(requiredRoles) {
    if (!requiredRoles || requiredRoles.length === 0) return true
    // 超级管理员默认拥有所有角色权限
    if (isSuperAdmin.value) return true
    return requiredRoles.some(code => roleCodes.value.includes(code))
  }

  /**
   * 判断用户是否拥有指定权限（任一匹配即可）
   * @param {string[]} requiredPermissions - 需要的权限编码列表
   * @returns {boolean}
   */
  function hasAnyPermission(requiredPermissions) {
    if (!requiredPermissions || requiredPermissions.length === 0) return true
    if (isSuperAdmin.value) return true
    return requiredPermissions.some(code => permissionCodes.value.includes(code))
  }

  /**
   * 设置登录信息
   * 登录接口返回 { token, user }，将两者分别持久化
   * @param {{ token: string, user: object }} data
   */
  function setLoginInfo(data) {
    if (data?.token) {
      token.value = data.token
      localStorage.setItem(STORAGE_KEY_TOKEN, data.token)
    }
    if (data?.user) {
      userInfo.value = data.user
      saveToStorage(STORAGE_KEY_USER, data.user)
    }
  }

  /**
   * 登出
   * 调用后端登出接口，无论成功与否都清除本地状态
   */
  async function logout() {
    try {
      await httpPost('/auth/logout')
    } catch {
      // 登出请求失败也清除本地状态
    } finally {
      clearAuth()
    }
  }

  /**
   * 清除登录状态（不调用后端接口）
   */
  function clearAuth() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem(STORAGE_KEY_TOKEN)
    localStorage.removeItem(STORAGE_KEY_USER)
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    nickname,
    roles,
    isSuperAdmin,
    permissionCodes,
    roleCodes,
    hasAnyRole,
    hasAnyPermission,
    setLoginInfo,
    logout,
    clearAuth
  }
})
