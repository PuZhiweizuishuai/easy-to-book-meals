import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { httpGet, httpPost, isSuccess } from '@/utils/fetch'

/**
 * 认证状态管理 Store
 * 管理用户登录状态、Token、用户信息
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)

  /** 是否已登录 */
  const isLoggedIn = computed(() => !!token.value)

  /** 用户昵称 */
  const nickname = computed(() => userInfo.value?.nickname || '管理员')

  /** 用户角色列表 */
  const roles = computed(() => userInfo.value?.roleDataList || [])

  /** 是否为超级管理员 */
  const isSuperAdmin = computed(() => {
    return roles.value.some(r => r.roleCode === 'SUPER_ADMIN')
  })

  /**
   * 设置登录信息
   * @param {object} data - 登录接口返回的数据 { token, userId, nickname }
   */
  function setLoginInfo(data) {
    if (data?.token) {
      token.value = data.token
      localStorage.setItem('token', data.token)
    }
  }

  /**
   * 获取当前用户信息（含角色与权限）
   * 后端接口：GET /sys/auth/info
   */
  async function fetchUserInfo() {
    try {
      const res = await httpGet('/sys/auth/info')
      if (isSuccess(res) && res.data) {
        userInfo.value = res.data
        return res.data
      }
    } catch (e) {
      console.error('获取用户信息失败', e)
    }
    return null
  }

  /**
   * 登出
   * 后端接口：POST /sys/auth/logout
   */
  async function logout() {
    try {
      await httpPost('/sys/auth/logout')
    } catch {
      // 登出请求失败也清除本地状态
    } finally {
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('token')
    }
  }

  /**
   * 清除登录状态（不调用后端接口）
   */
  function clearAuth() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    nickname,
    roles,
    isSuperAdmin,
    setLoginInfo,
    fetchUserInfo,
    logout,
    clearAuth
  }
})
